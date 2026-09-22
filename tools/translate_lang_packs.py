"""Generate MAD Launcher language overlay JSON packs from eng.json.

Filenames are ISO 639-2/T (`rus.json`, `heb.json`). Reuses existing
translations; fills missing keys and English leftovers; drops stale keys.
"""

from __future__ import annotations

import json
import re
import sys
import time
from pathlib import Path

from deep_translator import GoogleTranslator

ROOT = Path(__file__).resolve().parents[1]
EN_PATH = ROOT / "Lang" / "eng.json"
OUT_DIR = ROOT / "Lang"

# Overlay stem -> Google Translate target
LANGUAGES: dict[str, str] = {
    "afr": "af",
    "amh": "am",
    "ara": "ar",
    "ben": "bn",
    "bul": "bg",
    "cat": "ca",
    "ces": "cs",
    "dan": "da",
    "deu": "de",
    "ell": "el",
    "est": "et",
    "eus": "eu",
    "fas": "fa",
    "fil": "tl",
    "fin": "fi",
    "fra": "fr",
    "glg": "gl",
    "guj": "gu",
    "hau": "ha",
    "heb": "iw",
    "hin": "hi",
    "hrv": "hr",
    "hun": "hu",
    "ibo": "ig",
    "ind": "id",
    "ita": "it",
    "jpn": "ja",
    "kan": "kn",
    "khm": "km",
    "kor": "ko",
    "lao": "lo",
    "lav": "lv",
    "lit": "lt",
    "mal": "ml",
    "mar": "mr",
    "msa": "ms",
    "mya": "my",
    "nep": "ne",
    "nld": "nl",
    "nor": "no",
    "pan": "pa",
    "pol": "pl",
    "por": "pt",
    "ron": "ro",
    "rus": "ru",
    "sin": "si",
    "slk": "sk",
    "slv": "sl",
    "spa": "es",
    "srp": "sr",
    "swa": "sw",
    "swe": "sv",
    "tam": "ta",
    "tel": "te",
    "tha": "th",
    "tur": "tr",
    "ukr": "uk",
    "urd": "ur",
    "vie": "vi",
    "yor": "yo",
    "zho": "zh-CN",
    "zul": "zu",
}

# Common 2-letter / legacy aliases from older pack names
ALIASES: dict[str, str] = {
    "af": "afr",
    "am": "amh",
    "ar": "ara",
    "bg": "bul",
    "bn": "ben",
    "ca": "cat",
    "cs": "ces",
    "da": "dan",
    "de": "deu",
    "el": "ell",
    "es": "spa",
    "et": "est",
    "eu": "eus",
    "fa": "fas",
    "fi": "fin",
    "fr": "fra",
    "gl": "glg",
    "gu": "guj",
    "ha": "hau",
    "he": "heb",
    "hi": "hin",
    "hr": "hrv",
    "hu": "hun",
    "id": "ind",
    "ig": "ibo",
    "it": "ita",
    "iw": "heb",
    "ja": "jpn",
    "km": "khm",
    "kn": "kan",
    "ko": "kor",
    "lo": "lao",
    "lt": "lit",
    "lv": "lav",
    "ml": "mal",
    "mr": "mar",
    "ms": "msa",
    "my": "mya",
    "ne": "nep",
    "nl": "nld",
    "no": "nor",
    "pa": "pan",
    "pl": "pol",
    "pt": "por",
    "ro": "ron",
    "ru": "rus",
    "si": "sin",
    "sk": "slk",
    "sl": "slv",
    "sr": "srp",
    "sv": "swe",
    "sw": "swa",
    "ta": "tam",
    "te": "tel",
    "th": "tha",
    "tl": "fil",
    "tr": "tur",
    "uk": "ukr",
    "ur": "urd",
    "vi": "vie",
    "yo": "yor",
    "zh": "zho",
    "zu": "zul",
}

BATCH_SIZE = 8
JOIN_SEP = " /+/ "

LITERAL_KEEP = [
    "MAD Launcher",
    "MAD Dock",
    "Action Box",
    "ActionBox",
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
    "/abs",
    "/ss",
    "/r",
    "/h",
    "/n",
    "/t",
    "/a",
    "/g",
    "2+2",
    "32f",
    "100c",
    "10kg",
    "5lb",
    "30psi",
    "2bar",
    "911",
    "@",
    "@@",
    "!",
    "&",
]


def protect(text: str) -> tuple[str, dict[str, str]]:
    tokens: dict[str, str] = {}
    out = text
    idx = 0

    def tokenize(match: re.Match[str]) -> str:
        nonlocal idx
        key = f"ZZZ{idx}ZZZ"
        tokens[key] = match.group(0)
        idx += 1
        return key

    patterns = [
        r"%\d+\$[sd]",
        r"MAD Launcher",
        r"MAD Dock",
        r"ActionBox",
        r"Action Box",
        r"HyperOS",
        r"One UI",
        r"Google Drive",
        r"Google Form",
        r"ChatGPT|Gemini|Claude|Copilot|Waze|Maps|Keep|Android|Xiaomi",
        r":yt\b",
        r"/c\b",
        r"/sss\b",
        r"/ls\b",
        r"/abs\b",
        r"/ss\b",
        r"/r\b",
        r"/h\b",
        r"2\+2",
        r"32f",
        r"100c",
        r"10kg",
        r"5lb",
        r"30psi",
        r"2bar",
        r"911",
        r"@@",
        r"@",
        r"!",
        r"&",
    ]
    combined = "|".join(f"(?:{p})" for p in patterns)
    out = re.sub(combined, tokenize, out)
    return out, tokens


def restore(text: str, tokens: dict[str, str]) -> str:
    out = text
    for key in sorted(tokens, key=lambda k: len(k), reverse=True):
        out = out.replace(key, tokens[key])
    return out


def is_corrupt(text: str) -> bool:
    return "ZZZ" in text or "ЗЗЗ" in text


def _sleep_retry(attempt: int) -> None:
    time.sleep(min(20, 2.5 * (attempt + 1)))


def translate_one(text: str, translator: GoogleTranslator) -> str:
    if not text.strip():
        return text
    candidates = [text]
    if not text.endswith("."):
        candidates.append(text + ".")
    for padded in candidates:
        protected, tokens = protect(padded)
        for attempt in range(6):
            try:
                translated = translator.translate(protected)
                if translated:
                    time.sleep(0.25)
                    restored = restore(translated, tokens)
                    if padded != text and restored.endswith("."):
                        restored = restored[:-1]
                    return restored
            except Exception as exc:  # noqa: BLE001
                err = repr(exc)
                if attempt < 5 and ("TooManyRequests" in err or "TranslationNotFound" in err):
                    _sleep_retry(attempt)
                    continue
                print(f"  warn: {err} — keeping English", file=sys.stderr)
                break
    return text


def translate_joined(pairs: list[tuple[str, str]], translator: GoogleTranslator) -> list[str] | None:
    protected_bits: list[str] = []
    token_maps: list[dict[str, str]] = []
    for _, value in pairs:
        p, tokens = protect(value)
        protected_bits.append(p if p.strip() else value)
        token_maps.append(tokens)
    blob = JOIN_SEP.join(protected_bits)
    for attempt in range(6):
        try:
            translated = translator.translate(blob)
            if not translated:
                raise RuntimeError("empty joined translation")
            parts = [p.strip() for p in translated.split("/+/")]
            if len(parts) != len(pairs):
                return None
            time.sleep(0.3)
            out: list[str] = []
            for raw, tokens, (_, original) in zip(parts, token_maps, pairs):
                out.append(restore(raw, tokens) if raw else original)
            return out
        except Exception as exc:  # noqa: BLE001
            err = repr(exc)
            if attempt < 5 and ("TooManyRequests" in err or "TranslationNotFound" in err):
                print(f"  rate-limit, retry {attempt + 1}", file=sys.stderr)
                _sleep_retry(attempt)
                continue
            print(f"  join warn: {err}", file=sys.stderr)
            return None
    return None


def translate_batch(pairs: list[tuple[str, str]], translator: GoogleTranslator) -> list[str]:
    """pairs are (key, english). Returns translated strings in the same order."""
    if not pairs:
        return []
    if len(pairs) == 1:
        return [translate_one(pairs[0][1], translator)]
    joined = translate_joined(pairs, translator)
    if joined is not None:
        return joined
    mid = len(pairs) // 2
    print(f"  split batch {len(pairs)} -> {mid}+{len(pairs) - mid}", file=sys.stderr)
    return translate_batch(pairs[:mid], translator) + translate_batch(pairs[mid:], translator)


def translate_pack(en: dict[str, str], target: str, existing: dict[str, str] | None = None) -> dict[str, str]:
    translator = GoogleTranslator(source="en", target=target)
    out: dict[str, str] = {}
    pending: list[tuple[str, str]] = []
    for key, value in en.items():
        prior = (existing or {}).get(key)
        if prior and not is_corrupt(prior):
            out[key] = prior
            continue
        pending.append((key, value))

    print(f"  reuse {len(out)}, translate {len(pending)}")
    for i in range(0, len(pending), BATCH_SIZE):
        chunk = pending[i : i + BATCH_SIZE]
        translated = translate_batch(chunk, translator)
        for (key, original), text in zip(chunk, translated):
            out[key] = text if text else original
        done = min(i + BATCH_SIZE, len(pending))
        if done == len(pending) or done % (BATCH_SIZE * 4) == 0:
            print(f"  translated {done}/{len(pending)}")
    # Keep master key order
    return {key: out[key] for key in en}


def validate_keys(en: dict[str, str], translated: dict[str, str], filename: str) -> None:
    missing = set(en) - set(translated)
    extra = set(translated) - set(en)
    if missing or extra:
        raise SystemExit(f"{filename}: key mismatch missing={len(missing)} extra={len(extra)}")


def resolve_stem(code: str) -> str:
    raw = code.strip().lower().removesuffix(".json")
    if raw in LANGUAGES:
        return raw
    if raw in ALIASES:
        return ALIASES[raw]
    raise SystemExit(f"Unknown language code: {code}")


def main() -> None:
    en = json.loads(EN_PATH.read_text(encoding="utf-8"))
    if len(sys.argv) > 1:
        stems = [resolve_stem(c) for c in sys.argv[1:]]
    else:
        stems = sorted(LANGUAGES)

    for stem in stems:
        target = LANGUAGES[stem]
        out_path = OUT_DIR / f"{stem}.json"
        existing = None
        if out_path.exists():
            existing = json.loads(out_path.read_text(encoding="utf-8"))
        print(f"Translating {stem}.json -> {target} ...")
        translated = translate_pack(en, target, existing)
        validate_keys(en, translated, stem)
        out_path.write_text(json.dumps(translated, ensure_ascii=False, indent=4) + "\n", encoding="utf-8")
        print(f"  wrote {out_path} ({len(translated)} keys)")
    print("ALL_PACKS_DONE")


if __name__ == "__main__":
    main()
