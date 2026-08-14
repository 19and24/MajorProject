from datetime import datetime, timedelta

from sqlalchemy.orm import Session

from app.crud import dashboard as dashboard_crud
from app.crud import fridge_item as fridge_item_crud
from app.crud import recommendation as recommendation_crud
from app.services.recommendation import score_recipe, stock_status

DEFAULT_EXPIRING_DAYS = 3
DEFAULT_RECOMMENDATION_COUNT = 5


def _serialize_expiring_row(item, *, now: datetime) -> dict:
    return {
        "fridgeItemId": item.id,
        "ingredientName": item.ingredient.name,
        "deadline": item.deadline,
        "daysUntilDeadline": (item.deadline.date() - now.date()).days,
    }


def get_dashboard(db: Session) -> dict:
    now = datetime.now()
    threshold = now + timedelta(days=DEFAULT_EXPIRING_DAYS)

    expiring_items = fridge_item_crud.get_expiring(
        db, days=DEFAULT_EXPIRING_DAYS, include_expired=False, now=now
    )

    non_expired_items = [
        item for item in recommendation_crud.get_fridge_items(
            db, starred=None, category=None, include_expired=False
        )
        if stock_status(item.deadline, now) != "EXPIRED"
    ]

    recommended_recipes: list[dict] = []
    if non_expired_items:
        selected_by_ingredient = {item.ingredient.id: item for item in non_expired_items}
        ingredient_ids = list(selected_by_ingredient.keys())
        candidate_recipes = recommendation_crud.get_candidate_recipes(
            db, ingredient_ids, limit=DEFAULT_RECOMMENDATION_COUNT * 3
        )
        ingredients_by_recipe = recommendation_crud.get_ingredients_for_recipes(
            db, [recipe.id for recipe in candidate_recipes]
        )
        scored = [
            score_recipe(recipe, ingredients_by_recipe.get(recipe.id, []), selected_by_ingredient, now)
            for recipe in candidate_recipes
        ]
        scored.sort(key=lambda r: -r["score"])
        recommended_recipes = [
            {"id": row["recipe"]["id"], "title": row["recipe"]["title"], "score": row["score"]}
            for row in scored[:DEFAULT_RECOMMENDATION_COUNT]
        ]

    return {
        "totalFridgeItemCount": dashboard_crud.count_fridge_items(db),
        "starredFridgeItemCount": dashboard_crud.count_starred_fridge_items(db),
        "expiringSoonCount": dashboard_crud.count_expiring(db, now=now, threshold=threshold),
        "expiredCount": dashboard_crud.count_expired(db, now=now),
        "upcomingNotificationCount": dashboard_crud.count_upcoming_notifications(db, now=now),
        "expiringItems": [_serialize_expiring_row(item, now=now) for item in expiring_items],
        "recommendedRecipes": recommended_recipes,
    }
