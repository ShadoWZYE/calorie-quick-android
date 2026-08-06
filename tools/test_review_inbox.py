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
            "schemaVersion": 7,
            "feedbackThread": [{
                "id": "feedback-1",
                "text": "Scrolling freezes until results load.",
                "imageBundlePath": media_path,
            }],
            "diagnostics": [],
            "performance": [],
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
            self.assertTrue(Path(feedback["attachments"][0]).is_file())
            feedback["reviewerComments"] = "Reproduced on the test phone."
            store.save_review(review)

            duplicate, created = store.import_bundle(archive)

            self.assertFalse(created)
            saved_feedback = next(item for item in duplicate["items"] if item["sourceType"] == "FEEDBACK")
            self.assertEqual("Reproduced on the test phone.", saved_feedback["reviewerComments"])

    def test_brief_requires_explicit_acceptance_and_promotion(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            store = ReviewInboxStore(root / "inbox")
            review, _ = store.import_bundle(self.create_bundle(root))
            accepted, deferred = review["items"]
            accepted.update(decision="ACCEPT_NEXT", promote=True, reviewerComments="Ship after regression test.")
            deferred.update(decision="DEFERRED", promote=True)
            output = root / "brief.json"

            brief = store.export_implementation_brief([review], output)

            self.assertEqual(1, brief["itemCount"])
            self.assertEqual(accepted["id"], brief["items"][0]["id"])
            self.assertTrue(output.is_file())
            self.assertTrue(output.with_suffix(".md").is_file())


if __name__ == "__main__":
    unittest.main()
