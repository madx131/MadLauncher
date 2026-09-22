"""Repair overlay packs: restore lost %1$s tokens and drop ZZZ artifacts."""

from __future__ import annotations

import json
import re
from pathlib import Path

from validate_lang_packs import extract_placeholders, placeholder_sets_match

ROOT = Path(__file__).resolve().parents[1]
LANG_DIR = ROOT / "Lang"
EN = json.loads((LANG_DIR / "eng.json").read_text(encoding="utf-8"))
SKIP = {"eng.json", "en.json", "download-ids.json"}
ZZZ_RE = re.compile(r"Z{3}\d+Z{0,3}|З{3}\d+")


def repair_value(en_val: str, val: str) -> str:
    if not val:
        return en_val
    fixed = val
    phs = extract_placeholders(en_val)
    for i, ph in enumerate(phs):
        fixed = fixed.replace(f"ZZZ{i}ZZZ", ph)
        fixed = fixed.replace(f"ZZZ{i}", ph)
        fixed = fixed.replace(f"ЗЗЗ{i}ЗЗЗ", ph)
        fixed = fixed.replace(f"ЗЗЗ{i}", ph)
    if ZZZ_RE.search(fixed):
        return en_val
    if not placeholder_sets_match(en_val, fixed):
        return en_val
    return fixed


def main() -> None:
    files = sorted(p for p in LANG_DIR.glob("*.json") if p.name not in SKIP)
    for path in files:
        data = json.loads(path.read_text(encoding="utf-8"))
        changed = 0
        out = {}
        for key, en_val in EN.items():
            val = data.get(key, en_val)
            nxt = repair_value(en_val, val)
            out[key] = nxt
            if nxt != val:
                changed += 1
        path.write_text(json.dumps(out, ensure_ascii=False, indent=4) + "\n", encoding="utf-8")
        print(f"{path.name}: repaired {changed} keys")


if __name__ == "__main__":
    main()
