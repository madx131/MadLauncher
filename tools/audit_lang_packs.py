import json
import glob
import os
from pathlib import Path

EN = len(json.loads(Path(r"S:\Madl\Lang\eng.json").read_text(encoding="utf-8")))
for p in sorted(glob.glob(r"S:\Madl\Lang\*.json")):
    n = os.path.basename(p)
    d = json.loads(Path(p).read_text(encoding="utf-8"))
    status = "OK" if len(d) == EN else "INCOMPLETE"
    print(f"{n}\t{len(d)}\t{status}")
