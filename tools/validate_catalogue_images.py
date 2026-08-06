#!/usr/bin/env python3
"""Validate built-in food thumbnail dimensions and detect displaced sprite tiles."""

from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image

EXPECTED_SIZE = (256, 256)


def looks_like_stacked_tile(image: Image.Image) -> bool:
    """Detect the characteristic previous-card strip found in one generated batch."""
    rgb = image.convert("RGB")
    if rgb.size != EXPECTED_SIZE:
        return False

    def luminance_delta(y: int) -> float:
        edge = sum(rgb.getpixel((0, y))) / 3
        center = sum(rgb.getpixel((rgb.width // 2, y))) / 3
        return center - edge

    # A rounded previous tile occupies the top, collapses to the gutter near y=35,
    # and a second rounded tile starts around y=42-50.
    return (
        max(luminance_delta(y) for y in (0, 10, 20)) > 5
        and abs(luminance_delta(35)) < 8
        and max(luminance_delta(y) for y in (45, 50, 60)) > 5
    )


def validate_directory(directory: Path) -> list[str]:
    errors: list[str] = []
    paths = sorted(directory.glob("food_item_*.webp"))
    if not paths:
        return [f"No built-in food images found in {directory}"]
    for path in paths:
        try:
            with Image.open(path) as image:
                if image.size != EXPECTED_SIZE:
                    errors.append(f"{path.name}: expected {EXPECTED_SIZE}, found {image.size}")
                if looks_like_stacked_tile(image):
                    errors.append(f"{path.name}: appears to contain a displaced neighboring sprite tile")
        except OSError as exc:
            errors.append(f"{path.name}: cannot decode: {exc}")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "directory",
        nargs="?",
        type=Path,
        default=Path("app/src/main/res/drawable-nodpi"),
    )
    args = parser.parse_args()
    errors = validate_directory(args.directory)
    if errors:
        print("\n".join(errors))
        return 1
    print(f"Catalogue image validation passed: {args.directory}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
