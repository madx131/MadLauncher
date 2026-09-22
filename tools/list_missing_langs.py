import json
import glob
import os
from pathlib import Path

from translate_lang_packs import LANGUAGES

ROOT = Path(__file__).resolve().parents[1]
LANG_DIR = ROOT / "Lang"
EN_KEYS = set(json.loads((LANG_DIR / "eng.json").read_text(encoding="utf-8")).keys())

existing = {os.path.splitext(os.path.basename(p))[0] for p in glob.glob(str(LANG_DIR / "*.json"))}
missing = [c for c in LANGUAGES if c not in existing]
incomplete = []
for p in glob.glob(str(LANG_DIR / "*.json")):
    code = os.path.splitext(os.path.basename(p))[0]
    if code in {"en", "eng", "download-ids"}:
        continue
    keys = set(json.loads(Path(p).read_text(encoding="utf-8")).keys())
    if keys != EN_KEYS:
        incomplete.append(code)

print("existing", len(existing) - 1)
print("missing", missing)
print("incomplete", incomplete)
print("to_generate", sorted(set(missing + incomplete + [c for c in LANGUAGES if c not in existing])))
