"""Per-user search history stored in data/history.json."""
import json, time
from pathlib import Path

FILE = Path(__file__).parent / "data" / "history.json"


def _load() -> dict:
    try:
        return json.loads(FILE.read_text(encoding="utf-8"))
    except Exception:
        return {}


def get(user: str) -> list:
    return _load().get(user, [])


def add(user: str, title: str, kind: str, words: int, result: dict) -> None:
    data = _load()
    items = data.setdefault(user, [])
    items.insert(0, {"title": title, "kind": kind, "words": words,
                     "time": time.strftime("%d %b %H:%M"), "result": result})
    data[user] = items[:50]
    FILE.parent.mkdir(exist_ok=True)
    FILE.write_text(json.dumps(data, ensure_ascii=False, indent=1), encoding="utf-8")


def clear(user: str) -> None:
    data = _load()
    data[user] = []
    FILE.write_text(json.dumps(data, ensure_ascii=False, indent=1), encoding="utf-8")
