import hashlib
import json
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from review_inbox_core import ReviewInboxStore  # noqa: E402


class ReviewInboxStoreTest(unittest.TestCase):
    def create_bundle(self, folder: Path) -> Path:
        archive = folder / "review.zip"
        jpeg = b"\xff\xd8\xff\xd9"
        media_path = "support/feedback-images/context.jpg"
        catalogue = {
            "schema": "calorie-quick-personal-catalogue",
            "schemaVersion": 2,
            "foods": [{
                "id": "food-1",
                "name": {"en": "Test food", "ro": "Aliment test"},
                "category": None,
                "measures": [],
                "nutritionPer100g": {
                    "calories": 100, "proteinGrams": 4, "carbsGrams": 16,
                    "fatGrams": 2, "fiberGrams": 0,
                },
                "image": None,
            }],
            "recipes": [],
        }
        support = {
            "schema": "calorie-quick-support-bundle",
            "schemaVersion": 8,
            "feedbackThread": [{
                "id": "feedback-1",
                "text": "Scrolling freezes until results load.",
                "imageBundlePath": media_path,
            }],
            "diagnostics": [],
            "performance": [],
            "productDataIssues": [{
                "id": "issue-1",
                "barcode": "12345670",
                "productName": "Cached food",
                "brand": "Example",
                "reasonCodes": ["NUTRITION", "SERVING_PACKAGE"],
                "comment": "Package size and calories are wrong.",
            }],
        }
        root = {
            "schema": "calorie-quick-review-bundle",
            "schemaVersion": 2,
            "app": {"versionName": "test", "buildId": "test-build"},
            "media": [{
                "path": media_path,
                "sha256": hashlib.sha256(jpeg).hexdigest(),
                "byteLength": len(jpeg),
                "width": None,
                "height": None,
            }],
        }
        with zipfile.ZipFile(archive, "w") as output:
            output.writestr("catalogue/catalogue.json", json.dumps(catalogue))
            output.writestr("support/support.json", json.dumps(support))
            output.writestr("bundle.json", json.dumps(root))
            output.writestr(media_path, jpeg)
            response_path = "open-food-facts/responses/test.json"
            output.writestr("open-food-facts/cache.json", json.dumps({
                "schema": "calorie-quick-open-food-facts-cache", "schemaVersion": 1,
                "responseCount": 1, "responses": [response_path],
            }))
            output.writestr(response_path, json.dumps({
                "schema": "calorie-quick-open-food-facts-response", "schemaVersion": 1,
                "response": {"product": {
                    "code": "12345670", "product_name": "Cached food",
                    "nutriments": {"fiber_100g": 2}, "categories": "Snacks",
                }},
            }))
        return archive

    def test_import_deduplicates_and_preserves_review_edits(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            store = ReviewInboxStore(root / "inbox")
            archive = self.create_bundle(root)

            review, created = store.import_bundle(archive)

            self.assertTrue(created)
            self.assertEqual(2, len(review["items"]))
            feedback = next(item for item in review["items"] if item["sourceType"] == "FEEDBACK")
            self.assertEqual("PERFORMANCE", feedback["classification"])
            self.assertTrue(feedback["assistantAnalysis"])
            self.assertTrue(Path(feedback["attachments"][0]).is_file())
            self.assertFalse(any(item["sourceType"] == "OPEN_FOOD_FACTS_CACHE" for item in review["items"]))
            product_issue = next(item for item in review["items"] if item["sourceType"] == "PRODUCT_DATA_ISSUE")
            self.assertIn("NUTRITION", product_issue["description"])
            self.assertIn("open-food-facts/responses/test.json", product_issue["description"])
            self.assertTrue((root / "inbox" / "bundles" / review["bundleHash"] / "source.zip").is_file())
            shared = json.loads((root / "inbox" / "shared-food-catalogue.json").read_text(encoding="utf-8"))
            self.assertEqual(2, shared["foodCount"])
            self.assertIn("barcode:12345670", shared["foods"])
            feedback["reviewerComments"] = "Reproduced on the test phone."
            store.save_review(review)

            duplicate, created = store.import_bundle(archive)

            self.assertFalse(created)
            saved_feedback = next(item for item in duplicate["items"] if item["sourceType"] == "FEEDBACK")
            self.assertEqual("Reproduced on the test phone.", saved_feedback["reviewerComments"])

    def test_brief_contains_only_next_items(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            store = ReviewInboxStore(root / "inbox")
            review, _ = store.import_bundle(self.create_bundle(root))
            accepted = review["items"][0]
            deferred = store.add_manual_item(review)
            accepted.update(decision="ACCEPT_NEXT", reviewerComments="Ship after regression test.")
            deferred.update(decision="ACCEPT_BACKLOG", promote=True)
            output = root / "brief.json"

            brief = store.export_implementation_brief([review], output)

            self.assertEqual(1, brief["itemCount"])
            self.assertEqual(accepted["id"], brief["items"][0]["id"])
            self.assertTrue(brief["items"][0]["assistantAnalysis"])
            self.assertTrue(output.is_file())
            self.assertTrue(output.with_suffix(".md").is_file())

    def test_implemented_items_move_to_testing_without_regressing_done(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            status_path = root / "implementation-status.json"
            status_path.write_text(json.dumps({
                "items": {
                    "feedback:testing": {"state": "TESTING", "reference": "0.2.0-alpha09"},
                    "feedback:done": {"state": "TESTING", "reference": "0.2.0-alpha08"},
                },
            }), encoding="utf-8")
            store = ReviewInboxStore(root / "inbox", status_path)
            review = {
                "bundleHash": "fixture",
                "items": [
                    {"id": "feedback:testing", "decision": "ACCEPT_NEXT", "status": "READY", "promote": True},
                    {"id": "feedback:done", "decision": "RESOLVED", "status": "RESOLVED", "promote": False},
                ],
            }
            store.save_review(review)

            loaded = store.list_reviews()[0]

            testing, done = loaded["items"]
            self.assertEqual("NEEDS_REPRODUCTION", testing["decision"])
            self.assertEqual("NEEDS_INFO", testing["status"])
            self.assertFalse(testing["promote"])
            self.assertEqual("0.2.0-alpha09", testing["linkedReference"])
            self.assertEqual("RESOLVED", done["decision"])


if __name__ == "__main__":
    unittest.main()
