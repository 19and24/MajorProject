import unittest
from datetime import datetime, timedelta

from fastapi.testclient import TestClient
from sqlalchemy import create_engine, event
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from app.database import Base
from app.dependencies import get_db
from app.main import app
from app.models.fridge_item import FridgeItem
from app.models.ingredient import Ingredient
from app.models.notification import Notification
from app.models.recipe import Recipe
from app.models.recipe_ingredient import RecipeIngredient


def iso(dt: datetime) -> str:
    return dt.isoformat()


class FullApiTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.engine = create_engine(
            "sqlite://", connect_args={"check_same_thread": False}, poolclass=StaticPool
        )

        @event.listens_for(cls.engine, "connect")
        def enable_foreign_keys(connection, _):
            connection.execute("PRAGMA foreign_keys=ON")

        cls.Session = sessionmaker(bind=cls.engine)
        Base.metadata.create_all(cls.engine)

        def override_db():
            db = cls.Session()
            try:
                yield db
            finally:
                db.close()

        app.dependency_overrides[get_db] = override_db
        cls.client = TestClient(app)

    @classmethod
    def tearDownClass(cls):
        app.dependency_overrides.clear()
        Base.metadata.drop_all(cls.engine)
        cls.engine.dispose()

    def setUp(self):
        self.now = datetime.now()
        with self.Session() as db:
            db.query(RecipeIngredient).delete()
            db.query(Recipe).delete()
            db.query(Notification).delete()
            db.query(FridgeItem).delete()
            db.query(Ingredient).delete()

            green_onion = Ingredient(id=12, name="대파", category="채소", emoji="🌱")
            egg = Ingredient(id=13, name="계란", category="유제품·계란", emoji="🥚")
            soy_sauce = Ingredient(id=40, name="국간장", category="조미료", emoji="🧂")
            db.add_all([green_onion, egg, soy_sauce])
            db.flush()

            recipe = Recipe(
                id=205, title="계란 대파국", method="끓이기", ingredient_acto="기타",
                type="국", ingredient_all="대파 20g, 계란 2개, 국간장 1큰술",
                portion="1인분", cooktime="15분",
            )
            db.add(recipe)
            db.flush()
            db.add_all([
                RecipeIngredient(
                    recipe_id=recipe.id,
                    ingredient_id=green_onion.id,
                    original_name="대파 흰부분",
                    amount="20g",
                ),
                RecipeIngredient(
                    recipe_id=recipe.id,
                    ingredient_id=egg.id,
                    original_name="계란",
                    amount="2개",
                ),
                RecipeIngredient(
                    recipe_id=recipe.id,
                    ingredient_id=soy_sauce.id,
                    original_name="국간장",
                    amount="1큰술",
                ),
            ])

            expiring_item = FridgeItem(
                id=21, ingredient_id=green_onion.id, quantity=2, unit="개",
                deadline=self.now + timedelta(days=1), starred=True,
                created_at=self.now, updated_at=self.now,
            )
            fresh_item = FridgeItem(
                id=22, ingredient_id=egg.id, quantity=6, unit="개",
                deadline=self.now + timedelta(days=10), starred=False,
                created_at=self.now, updated_at=self.now,
            )
            expired_item = FridgeItem(
                id=23, ingredient_id=egg.id, quantity=1, unit="개",
                deadline=self.now - timedelta(days=2), starred=False,
                created_at=self.now, updated_at=self.now,
            )
            db.add_all([expiring_item, fresh_item, expired_item])
            db.commit()

    # --- fridge item star / expiring / consume ---

    def test_star_toggle(self):
        res = self.client.patch("/api/v1/fridge-items/22/star", json={"starred": True})
        self.assertEqual(res.status_code, 200)
        self.assertEqual(res.json()["data"], {"id": 22, "starred": True})

    def test_expiring_endpoint(self):
        res = self.client.get("/api/v1/fridge-items/expiring", params={"days": 3})
        self.assertEqual(res.status_code, 200)
        ids = {row["id"] for row in res.json()["data"]}
        self.assertIn(21, ids)
        self.assertNotIn(23, ids)  # expired, excluded by default

        res2 = self.client.get(
            "/api/v1/fridge-items/expiring", params={"days": 3, "includeExpired": True}
        )
        ids2 = {row["id"] for row in res2.json()["data"]}
        self.assertIn(23, ids2)

    def test_consume(self):
        res = self.client.post("/api/v1/fridge-items/22/consume", json={"quantity": 6})
        self.assertEqual(res.status_code, 200)
        self.assertEqual(res.json()["data"]["remainingQuantity"], 0)
        self.assertTrue(res.json()["data"]["depleted"])

        over = self.client.post("/api/v1/fridge-items/22/consume", json={"quantity": 1})
        self.assertEqual(over.status_code, 422)
        self.assertEqual(over.json()["errorCode"], "INSUFFICIENT_QUANTITY")

    # --- notifications ---

    def test_notification_crud(self):
        notify_time = iso(self.now + timedelta(days=1))
        created = self.client.post("/api/v1/notifications", json={
            "fridgeItemId": 21, "title": "대파 유통기한 임박",
            "content": "대파의 유통기한이 하루 남았습니다.", "notifyTime": notify_time,
        })
        self.assertEqual(created.status_code, 201)
        notification_id = created.json()["data"]["id"]

        listed = self.client.get("/api/v1/notifications", params={"fridgeItemId": 21})
        self.assertEqual(listed.json()["data"]["totalElements"], 1)

        detail = self.client.get(f"/api/v1/notifications/{notification_id}")
        self.assertEqual(detail.status_code, 200)
        self.assertEqual(detail.json()["data"]["fridgeItem"]["ingredient"]["name"], "대파")

        past_notify = self.client.post("/api/v1/notifications", json={
            "fridgeItemId": 21, "title": "invalid",
            "notifyTime": iso(self.now - timedelta(days=1)),
        })
        self.assertEqual(past_notify.status_code, 422)
        self.assertEqual(past_notify.json()["errorCode"], "INVALID_NOTIFY_TIME")

        deleted = self.client.delete(f"/api/v1/notifications/{notification_id}")
        self.assertEqual(deleted.status_code, 204)

    def test_default_notifications(self):
        res = self.client.post(
            "/api/v1/fridge-items/22/notifications/default",
            json={"daysBefore": [3, 1], "notifyHour": 9},
        )
        self.assertEqual(res.status_code, 200)
        created = res.json()["data"]["createdNotifications"]
        self.assertEqual(len(created), 2)

        # Re-running should skip duplicates.
        res2 = self.client.post(
            "/api/v1/fridge-items/22/notifications/default",
            json={"daysBefore": [3, 1], "notifyHour": 9},
        )
        self.assertEqual(res2.json()["data"]["createdNotifications"], [])

        no_deadline = self.client.post(
            "/api/v1/fridge-items/21/notifications/default",
            json={"daysBefore": [1], "notifyHour": 9},
        )
        self.assertEqual(no_deadline.status_code, 200)  # item 21 has a deadline; should succeed
        self.assertEqual(no_deadline.status_code, 200)

    # --- recipes ---

    def test_recipe_list_and_detail(self):
        listed = self.client.get("/api/v1/recipes", params={"keyword": "대파국"})
        self.assertEqual(listed.status_code, 200)
        self.assertEqual(listed.json()["data"]["totalElements"], 1)

        detail = self.client.get("/api/v1/recipes/205")
        self.assertEqual(detail.status_code, 200)
        names = {i["ingredient"]["name"] for i in detail.json()["data"]["ingredients"]}
        self.assertEqual(names, {"대파", "계란", "국간장"})
        green_onion = next(
            item
            for item in detail.json()["data"]["ingredients"]
            if item["ingredient"]["name"] == "대파"
        )
        self.assertEqual(green_onion["originalName"], "대파 흰부분")

        ingredients = self.client.get("/api/v1/recipes/205/ingredients")
        self.assertEqual(len(ingredients.json()["data"]), 3)

        external = self.client.get("/api/v1/recipes/205/external-search")
        self.assertEqual(external.json()["data"]["provider"], "10000RECIPE")
        self.assertIn("10000recipe.com", external.json()["data"]["url"])

    # --- recommendations ---

    def test_selectable_fridge_items(self):
        res = self.client.get("/api/v1/recommendations/fridge-items")
        self.assertEqual(res.status_code, 200)
        by_id = {row["fridgeItemId"]: row for row in res.json()["data"]["items"]}
        self.assertEqual(by_id[21]["status"], "EXPIRING")
        self.assertEqual(by_id[22]["status"], "FRESH")
        self.assertEqual(by_id[23]["status"], "EXPIRED")
        self.assertFalse(by_id[23]["selectable"])

    def test_recommend_recipes_success(self):
        res = self.client.post("/api/v1/recommendations/recipes", json={
            "selectedFridgeItemIds": [21, 22],
            "prioritizeExpiring": True,
        })
        self.assertEqual(res.status_code, 200)
        data = res.json()["data"]
        self.assertEqual(len(data["selectedFridgeItems"]), 2)
        self.assertTrue(len(data["recommendations"]) >= 1)
        top = data["recommendations"][0]
        self.assertEqual(top["recipe"]["id"], 205)
        self.assertTrue(top["expiringTargeted"])
        ownerships = {i["name"]: i["ownershipStatus"] for i in top["ingredients"]}
        self.assertEqual(ownerships["대파"], "EXPIRING")
        self.assertEqual(ownerships["계란"], "AVAILABLE")
        self.assertEqual(ownerships["국간장"], "MISSING")
        green_onion = next(item for item in top["ingredients"] if item["name"] == "대파")
        self.assertEqual(green_onion["originalName"], "대파 흰부분")
        self.assertIn("10000recipe.com", top["externalSearch"]["url"])

    def test_recommend_requires_selection(self):
        res = self.client.post("/api/v1/recommendations/recipes", json={
            "selectedFridgeItemIds": [],
        })
        self.assertEqual(res.status_code, 422)
        self.assertEqual(res.json()["errorCode"], "RECOMMENDATION_INGREDIENT_REQUIRED")

    def test_recommend_rejects_expired_selection(self):
        res = self.client.post("/api/v1/recommendations/recipes", json={
            "selectedFridgeItemIds": [23],
        })
        self.assertEqual(res.status_code, 422)
        self.assertEqual(res.json()["errorCode"], "EXPIRED_FRIDGE_ITEM_SELECTED")
        self.assertEqual(res.json()["data"]["invalidFridgeItemIds"], [23])

    def test_recommend_empty_result_is_200(self):
        # A fridge item whose ingredient has no recipes at all -> empty recommendations.
        with self.Session() as db:
            lonely = Ingredient(id=99, name="특이재료", category="기타", emoji=None)
            db.add(lonely)
            db.flush()
            item = FridgeItem(
                id=50, ingredient_id=lonely.id, quantity=1, unit="개",
                deadline=self.now + timedelta(days=5), starred=False,
                created_at=self.now, updated_at=self.now,
            )
            db.add(item)
            db.commit()

        res = self.client.post("/api/v1/recommendations/recipes", json={
            "selectedFridgeItemIds": [50],
        })
        self.assertEqual(res.status_code, 200)
        self.assertEqual(res.json()["data"]["recommendations"], [])
        self.assertIn("emptyReason", res.json()["data"])

    def test_filter_endpoint_matches_recommend(self):
        res = self.client.post("/api/v1/recommendations/recipes/filter", json={
            "selectedFridgeItemIds": [21, 22],
        })
        self.assertEqual(res.status_code, 200)
        self.assertTrue(len(res.json()["data"]["recommendations"]) >= 1)

    # --- dashboard ---

    def test_dashboard(self):
        res = self.client.get("/api/v1/dashboard")
        self.assertEqual(res.status_code, 200)
        data = res.json()["data"]
        self.assertEqual(data["totalFridgeItemCount"], 3)
        self.assertEqual(data["starredFridgeItemCount"], 1)
        self.assertEqual(data["expiredCount"], 1)
        self.assertTrue(data["expiringSoonCount"] >= 1)
        self.assertIsInstance(data["expiringItems"], list)
        self.assertIsInstance(data["recommendedRecipes"], list)


if __name__ == "__main__":
    unittest.main()
