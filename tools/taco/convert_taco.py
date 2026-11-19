import re
import sys
import zipfile
import xml.etree.ElementTree as ET
from decimal import Decimal, ROUND_HALF_UP

NS = {"m": "http://schemas.openxmlformats.org/spreadsheetml/2006/main"}
NAME_ROW = 2
UNIT_ROW = 3
HEADER = {
    "kcal": (3, "Energia", "(kcal)"),
    "protein": (5, "Proteína", "(g)"),
    "fat": (6, "Lipídeos", "(g)"),
    "carbohydrate": (8, "idrato", "(g)"),
    "fiber": (9, "Alimentar", "(g)"),
    "sodium": (17, "Sódio", "(mg)"),
}
COLUMNS = {name: column for name, (column, _, _) in HEADER.items()}
REQUIRED = ("kcal", "protein", "carbohydrate", "fat")
OPTIONAL = ("fiber", "sodium")
ZERO_MARKERS = {"Tr", "NA"}
SCALE = Decimal("0.0001")


def shared_strings(workbook):
    if "xl/sharedStrings.xml" not in workbook.namelist():
        return []
    root = ET.fromstring(workbook.read("xl/sharedStrings.xml"))
    return ["".join(t.text or "" for t in si.iter("{%s}t" % NS["m"])) for si in root.findall("m:si", NS)]


def column_index(reference):
    index = 0
    for letter in re.match(r"[A-Z]+", reference).group():
        index = index * 26 + ord(letter) - 64
    return index - 1


def first_sheet_rows(path):
    workbook = zipfile.ZipFile(path)
    strings = shared_strings(workbook)
    root = ET.fromstring(workbook.read("xl/worksheets/sheet1.xml"))
    for row in root.iter("{%s}row" % NS["m"]):
        cells = {}
        for cell in row.findall("m:c", NS):
            value = cell.find("m:v", NS)
            if value is None:
                continue
            text = strings[int(value.text)] if cell.get("t") == "s" else value.text
            cells[column_index(cell.get("r"))] = text.strip()
        yield int(row.get("r")), cells


def parse_value(raw):
    if raw is None or raw == "" or raw == "*":
        return None
    if raw in ZERO_MARKERS:
        return Decimal("0")
    return Decimal(raw).quantize(SCALE, rounding=ROUND_HALF_UP).normalize()


def check_header(rows):
    names, units = rows.get(NAME_ROW, {}), rows.get(UNIT_ROW, {})
    for nutrient, (column, name, unit) in HEADER.items():
        found = (names.get(column), units.get(column))
        if found != (name, unit):
            raise ValueError(
                f"Column {column} should hold {nutrient} ({name} {unit}) but holds {found}: "
                "the spreadsheet layout changed, update HEADER after checking the new layout"
            )


def convert(path):
    return convert_rows(dict(first_sheet_rows(path)))


def convert_rows(rows):
    check_header(rows)
    foods, skipped = [], []
    for cells in rows.values():
        number = cells.get(0, "")
        if not number.isdigit():
            continue
        values = {name: parse_value(cells.get(column)) for name, column in COLUMNS.items()}
        if values["carbohydrate"] is not None and values["carbohydrate"] < 0:
            values["carbohydrate"] = Decimal("0")
        negative = [name for name, value in values.items() if value is not None and value < 0]
        if negative:
            raise ValueError(f"Negative {negative} in TACO food {number}")
        if any(values[name] is None for name in REQUIRED):
            skipped.append(f"{number} {cells[1]}")
            continue
        foods.append((number, " ".join(cells[1].split()), values))
    return foods, skipped


def main(source, target):
    foods, skipped = convert(source)
    with open(target, "w", encoding="utf-8", newline="\n") as out:
        out.write("\t".join(("taco_number", "name", *REQUIRED, *OPTIONAL)) + "\n")
        for number, name, values in foods:
            cells = [number, name] + ["" if values[n] is None else format(values[n], "f") for n in (*REQUIRED, *OPTIONAL)]
            out.write("\t".join(cells) + "\n")
    print(f"{len(foods)} foods written, {len(skipped)} skipped: {'; '.join(skipped)}")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
