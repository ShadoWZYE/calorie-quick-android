#!/usr/bin/env python3
"""Validate Calorie Quick review ZIPs and emit privacy-conscious local reports.

This tool reads archive members in memory. It never imports data into the app and
never extracts attached media to disk.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import struct
import sys
import zipfile
from pathlib import Path, PurePosixPath
from typing import Any

MAX_ENTRIES = 2_000
MAX_ENTRY_BYTES = 32 * 1024 * 1024
MAX_TOTAL_BYTES = 256 * 1024 * 1024


def _safe_member(name: str) -> bool:
    path = PurePosixPath(name)
    return bool(name) and "\\" not in name and not path.is_absolute() and ".." not in path.parts


def _image_info(data: bytes) -> dict[str, Any]:
    result: dict[str, Any] = {
        "hasExif": b"Exif\x00\x00" in data,
        "hasXmp": b"http://ns.adobe.com/xap/1.0/" in data or b"<x:xmpmeta" in data,
    }
    if data.startswith(b"\x89PNG\r\n\x1a\n") and len(data) >= 24:
        result.update(mimeType="image/png", width=struct.unpack(">I", data[16:20])[0], height=struct.unpack(">I", data[20:24])[0])
        return result
    if data.startswith(b"\xff\xd8"):
        result["mimeType"] = "image/jpeg"
        offset = 2
        while offset + 9 < len(data):
            if data[offset] != 0xFF:
                offset += 1
                continue
            marker = data[offset + 1]
            offset += 2
            if marker in (0xD8, 0xD9) or 0xD0 <= marker <= 0xD7:
                continue
            if offset + 2 > len(data):
                break
            length = struct.unpack(">H", data[offset:offset + 2])[0]
            if marker in range(0xC0, 0xC4) and offset + 7 < len(data):
                result.update(height=struct.unpack(">H", data[offset + 3:offset + 5])[0], width=struct.unpack(">H", data[offset + 5:offset + 7])[0])
                break
            if length < 2:
                break
            offset += length
    return result


def _valid_ean(value: str) -> bool:
    if not value.isdigit() or len(value) not in (8, 12, 13, 14):
        return False
    digits = [int(char) for char in value]
    payload = digits[:-1]
    total = sum(digit * (3 if (len(payload) - index) % 2 else 1) for index, digit in enumerate(payload))
    return (10 - total % 10) % 10 == digits[-1]


def inspect_bundle(archive_path: Path) -> dict[str, Any]:
    warnings: list[str] = []
    errors: list[str] = []
    entries: dict[str, bytes] = {}
    entry_summary: list[dict[str, Any]] = []
    try:
        with zipfile.ZipFile(archive_path) as archive:
            infos = archive.infolist()
            if len(infos) > MAX_ENTRIES:
                errors.append(f"Archive has {len(infos)} entries; limit is {MAX_ENTRIES}.")
                infos = infos[:MAX_ENTRIES]
            total = 0
            seen: set[str] = set()
            for info in infos:
                if info.is_dir():
                    continue
                if not _safe_member(info.filename):
                    errors.append(f"Unsafe archive path: {info.filename!r}")
                    continue
                if info.filename in seen:
                    errors.append(f"Duplicate archive path: {info.filename}")
                    continue
                seen.add(info.filename)
                total += info.file_size
                if info.file_size > MAX_ENTRY_BYTES:
                    errors.append(f"Entry exceeds {MAX_ENTRY_BYTES} bytes: {info.filename}")
                    continue
                if total > MAX_TOTAL_BYTES:
                    errors.append(f"Archive expands beyond {MAX_TOTAL_BYTES} bytes.")
                    break
                data = archive.read(info)
                entries[info.filename] = data
                summary: dict[str, Any] = {
                    "path": info.filename,
                    "byteLength": len(data),
                    "sha256": hashlib.sha256(data).hexdigest(),
                }
                if data.startswith((b"\xff\xd8", b"\x89PNG\r\n\x1a\n")):
                    summary.update(_image_info(data))
                entry_summary.append(summary)
    except (OSError, zipfile.BadZipFile) as exc:
        return {"valid": False, "errors": [f"Cannot read ZIP: {exc}"], "warnings": [], "archive": str(archive_path)}

    def read_json(name: str) -> dict[str, Any] | None:
        data = entries.get(name)
        if data is None:
            return None
        try:
            value = json.loads(data)
            if not isinstance(value, dict):
                raise ValueError("root is not an object")
            return value
        except (UnicodeDecodeError, json.JSONDecodeError, ValueError) as exc:
            errors.append(f"Invalid JSON in {name}: {exc}")
            return None

    root = read_json("bundle.json")
    catalogue = read_json("catalogue/catalogue.json")
    support = read_json("support/support.json")
    bundle_format = "v2" if root else "legacy"
    if root:
        if root.get("schema") != "calorie-quick-review-bundle" or root.get("schemaVersion") != 2:
            errors.append("Unsupported root review-bundle schema.")
        declared_media = root.get("media", [])
        if not isinstance(declared_media, list):
            errors.append("bundle.json media must be an array.")
            declared_media = []
        for item in declared_media:
            if not isinstance(item, dict) or not isinstance(item.get("path"), str):
                errors.append("Malformed media declaration in bundle.json.")
                continue
            path = item["path"]
            data = entries.get(path)
            if data is None:
                errors.append(f"Declared media is missing: {path}")
                continue
            actual_hash = hashlib.sha256(data).hexdigest()
            if item.get("sha256") != actual_hash:
                errors.append(f"SHA-256 mismatch: {path}")
            if item.get("byteLength") != len(data):
                errors.append(f"Byte-length mismatch: {path}")
            image = _image_info(data)
            if image.get("hasExif") or image.get("hasXmp"):
                errors.append(f"Sanitized v2 media still contains metadata: {path}")
            if item.get("width") != image.get("width") or item.get("height") != image.get("height"):
                errors.append(f"Image dimensions mismatch: {path}")
        declared_paths = {item.get("path") for item in declared_media if isinstance(item, dict)}
        unlisted = [item["path"] for item in entry_summary if "mimeType" in item and item["path"] not in declared_paths]
        if unlisted:
            errors.append("Images missing from root media inventory: " + ", ".join(unlisted))
    else:
        warnings.append("Legacy bundle has no root manifest, media hashes, or exact build identity.")

    if catalogue is None:
        errors.append("Missing catalogue/catalogue.json.")
        foods: list[dict[str, Any]] = []
        recipes: list[dict[str, Any]] = []
    else:
        foods = [item for item in catalogue.get("foods", []) if isinstance(item, dict)]
        recipes = [item for item in catalogue.get("recipes", []) if isinstance(item, dict)]
        if catalogue.get("schema") != "calorie-quick-personal-catalogue" or catalogue.get("schemaVersion") not in (1, 2):
            errors.append("Unsupported catalogue schema.")

    food_findings: list[dict[str, Any]] = []
    for food in foods:
        name = food.get("name", {}).get("en") if isinstance(food.get("name"), dict) else None
        issues: list[str] = []
        nutrition = food.get("nutritionPer100g", {})
        try:
            calories = float(nutrition.get("calories"))
            expected = 4 * float(nutrition.get("proteinGrams", 0)) + 4 * float(nutrition.get("carbsGrams", 0)) + 9 * float(nutrition.get("fatGrams", 0))
            if abs(calories - expected) > max(20, calories * 0.15):
                issues.append(f"macro energy estimate is {expected:.0f} kcal vs {calories:.0f} declared")
        except (TypeError, ValueError):
            issues.append("nutrition values are incomplete")
        barcode = food.get("barcode")
        if barcode:
            barcode = str(barcode)
            if not _valid_ean(barcode):
                issues.append("barcode check digit or length is invalid")
            if len(barcode) >= 2 and barcode[:2].isdigit() and 20 <= int(barcode[:2]) <= 29:
                issues.append("barcode uses GS1 restricted-circulation prefix 20–29; do not treat as global identity")
        if not food.get("category"):
            issues.append("category is missing")
        if not food.get("measures"):
            issues.append("known measures are missing")
        if nutrition.get("fiberGrams") == 0:
            issues.append("fiber is recorded as zero; verify it is known rather than unknown")
        if issues:
            food_findings.append({"id": food.get("id"), "name": name, "issues": issues})

    images_with_metadata = [
        item["path"] for item in entry_summary if item.get("hasExif") or item.get("hasXmp")
    ]
    if images_with_metadata and not root:
        warnings.append(f"{len(images_with_metadata)} legacy image(s) contain EXIF/XMP metadata.")
    feedback_count = 0
    performance_count = 0
    if support:
        if support.get("schema") != "calorie-quick-support-bundle" or support.get("schemaVersion") not in (5, 6, 7):
            errors.append("Unsupported support schema.")
        thread = support.get("feedbackThread", [])
        feedback_count = len(thread) if isinstance(thread, list) else 0
        performance = support.get("performance", [])
        performance_count = len(performance) if isinstance(performance, list) else 0

    return {
        "valid": not errors,
        "archive": str(archive_path.resolve()),
        "archiveSha256": hashlib.sha256(archive_path.read_bytes()).hexdigest(),
        "format": bundle_format,
        "errors": errors,
        "warnings": warnings,
        "summary": {
            "entryCount": len(entry_summary),
            "foodCount": len(foods),
            "recipeCount": len(recipes),
            "feedbackMessageCount": feedback_count,
            "performanceOperationCount": performance_count,
            "imageCount": sum("mimeType" in item for item in entry_summary),
            "imagesWithMetadata": len(images_with_metadata),
        },
        "build": (root or {}).get("app") or ({
            "versionName": support.get("appVersion"),
            "versionCode": support.get("appVersionCode"),
            "buildId": support.get("buildId"),
        } if support else None),
        "foodFindings": food_findings,
        "mediaWithMetadata": images_with_metadata,
        "entries": entry_summary,
    }


def markdown_report(report: dict[str, Any]) -> str:
    summary = report.get("summary", {})
    lines = [
        "# Calorie Quick review-bundle report",
        "",
        f"- Result: {'PASS' if report.get('valid') else 'FAIL'}",
        f"- Format: {report.get('format', 'unknown')}",
        f"- Archive SHA-256: `{report.get('archiveSha256', 'unavailable')}`",
        f"- Contents: {summary.get('foodCount', 0)} foods, {summary.get('recipeCount', 0)} recipes, {summary.get('feedbackMessageCount', 0)} feedback messages, {summary.get('imageCount', 0)} images",
        f"- Images containing EXIF/XMP: {summary.get('imagesWithMetadata', 0)}",
    ]
    for heading, key in (("Errors", "errors"), ("Warnings", "warnings")):
        values = report.get(key, [])
        if values:
            lines.extend(["", f"## {heading}", ""] + [f"- {value}" for value in values])
    findings = report.get("foodFindings", [])
    if findings:
        lines.extend(["", "## Catalogue findings", ""])
        for finding in findings:
            lines.append(f"- {finding.get('name') or finding.get('id') or 'Unnamed food'}: " + "; ".join(finding["issues"]))
    lines.extend(["", "The report intentionally omits feedback text and does not extract image contents.", ""])
    return "\n".join(lines)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("archive", type=Path)
    parser.add_argument("--output-dir", type=Path, help="Directory for JSON and Markdown reports")
    args = parser.parse_args(argv)
    report = inspect_bundle(args.archive)
    output_dir = args.output_dir or args.archive.parent
    output_dir.mkdir(parents=True, exist_ok=True)
    stem = args.archive.stem + "-report"
    (output_dir / f"{stem}.json").write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    (output_dir / f"{stem}.md").write_text(markdown_report(report), encoding="utf-8")
    print(markdown_report(report))
    return 0 if report.get("valid") else 1


if __name__ == "__main__":
    sys.exit(main())
