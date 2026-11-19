import unittest

import convert_taco


def header(**moved):
    names, units = {}, {}
    for nutrient, (column, name, unit) in convert_taco.HEADER.items():
        column = moved.get(nutrient, column)
        names[column], units[column] = name, unit
    return {convert_taco.NAME_ROW: names, convert_taco.UNIT_ROW: units}


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


if __name__ == "__main__":
    unittest.main()
