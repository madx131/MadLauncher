import json
import re
from pathlib import Path

XML_PATH = Path(__file__).resolve().parents[1] / "app/src/main/res/values/strings.xml"
OUT_PATHS = [
    Path(__file__).resolve().parents[1] / "Lang/eng.json",
]


def decode_android(s: str) -> str:
    s = s.replace("\\n", "\n")
    s = s.replace("\\'", "'")
    s = s.replace('\\"', '"')
    s = s.replace("&amp;", "&")
    s = s.replace("&lt;", "<")
    s = s.replace("&gt;", ">")
    s = s.replace("&quot;", '"')
    s = s.replace("&apos;", "'")
    return s


def main() -> None:
    text = XML_PATH.read_text(encoding="utf-8")
    pattern = re.compile(r'<string name="([^"]+)">([\s\S]*?)</string>')
    data = {name: decode_android(raw) for name, raw in pattern.findall(text)}

    payload = json.dumps(data, ensure_ascii=False, indent=4) + "\n"
    for path in OUT_PATHS:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(payload, encoding="utf-8")

    print(f"keys: {len(data)}")
    print(f"search_in_app: {data['search_in_app']!r}")
    print(f"written: {OUT_PATHS[0]}")


if __name__ == "__main__":
    main()
