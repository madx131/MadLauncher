"""Validate MAD Launcher language overlay JSON packs against en.json."""

from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LANG_DIR = ROOT / "Lang"
EN_PATH = LANG_DIR / "eng.json"

PLACEHOLDER_RE = re.compile(r"%\d+\$[sd]")

# Values allowed to stay English (exact match to en.json value)
ALLOW_ENGLISH_VALUES = {
    "MAD Launcher",
    "MAD Dock",
    "Action Box",
    "HyperOS",
    "One UI",
    "Google Drive",
    "Google Form",
    "Google",
    "Yahoo",
    "Ecosia",
    "Startpage",
    "Brave",
    "Yandex",
    "DuckDuckGo",
    "Nord",
    "Dock",
    "911",
}

# Substrings that may appear in English within translated values
ALLOW_ENGLISH_SUBSTRINGS = (
    "MAD Launcher",
    "MAD Dock",
    "Action Box",
    "HyperOS",
    "One UI",
    "Google Drive",
    "Google Form",
    "ChatGPT",
    "Gemini",
    "Claude",
    "Copilot",
    "Waze",
    "Maps",
    "Keep",
    "Android",
    "Xiaomi",
    ":yt",
    "/c",
    "/sss",
    "/ls",
    "2+2",
    "32f",
    "100c",
    "10kg",
    "5lb",
    "30psi",
    "2bar",
    "nav",
    "@",
    "@@",
    "!",
    "&",
)


def extract_placeholders(text: str) -> list[str]:
    return PLACEHOLDER_RE.findall(text)


def placeholder_sets_match(en_val: str, val: str) -> bool:
    return set(extract_placeholders(en_val)) == set(extract_placeholders(val))


def is_mostly_english(value: str, en_value: str) -> bool:
    if value == en_value:
        if value in ALLOW_ENGLISH_VALUES:
            return False
        if PLACEHOLDER_RE.search(value) and placeholder_sets_match(en_value, value):
            return False
        if value.strip() in {"%1$s · %2$s", "%1$s"}:
            return False
        return True
    return False


def validate_file(filename: str, en: dict[str, str]) -> dict:
    path = LANG_DIR / filename
    result = {
        "file": filename,
        "valid": True,
        "json_error": None,
        "missing_keys": [],
        "extra_keys": [],
        "placeholder_errors": [],
        "english_leftovers": [],
    }

    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        result["valid"] = False
        result["json_error"] = str(exc)
        return result

    data_keys = set(data.keys())
    en_keys = set(en.keys())
    result["missing_keys"] = sorted(en_keys - data_keys)
    result["extra_keys"] = sorted(data_keys - en_keys)

    if result["missing_keys"] or result["extra_keys"]:
        result["valid"] = False

    for key in en_keys & data_keys:
        en_val = en[key]
        val = data[key]
        en_ph = extract_placeholders(en_val)
        val_ph = extract_placeholders(val)
        if not placeholder_sets_match(en_val, val):
            result["placeholder_errors"].append(
                {"key": key, "expected": en_ph, "got": val_ph, "value": val}
            )
            result["valid"] = False
        if is_mostly_english(val, en_val):
            result["english_leftovers"].append({"key": key, "value": val})

    if result["english_leftovers"]:
        result["valid"] = False

    return result


def main() -> None:
    en = json.loads(EN_PATH.read_text(encoding="utf-8"))
    skip = {"eng.json", "en.json", "download-ids.json"}
    files = sorted(p.name for p in LANG_DIR.glob("*.json") if p.name not in skip)

    print(f"Master eng.json: {len(en)} keys\n")
    valid_count = 0
    for name in files:
        r = validate_file(name, en)
        status = "VALID" if r["valid"] else "INVALID"
        print(f"=== {name}: {status} ===")
        if r["json_error"]:
            print(f"  JSON error: {r['json_error']}")
        if r["missing_keys"]:
            print(f"  Missing ({len(r['missing_keys'])}): {', '.join(r['missing_keys'][:8])}{'…' if len(r['missing_keys'])>8 else ''}")
        if r["extra_keys"]:
            print(f"  Extra ({len(r['extra_keys'])}): {', '.join(r['extra_keys'][:8])}{'…' if len(r['extra_keys'])>8 else ''}")
        if r["placeholder_errors"]:
            print(f"  Placeholder errors ({len(r['placeholder_errors'])}):")
            for e in r["placeholder_errors"][:5]:
                print(f"    {e['key']}: expected {e['expected']}, got {e['got']}")
        if r["english_leftovers"]:
            print(f"  Untranslated English ({len(r['english_leftovers'])}):")
            for e in r["english_leftovers"][:5]:
                print(f"    {e['key']}: {e['value'][:60]}")
            if len(r["english_leftovers"]) > 5:
                print(f"    … and {len(r['english_leftovers']) - 5} more")
        if r["valid"]:
            valid_count += 1
            print("  VALID")
        print()

    print(f"Summary: {valid_count}/{len(files)} packs VALID")


if __name__ == "__main__":
    main()
