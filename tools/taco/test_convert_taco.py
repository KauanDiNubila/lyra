import contextlib
import io
import re
import tempfile
import unittest
import zipfile
from decimal import Decimal, InvalidOperation
from pathlib import Path
from xml.sax.saxutils import escape

import convert_taco


def header(**moved):
    names, units = {}, {}
    for nutrient, (column, name, unit) in convert_taco.HEADER.items():
        column = moved.get(nutrient, column)
        names[column], units[column] = name, unit
    return {convert_taco.NAME_ROW: names, convert_taco.UNIT_ROW: units}


def food(number, name, kcal="100", protein="1", carbohydrate="3", fat="2", fiber="4", sodium="5"):
    values = {"kcal": kcal, "protein": protein, "carbohydrate": carbohydrate, "fat": fat, "fiber": fiber, "sodium": sodium}
    cells = {0: number, 1: name}
    for nutrient, value in values.items():
        if value is not None:
            cells[convert_taco.COLUMNS[nutrient]] = value
    return cells


def sheet(*rows):
    sheet_rows = header()
    for position, cells in enumerate(rows, start=4):
        sheet_rows[position] = cells
    return sheet_rows


class CheckHeaderTest(unittest.TestCase):
    def test_accepts_the_published_layout(self):
        convert_taco.check_header(header())

    def test_rejects_a_column_that_moved(self):
        with self.assertRaisesRegex(ValueError, "Column 5 should hold protein"):
            convert_taco.check_header(header(protein=6))

    def test_rejects_a_wrong_unit(self):
        rows = header()
        rows[convert_taco.UNIT_ROW][17] = "(g)"
        with self.assertRaisesRegex(ValueError, "Column 17 should hold sodium"):
            convert_taco.check_header(rows)

    def test_rejects_a_sheet_without_header_rows(self):
        with self.assertRaises(ValueError):
            convert_taco.check_header({})


class ParseValueTest(unittest.TestCase):
    def test_trace_and_not_applicable_are_zero(self):
        self.assertEqual(convert_taco.parse_value("Tr"), 0)
        self.assertEqual(convert_taco.parse_value("NA"), 0)

    def test_missing_and_under_evaluation_are_unknown(self):
        self.assertIsNone(convert_taco.parse_value(None))
        self.assertIsNone(convert_taco.parse_value(""))
        self.assertIsNone(convert_taco.parse_value("*"))

    def test_rounds_to_four_decimal_places(self):
        self.assertEqual(convert_taco.parse_value("123.53489250000001"), Decimal("123.5349"))
        self.assertEqual(convert_taco.parse_value("128.25848566666664"), Decimal("128.2585"))

    def test_rounds_half_up(self):
        self.assertEqual(convert_taco.parse_value("0.00005"), Decimal("0.0001"))
        self.assertEqual(convert_taco.parse_value("0.00004"), 0)

    def test_reads_scientific_notation(self):
        self.assertEqual(convert_taco.parse_value("2.0333333333333332E-2"), Decimal("0.0203"))

    def test_keeps_whole_numbers_whole(self):
        self.assertEqual(format(convert_taco.parse_value("100"), "f"), "100")
        self.assertEqual(format(convert_taco.parse_value("884"), "f"), "884")

    def test_text_that_is_not_a_number_fails(self):
        with self.assertRaises(InvalidOperation):
            convert_taco.parse_value("abc")


class ColumnIndexTest(unittest.TestCase):
    def test_letters_become_positions(self):
        self.assertEqual(convert_taco.column_index("A1"), 0)
        self.assertEqual(convert_taco.column_index("Z10"), 25)
        self.assertEqual(convert_taco.column_index("AA2"), 26)
        self.assertEqual(convert_taco.column_index("AB7"), 27)


class ConvertRowsTest(unittest.TestCase):
    def test_keeps_a_complete_food_with_rounded_values(self):
        rows = sheet(food("1", "Arroz", kcal="128.25848566666664", protein="2.5208", carbohydrate="28.0598", fat="0.227"))

        foods, skipped = convert_taco.convert_rows(rows)

        self.assertEqual(skipped, [])
        number, name, values = foods[0]
        self.assertEqual((number, name), ("1", "Arroz"))
        self.assertEqual(values["kcal"], Decimal("128.2585"))
        self.assertEqual(values["protein"], Decimal("2.5208"))
        self.assertEqual(values["carbohydrate"], Decimal("28.0598"))
        self.assertEqual(values["fat"], Decimal("0.227"))

    def test_collapses_repeated_spaces_in_the_name(self):
        foods, _ = convert_taco.convert_rows(sheet(food("591", "Coco,  verde, cru")))

        self.assertEqual(foods[0][1], "Coco, verde, cru")

    def test_trace_and_not_applicable_become_zero(self):
        foods, _ = convert_taco.convert_rows(sheet(food("1", "Azeite", protein="NA", carbohydrate="NA", fat="Tr", fiber="NA")))

        values = foods[0][2]
        self.assertEqual([values[n] for n in ("protein", "carbohydrate", "fat", "fiber")], [0, 0, 0, 0])

    def test_unknown_fiber_and_sodium_keep_the_food(self):
        foods, skipped = convert_taco.convert_rows(sheet(food("1", "Leite", fiber=None, sodium="*")))

        self.assertEqual(skipped, [])
        self.assertIsNone(foods[0][2]["fiber"])
        self.assertIsNone(foods[0][2]["sodium"])

    def test_skips_a_food_when_a_required_value_is_unknown(self):
        for nutrient in convert_taco.REQUIRED:
            for unknown in ("*", None):
                with self.subTest(nutrient=nutrient, unknown=unknown):
                    foods, skipped = convert_taco.convert_rows(sheet(food("7", "Iogurte", **{nutrient: unknown})))

                    self.assertEqual(foods, [])
                    self.assertEqual(skipped, ["7 Iogurte"])

    def test_negative_carbohydrate_from_the_difference_becomes_zero(self):
        foods, _ = convert_taco.convert_rows(sheet(food("288", "Corimba", carbohydrate="-0.0267")))

        self.assertEqual(foods[0][2]["carbohydrate"], 0)

    def test_any_other_negative_value_aborts_the_conversion(self):
        with self.assertRaisesRegex(ValueError, "protein"):
            convert_taco.convert_rows(sheet(food("9", "Estranho", protein="-0.5")))

    def test_ignores_rows_that_are_not_foods(self):
        rows = sheet({0: "Cereais e derivados"}, {0: "*", 1: "as análises estão sendo reavaliadas"}, food("1", "Arroz"))

        foods, skipped = convert_taco.convert_rows(rows)

        self.assertEqual([number for number, _, _ in foods], ["1"])
        self.assertEqual(skipped, [])

    def test_keeps_the_order_of_the_sheet(self):
        foods, _ = convert_taco.convert_rows(sheet(food("2", "B"), food("1", "A")))

        self.assertEqual([number for number, _, _ in foods], ["2", "1"])

    def test_refuses_a_sheet_with_another_layout(self):
        rows = sheet(food("1", "Arroz"))
        rows[convert_taco.UNIT_ROW][3] = "(kJ)"

        with self.assertRaisesRegex(ValueError, "Column 3 should hold kcal"):
            convert_taco.convert_rows(rows)


def column_letters(index):
    letters = ""
    index += 1
    while index:
        index, remainder = divmod(index - 1, 26)
        letters = chr(65 + remainder) + letters
    return letters


def build_xlsx(path, rows):
    strings, xml_rows = [], []
    for number, cells in sorted(rows.items()):
        xml_cells = []
        for column, value in sorted(cells.items()):
            reference = f"{column_letters(column)}{number}"
            if re.fullmatch(r"-?\d+(\.\d+)?", value):
                xml_cells.append(f'<c r="{reference}"><v>{value}</v></c>')
            else:
                strings.append(value)
                xml_cells.append(f'<c r="{reference}" t="s"><v>{len(strings) - 1}</v></c>')
        xml_rows.append(f'<row r="{number}">{"".join(xml_cells)}</row>')
    namespace = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
    sheet_xml = f'<worksheet xmlns="{namespace}"><sheetData>{"".join(xml_rows)}</sheetData></worksheet>'
    strings_xml = f'<sst xmlns="{namespace}">' + "".join(f"<si><t>{escape(text)}</t></si>" for text in strings) + "</sst>"
    with zipfile.ZipFile(path, "w") as workbook:
        workbook.writestr("xl/worksheets/sheet1.xml", sheet_xml)
        workbook.writestr("xl/sharedStrings.xml", strings_xml)


class MainTest(unittest.TestCase):
    def test_writes_the_tsv_from_a_real_xlsx_file(self):
        rows = sheet(
            food("1", "Feijão,  carioca", kcal="76.4", protein="4.8", carbohydrate="13.6", fat="0.5", fiber="8.5", sodium="Tr"),
            food("2", "Leite integral", kcal="*"),
            food("3", "Sal", kcal="0", protein="NA", carbohydrate="NA", fat="NA", fiber=None, sodium="39943.203"),
        )
        with tempfile.TemporaryDirectory() as folder:
            source, target = Path(folder, "taco.xlsx"), Path(folder, "taco.tsv")
            build_xlsx(source, rows)
            printed = io.StringIO()

            with contextlib.redirect_stdout(printed):
                convert_taco.main(str(source), str(target))

            self.assertEqual(target.read_text(encoding="utf-8"), (
                "taco_number\tname\tkcal\tprotein\tcarbohydrate\tfat\tfiber\tsodium\n"
                "1\tFeijão, carioca\t76.4\t4.8\t13.6\t0.5\t8.5\t0\n"
                "3\tSal\t0\t0\t0\t0\t\t39943.203\n"
            ))
            self.assertIn("2 foods written, 1 skipped: 2 Leite integral", printed.getvalue())


if __name__ == "__main__":
    unittest.main()
