"""Manual smoke test against a live server (python scripts/smoke_test.py [base_url])."""
import io
import json
import sys
import urllib.request

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8")

BASE = sys.argv[1] if len(sys.argv) > 1 else "http://127.0.0.1:8010/api/v1"


def call(method: str, path: str, body: dict | None = None):
    data = json.dumps(body).encode("utf-8") if body is not None else None
    headers = {"Content-Type": "application/json"} if data else {}
    req = urllib.request.Request(f"{BASE}{path}", data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req) as resp:
            return resp.status, json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read().decode("utf-8"))


def show(label: str, status: int, data):
    print(f"=== {label} [{status}] ===")
    print(json.dumps(data, ensure_ascii=False, indent=2)[:1500])
    print()


status, item = call("GET", "/fridge-items")
show("list fridge-items", status, item)

status, expiring = call("GET", "/fridge-items/expiring?days=5")
show("expiring fridge-items", status, expiring)

selected_ids = [row["id"] for row in item["data"]["items"]]
if selected_ids:
    status, sel = call("GET", "/recommendations/fridge-items")
    show("selectable fridge-items", status, sel)

    status, recs = call("POST", "/recommendations/recipes", {
        "selectedFridgeItemIds": selected_ids,
        "prioritizeExpiring": True,
        "limit": 5,
    })
    show("recommend recipes", status, recs)

status, dash = call("GET", "/dashboard")
show("dashboard", status, dash)

status, recipes = call("GET", "/recipes?keyword=%EA%B9%80%EC%B9%98&size=3")
show("recipe search (kimchi)", status, recipes)
