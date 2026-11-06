"""Scans a single photo for multiple food items (name/category/expiry) via Gemini Vision.

Category ids are constrained to the Android app's fixed CATEGORIES list
(app/model/Category.kt) so results can flow straight into the existing add-item UI
without a separate mapping step.
"""
import json
import re
from datetime import date
from typing import Any

import httpx

from app.config import settings
from app.core.responses import api_error

GEMINI_ENDPOINT = (
    "https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent"
)

# Keep in sync with app/src/main/java/com/example/cleanoutfridge/model/Category.kt
CATEGORY_DEFAULT_DAYS: dict[str, int] = {
    "유제품": 7,
    "육류": 3,
    "채소": 7,
    "과일": 5,
    "냉동식품": 90,
    "계란": 21,
    "두부/콩류": 5,
    "조미료": 180,
    "가공식품": 14,
    "기타": 7,
}

_PROMPT = """\
너는 사진 속 식재료를 인식하는 비전 AI다. 오늘 날짜는 {today}이다. 사진에 보이는 모든 식재료를 찾아서 JSON으로만 답해라.

각 식재료마다 다음 필드를 채워라:
- name: 한국어 식재료 이름 (예: "우유", "계란", "삼겹살")
- category: 반드시 다음 목록 중 하나의 값으로만: {categories}
- emoji: 식재료를 표현하는 이모지 1개
- confidence: 인식 확신도 0~100 정수
- expiry_days: 오늘({today})부터 유통/소비기한까지 남은 일수(정수, 음수 불가). 계산 방법:
  1. 포장에 "유통기한"/"소비기한" 날짜나 연월일이 선명히 보이면, 그 날짜에서 오늘 날짜를 뺀 일수를 사용해라.
  2. 날짜가 안 보이거나 흐릿하면 해당 식재료 카테고리의 통상적인 보관 가능 일수로 추정해라.
  날짜를 실제로 읽어서 계산했는지 추정했는지에 따라 confidence를 조정해라(날짜를 읽었으면 더 높게).

사진에서 같은 재료가 여러 개 보여도 한 항목으로 합쳐라. 식재료가 아예 안 보이면 빈 리스트를 반환해라.
반드시 아래 형식의 순수 JSON만 반환하고, 코드블록이나 설명 문구를 절대 붙이지 마라.

{{"items": [{{"name": "...", "category": "...", "emoji": "...", "confidence": 90, "expiry_days": 7}}]}}
"""


def _extract_json(text: str) -> dict[str, Any]:
    cleaned = text.strip()
    cleaned = re.sub(r"^```(?:json)?\s*", "", cleaned)
    cleaned = re.sub(r"\s*```$", "", cleaned)
    return json.loads(cleaned)


async def scan_image(image_bytes: bytes, mime_type: str) -> list[dict[str, Any]]:
    if not settings.GEMINI_API_KEY:
        raise api_error(500, "서버에 GEMINI_API_KEY가 설정되지 않았습니다.", "VISION_NOT_CONFIGURED")

    import base64

    prompt = _PROMPT.format(
        categories=", ".join(CATEGORY_DEFAULT_DAYS.keys()),
        today=date.today().isoformat(),
    )
    payload = {
        "contents": [
            {
                "parts": [
                    {"text": prompt},
                    {
                        "inline_data": {
                            "mime_type": mime_type,
                            "data": base64.b64encode(image_bytes).decode("ascii"),
                        }
                    },
                ]
            }
        ],
        "generationConfig": {"temperature": 0.2, "responseMimeType": "application/json"},
    }

    url = GEMINI_ENDPOINT.format(model=settings.GEMINI_MODEL)

    try:
        async with httpx.AsyncClient(timeout=30) as client:
            response = await client.post(
                url, params={"key": settings.GEMINI_API_KEY}, json=payload
            )
    except httpx.RequestError as exc:
        raise api_error(502, "AI 서버에 연결할 수 없습니다.", "VISION_REQUEST_FAILED") from exc

    if response.status_code != 200:
        raise api_error(
            502,
            f"AI 인식에 실패했습니다. (status={response.status_code})",
            "VISION_REQUEST_FAILED",
        )

    body = response.json()
    try:
        text = body["candidates"][0]["content"]["parts"][0]["text"]
        parsed = _extract_json(text)
        raw_items = parsed.get("items", [])
    except (KeyError, IndexError, json.JSONDecodeError) as exc:
        raise api_error(502, "AI 응답을 해석할 수 없습니다.", "VISION_PARSE_FAILED") from exc

    items: list[dict[str, Any]] = []
    for raw in raw_items:
        name = str(raw.get("name", "")).strip()
        if not name:
            continue
        category = str(raw.get("category", "기타")).strip()
        if category not in CATEGORY_DEFAULT_DAYS:
            category = "기타"
        confidence = int(raw.get("confidence", 70) or 70)
        expiry_days = raw.get("expiry_days")
        if not isinstance(expiry_days, int):
            expiry_days = CATEGORY_DEFAULT_DAYS[category]
        emoji = str(raw.get("emoji") or "🍱")
        items.append(
            {
                "name": name,
                "category": category,
                "emoji": emoji,
                "confidence": max(0, min(100, confidence)),
                "expiry_days": expiry_days,
            }
        )
    return items
