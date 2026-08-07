import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from promote_catalogue import promote, run  # noqa: E402


class PromoteCatalogueTest(unittest.TestCase):
    def test_clean_off_record_promotes_while_flagged_and_incomplete_records_quarantine(self):
        with tempfile.TemporaryDirectory() as folder:
            inbox = Path(folder)
            review_dir = inbox / "bundles" / "one"
            review_dir.mkdir(parents=True)
            review_dir.joinpath("review.json").write_text(json.dumps({"items": [{
                "id": "issue", "sourceType": "PRODUCT_DATA_ISSUE", "decision": "NEEDS_REPRODUCTION",
                "description": "barcode=5449000000286; reasons=IMAGE",
            }]}), encoding="utf-8")
            shared = {"foods": {
                "clean": {"displayName": "Cola", "openFoodFactsProduct": self.off_product("5449000000439")},
                "flagged": {"displayName": "Cola", "openFoodFactsProduct": self.off_product("5449000000286")},
                "incomplete": {"displayName": "Unknown", "openFoodFactsProduct": {"code": "0402004014227"}},
            }}

            promoted, recipes, quarantined = promote(shared, inbox)

            self.assertEqual(["off-5449000000439"], [item["id"] for item in promoted])
            self.assertFalse(recipes)
            self.assertEqual("OPEN_FOOD_FACTS", promoted[0]["catalogueOrigin"])
            self.assertFalse(promoted[0]["verified"])
            self.assertTrue(promoted[0]["sourceId"].startswith("cq-promoted:OPEN_FOOD_FACTS:"))
            self.assertEqual(2, len(quarantined))
            self.assertTrue(any("explicitly flagged" in reason for item in quarantined for reason in item["reasons"]))

    def test_opt_in_and_verification_are_independent_metadata(self):
        with tempfile.TemporaryDirectory() as folder:
            inbox = Path(folder)
            (inbox / "bundles").mkdir()
            personal = {
                "id": "personal-good",
                "name": {"en": "Soup", "ro": "Supă"},
                "brand": "Home",
                "reviewStatusAtExport": "EXPORTED",
                "selectedForThisExport": True,
                "category": "prepared-meals",
                "nutritionPer100g": {"calories": 80, "proteinGrams": 5, "carbsGrams": 8, "fatGrams": 3, "fiberGrams": 1},
                "measures": [{"label": {"en": "1 bowl", "ro": "1 bol"}, "grams": 300}],
            }
            promoted, recipes, quarantined = promote({"foods": {"personal": {"normalizedFood": personal}}}, inbox)

            self.assertFalse(quarantined)
            self.assertFalse(recipes)
            self.assertEqual("OPT_IN", promoted[0]["catalogueOrigin"])
            self.assertFalse(promoted[0]["verified"])
            self.assertTrue(promoted[0]["sourceId"].startswith("cq-promoted:OPT_IN:"))

    def test_explicit_verification_awards_verified_identity(self):
        with tempfile.TemporaryDirectory() as folder:
            inbox = Path(folder)
            (inbox / "bundles").mkdir()
            inbox.joinpath("catalogue-verification.json").write_text(json.dumps({"verified": [{
                "id": "off-5449000000439", "reviewedAt": "2026-08-07T12:00:00Z", "reviewedBy": "Calorie Quick",
                "checks": {"nutrition": True, "representativeName": True, "imageRightsAndFit": True},
            }]}), encoding="utf-8")

            promoted, recipes, quarantined = promote({"foods": {
                "clean": {"openFoodFactsProduct": self.off_product("5449000000439")},
            }}, inbox)

            self.assertFalse(quarantined)
            self.assertFalse(recipes)
            self.assertTrue(promoted[0]["verified"])
            self.assertTrue(promoted[0]["sourceId"].startswith("cq-verified:OPEN_FOOD_FACTS:"))

    def test_incomplete_verification_checklist_does_not_award_badge(self):
        with tempfile.TemporaryDirectory() as folder:
            inbox = Path(folder)
            (inbox / "bundles").mkdir()
            inbox.joinpath("catalogue-verification.json").write_text(json.dumps({"verified": [{
                "id": "off-5449000000439", "reviewedAt": "2026-08-07T12:00:00Z", "reviewedBy": "Calorie Quick",
                "checks": {"nutrition": True, "representativeName": True, "imageRightsAndFit": False},
            }]}), encoding="utf-8")

            promoted, recipes, quarantined = promote({"foods": {
                "clean": {"openFoodFactsProduct": self.off_product("5449000000439")},
            }}, inbox)

            self.assertFalse(recipes)
            self.assertFalse(quarantined)
            self.assertFalse(promoted[0]["verified"])
            self.assertTrue(promoted[0]["sourceId"].startswith("cq-promoted:"))

    def test_opt_in_recipe_promotes_as_recipe_and_searchable_food(self):
        with tempfile.TemporaryDirectory() as folder:
            inbox = Path(folder)
            (inbox / "bundles").mkdir()
            recipe = {
                "foodId": "recipe-coffee", "name": "My coffee",
                "reviewStatusAtExport": "READY_FOR_REVIEW", "selectedForThisExport": True,
                "template": {
                    "cookedYieldGrams": 251, "portionCount": 1,
                    "ingredients": [{
                        "foodId": "black-coffee", "name": {"en": "Black coffee", "ro": "Cafea neagră"},
                        "grams": 240,
                        "nutritionPer100g": {"calories": 1, "proteinGrams": 0.1, "carbsGrams": 0, "fatGrams": 0, "fiberGrams": 0},
                        "allergens": [],
                    }, {
                        "foodId": "honey", "name": {"en": "Honey", "ro": "Miere"}, "grams": 11,
                        "nutritionPer100g": {"calories": 304, "proteinGrams": 0.3, "carbsGrams": 82.4, "fatGrams": 0, "fiberGrams": 0.2},
                        "allergens": [],
                    }],
                },
            }

            foods, recipes, quarantined = promote({"foods": {}, "recipes": {
                "recipe-coffee": {"displayName": "My coffee", "normalizedRecipe": recipe},
            }}, inbox)

            self.assertFalse(quarantined)
            self.assertEqual("recipe-coffee", foods[0]["id"])
            self.assertEqual("recipe-coffee", recipes[0]["foodId"])
            self.assertEqual("Black coffee with honey", recipes[0]["name"])
            self.assertIn("My coffee", foods[0]["aliases"])
            self.assertEqual(2, len(recipes[0]["ingredients"]))
            self.assertEqual("OPT_IN", recipes[0]["catalogueOrigin"])
            self.assertFalse(recipes[0]["verified"])

    def test_run_writes_catalogue_and_quarantine_report(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            (root / "bundles").mkdir()
            (root / "shared-food-catalogue.json").write_text(json.dumps({"foods": {
                "clean": {"openFoodFactsProduct": self.off_product("5449000000439")},
            }}), encoding="utf-8")
            output = root / "promoted.json"
            report_path = root / "report.json"

            report = run(root, output, report_path)

            self.assertEqual(1, report["promotedCount"])
            self.assertEqual("MIXED-PER-RECORD", json.loads(output.read_text(encoding="utf-8"))["source"]["license"])
            self.assertTrue(report_path.is_file())

    @staticmethod
    def off_product(code: str) -> dict:
        return {
            "code": code,
            "product_name": "Coca-Cola",
            "product_name_ro": "Coca-Cola",
            "brands": "Coca-Cola",
            "quantity": "2 L",
            "product_quantity": 2000,
            "product_quantity_unit": "ml",
            "categories_tags": ["en:beverages", "en:sodas", "en:colas"],
            "nutriments": {
                "energy-kcal_100g": 42,
                "proteins_100g": 0,
                "carbohydrates_100g": 10.6,
                "fat_100g": 0,
                "fiber_100g": 0,
            },
            "image_front_small_url": "https://images.openfoodfacts.org/test.jpg",
        }


if __name__ == "__main__":
    unittest.main()
