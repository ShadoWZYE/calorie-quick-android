import sys
import unittest
from pathlib import Path

from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).parent))
from validate_catalogue_images import looks_like_stacked_tile, validate_directory  # noqa: E402


class CatalogueImageValidationTest(unittest.TestCase):
    def test_checked_in_catalogue_images_are_clean(self):
        directory = Path(__file__).parents[1] / "app/src/main/res/drawable-nodpi"
        self.assertEqual([], validate_directory(directory))

    def test_displaced_stacked_card_pattern_is_detected(self):
        image = Image.new("RGB", (256, 256), "#eee5fa")
        draw = ImageDraw.Draw(image)
        draw.rounded_rectangle((8, -30, 248, 30), radius=12, fill="#fbf7fb")
        draw.rounded_rectangle((8, 42, 248, 250), radius=12, fill="#fbf7fb")
        self.assertTrue(looks_like_stacked_tile(image))


if __name__ == "__main__":
    unittest.main()
