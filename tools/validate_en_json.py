import json
import re
from pathlib import Path

xml = Path(r"S:\Madl\app\src\main\res\values\strings.xml").read_text(encoding="utf-8")
names = set(re.findall(r'<string name="([^"]+)">', xml))
data = json.loads(Path(r"S:\Madl\Lang\eng.json").read_text(encoding="utf-8"))
missing = sorted(names - data.keys())
extra = sorted(data.keys() - names)
print("xml", len(names), "json", len(data))
print("missing", missing)
print("extra", extra)
for k in ("search_in_app", "platform_permissions_warning", "settings_apply_close"):
    print(k, repr(data[k]))
