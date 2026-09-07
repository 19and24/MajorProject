from datetime import datetime

from sqlalchemy.orm import Session

from app.core.responses import api_error
from app.crud import recommendation as recommendation_crud
from app.models.fridge_item import FridgeItem
from app.models.recipe import Recipe
from app.schemas.recommendation import RecommendRecipesRequest
from app.services import external_search

EXPIRING_THRESHOLD_DAYS = 3


def _days_until(deadline: datetime | None, now: datetime) -> int | None:
    if deadline is None:
        return None
    return (deadline.date() - now.date()).days


def stock_status(deadline: datetime | None, now: datetime) -> str:
    days = _days_until(deadline, now)
    if days is None:
        return "FRESH"
    if days < 0:
        return "EXPIRED"
    if days <= EXPIRING_THRESHOLD_DAYS:
        return "EXPIRING"
    return "FRESH"


def serialize_selectable(item: FridgeItem, *, now: datetime) -> dict:
    status = stock_status(item.deadline, now)
    expired = status == "EXPIRED"
    return {
        "fridgeItemId": item.id,
        "ingredient": {
            "id": item.ingredient.id,
            "name": item.ingredient.name,
            "category": item.ingredient.category,
            "emoji": item.ingredient.emoji,
        },
        "quantity": float(item.quantity),
        "unit": item.unit,
        "deadline": item.deadline,
        "daysUntilDeadline": _days_until(item.deadline, now),
        "status": status,
        "selectable": not expired,
        "selectedByDefault": not expired,
        **({"disabledReason": "소비기한이 지난 식재료는 추천에 사용할 수 없습니다."} if expired else {}),
    }


def list_selectable_fridge_items(db: Session, *, starred: bool | None, category: str | None,
                                  include_expired: bool) -> dict:
    now = datetime.now()
    items = recommendation_crud.get_fridge_items(
        db, starred=starred, category=category, include_expired=include_expired
    )
    serialized = [serialize_selectable(item, now=now) for item in items]
    if not include_expired:
        serialized = [row for row in serialized if row["status"] != "EXPIRED"]
    return {"items": serialized}


def _validate_selection(db: Session, request: RecommendRecipesRequest, *, now: datetime) -> list[FridgeItem]:
    if not request.selected_fridge_item_ids:
        raise api_error(
            422, "냉털에 사용할 식재료를 하나 이상 선택해주세요.", "RECOMMENDATION_INGREDIENT_REQUIRED",
        )
    selected_items = recommendation_crud.get_fridge_items_by_ids(db, request.selected_fridge_item_ids)
    found_ids = {item.id for item in selected_items}
    missing_ids = [i for i in request.selected_fridge_item_ids if i not in found_ids]
    if missing_ids:
        raise api_error(404, "냉장고 재고를 찾을 수 없습니다.", "FRIDGE_ITEM_NOT_FOUND")

    expired_ids = [item.id for item in selected_items if stock_status(item.deadline, now) == "EXPIRED"]
    if expired_ids:
        raise api_error(
            422, "소비기한이 지난 식재료는 선택할 수 없습니다.", "EXPIRED_FRIDGE_ITEM_SELECTED",
            data={"invalidFridgeItemIds": expired_ids},
        )
    return selected_items


def score_recipe(recipe: Recipe, links, selected_by_ingredient: dict[int, FridgeItem],
                   now: datetime) -> dict:
    ingredients_out = []
    available_count = 0
    expiring_used = False
    scoreable_links = [
        link
        for link in links
        if not link.optional and link.ingredient.category != "기본재료"
    ]
    scoreable_ids = {link.ingredient_id for link in scoreable_links}
    total = len(scoreable_links)

    for link in links:
        fridge_item = selected_by_ingredient.get(link.ingredient_id)
        if fridge_item is not None:
            status = stock_status(fridge_item.deadline, now)
            ownership = "EXPIRING" if status == "EXPIRING" else "AVAILABLE"
            if ownership == "EXPIRING":
                expiring_used = True
            if link.ingredient_id in scoreable_ids:
                available_count += 1
            fridge_item_id = fridge_item.id
        else:
            ownership = "MISSING"
            fridge_item_id = None

        ingredients_out.append({
            "ingredientId": link.ingredient_id,
            "name": link.ingredient.name,
            "originalName": link.original_name or link.ingredient.name,
            "amount": link.amount,
            "ownershipStatus": ownership,
            "fridgeItemId": fridge_item_id,
        })

    match_rate = (available_count / total) if total else 0.0
    score = match_rate + (0.15 if expiring_used else 0.0)

    return {
        "recipe": {
            "id": recipe.id,
            "title": recipe.title,
            "imageUrl": None,
            "method": recipe.method,
            "type": recipe.type,
            "portion": recipe.portion,
            "cooktime": recipe.cooktime,
        },
        "score": round(min(score, 1.0), 4),
        "matchRate": round(match_rate, 4),
        "expiringTargeted": expiring_used,
        "recommendationReason": _build_reason(recipe, ingredients_out, expiring_used),
        "ingredients": ingredients_out,
        "externalSearch": {
            "provider": external_search.PROVIDER,
            "url": external_search.build_url(recipe.title),
        },
    }


def _build_reason(recipe: Recipe, ingredients: list[dict], expiring_used: bool) -> str:
    have = [i["name"] for i in ingredients if i["ownershipStatus"] in ("AVAILABLE", "EXPIRING")]
    if not have:
        return f"{recipe.title} 레시피를 추천합니다."
    joined = ", ".join(have[:3])
    if expiring_used:
        return f"소비기한이 임박한 재료를 포함해 보유 중인 {joined}을(를) 활용할 수 있습니다."
    return f"보유 중인 {joined}을(를) 활용할 수 있습니다."


def recommend_recipes(db: Session, request: RecommendRecipesRequest) -> dict:
    now = datetime.now()
    selected_items = _validate_selection(db, request, now=now)

    excluded_ids = set(request.excluded_fridge_item_ids)
    active_items = [item for item in selected_items if item.id not in excluded_ids]

    selected_summary = [
        {
            "fridgeItemId": item.id,
            "ingredientId": item.ingredient.id,
            "name": item.ingredient.name,
            "status": stock_status(item.deadline, now),
        }
        for item in selected_items
    ]

    if not active_items:
        return {
            "selectedFridgeItems": selected_summary,
            "recommendations": [],
            "emptyReason": "선택한 식재료로 추천할 수 있는 레시피가 없습니다.",
            "recommendationMode": "RULE_BASED_FALLBACK",
        }

    selected_by_ingredient = {item.ingredient.id: item for item in active_items}
    ingredient_ids = list(selected_by_ingredient.keys())

    candidate_recipes = recommendation_crud.get_candidate_recipes(
        db, ingredient_ids, limit=max(request.limit * 5, 50)
    )
    if not candidate_recipes:
        return {
            "selectedFridgeItems": selected_summary,
            "recommendations": [],
            "emptyReason": "선택한 식재료로 추천할 수 있는 레시피가 없습니다.",
            "recommendationMode": "RULE_BASED_FALLBACK",
        }

    recipe_ids = [recipe.id for recipe in candidate_recipes]
    ingredients_by_recipe = recommendation_crud.get_ingredients_for_recipes(db, recipe_ids)

    scored = [
        score_recipe(recipe, ingredients_by_recipe.get(recipe.id, []), selected_by_ingredient, now)
        for recipe in candidate_recipes
    ]

    if request.prioritize_expiring:
        scored.sort(key=lambda r: (not r["expiringTargeted"], -r["score"]))
    else:
        scored.sort(key=lambda r: -r["score"])

    recommendations = scored[: request.limit]

    return {
        "selectedFridgeItems": selected_summary,
        "recommendations": recommendations,
        "recommendationMode": "RULE_BASED_FALLBACK",
    }
