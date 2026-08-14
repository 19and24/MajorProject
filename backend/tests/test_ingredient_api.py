import unittest

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


class IngredientApiTest(unittest.TestCase):
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
        with self.Session() as db:
            db.query(Notification).delete()
            db.query(FridgeItem).delete()
            db.query(Ingredient).delete()
            db.add_all([
                Ingredient(id=12, name="\ub300\ud30c", category="\ucc44\uc18c", emoji="\U0001f331"),
                Ingredient(id=13, name="\uacc4\ub780", category="\uc720\uc81c\ud488\u00b7\uacc4\ub780", emoji="\U0001f95a"),
            ])
            db.commit()

    def test_ingredient_read_apis(self):
        response = self.client.get("/api/v1/ingredients", params={"keyword": "\uacc4\ub780"})
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["data"]["items"][0]["name"], "\uacc4\ub780")
        self.assertEqual(response.json()["data"]["totalElements"], 1)

        detail = self.client.get("/api/v1/ingredients/13")
        self.assertEqual(detail.json()["data"]["emoji"], "\U0001f95a")

        categories = self.client.get("/api/v1/ingredient-categories").json()["data"]
        self.assertEqual({item["name"] for item in categories}, {"\ucc44\uc18c", "\uc720\uc81c\ud488\u00b7\uacc4\ub780"})

    def test_fridge_item_crud(self):
        created = self.client.post("/api/v1/fridge-items", json={
            "ingredientId": 13, "quantity": 6, "unit": "\uac1c",
            "deadline": "2030-08-01T23:59:59", "starred": False,
        })
        self.assertEqual(created.status_code, 201)
        item_id = created.json()["data"]["id"]
        self.assertEqual(created.json()["data"]["ingredient"]["name"], "\uacc4\ub780")

        listed = self.client.get("/api/v1/fridge-items", params={"category": "\uc720\uc81c\ud488\u00b7\uacc4\ub780"})
        self.assertEqual(listed.json()["data"]["totalElements"], 1)

        updated = self.client.patch(f"/api/v1/fridge-items/{item_id}", json={"quantity": 4, "starred": True})
        self.assertEqual(updated.status_code, 200)
        self.assertEqual(updated.json()["data"]["quantity"], 4)
        self.assertTrue(updated.json()["data"]["starred"])

        deleted = self.client.delete(f"/api/v1/fridge-items/{item_id}")
        self.assertEqual(deleted.status_code, 204)
        missing = self.client.get(f"/api/v1/fridge-items/{item_id}")
        self.assertEqual(missing.status_code, 404)
        self.assertEqual(missing.json()["errorCode"], "FRIDGE_ITEM_NOT_FOUND")

    def test_validation_and_missing_ingredient(self):
        invalid = self.client.post("/api/v1/fridge-items", json={
            "ingredientId": 13, "quantity": 0, "unit": "\uac1c"
        })
        self.assertEqual(invalid.status_code, 422)
        self.assertEqual(invalid.json()["errorCode"], "VALIDATION_ERROR")

        missing = self.client.post("/api/v1/fridge-items", json={
            "ingredientId": 999, "quantity": 1, "unit": "\uac1c"
        })
        self.assertEqual(missing.status_code, 404)
        self.assertEqual(missing.json()["errorCode"], "INGREDIENT_NOT_FOUND")


if __name__ == "__main__":
    unittest.main()
