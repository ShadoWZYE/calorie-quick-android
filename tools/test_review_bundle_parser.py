import hashlib
import importlib.util
import json
import tempfile
import unittest
import zipfile
from pathlib import Path

MODULE_PATH = Path(__file__).with_name("review_bundle_parser.py")
SPEC = importlib.util.spec_from_file_location("review_bundle_parser", MODULE_PATH)
PARSER = importlib.util.module_from_spec(SPEC)
assert SPEC and SPEC.loader
SPEC.loader.exec_module(PARSER)


class ReviewBundleParserTest(unittest.TestCase):
    def test_legacy_bundle_is_supported_with_warning(self):
        with tempfile.TemporaryDirectory() as folder:
            archive = Path(folder) / "legacy.zip"
            with zipfile.ZipFile(archive, "w") as output:
                output.writestr("catalogue/catalogue.json", json.dumps({
                    "schema": "calorie-quick-personal-catalogue", "schemaVersion": 1,
                    "foods": [], "recipes": [],
                }))
                output.writestr("support/support.json", json.dumps({
                    "schema": "calorie-quick-support-bundle", "schemaVersion": 5,
                    "feedbackThread": [],
                }))
            report = PARSER.inspect_bundle(archive)
            self.assertTrue(report["valid"])
            self.assertEqual("legacy", report["format"])
            self.assertTrue(report["warnings"])

    def test_v2_media_integrity_is_verified(self):
        jpeg = b"\xff\xd8\xff\xd9"
        digest = hashlib.sha256(jpeg).hexdigest()
        with tempfile.TemporaryDirectory() as folder:
            archive = Path(folder) / "v2.zip"
            with zipfile.ZipFile(archive, "w") as output:
                output.writestr("catalogue/catalogue.json", json.dumps({
                    "schema": "calorie-quick-personal-catalogue", "schemaVersion": 2,
                    "foods": [], "recipes": [],
                }))
                output.writestr("support/support.json", json.dumps({
                    "schema": "calorie-quick-support-bundle", "schemaVersion": 6,
                    "feedbackThread": [],
                }))
                output.writestr("media/test.jpg", jpeg)
                output.writestr("bundle.json", json.dumps({
                    "schema": "calorie-quick-review-bundle", "schemaVersion": 2,
                    "media": [{
                        "path": "media/test.jpg", "sha256": digest, "byteLength": len(jpeg),
                        "width": None, "height": None,
                    }],
                }))
            report = PARSER.inspect_bundle(archive)
            self.assertTrue(report["valid"], report["errors"])
            self.assertEqual("v2", report["format"])

    def test_unsafe_member_fails_validation(self):
        with tempfile.TemporaryDirectory() as folder:
            archive = Path(folder) / "unsafe.zip"
            with zipfile.ZipFile(archive, "w") as output:
                output.writestr("../escape.json", "{}")
            report = PARSER.inspect_bundle(archive)
            self.assertFalse(report["valid"])
            self.assertTrue(any("Unsafe" in error for error in report["errors"]))


if __name__ == "__main__":
    unittest.main()
