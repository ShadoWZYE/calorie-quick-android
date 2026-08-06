"""Private local storage and bundle ingestion for the Calorie Quick review inbox."""

from __future__ import annotations

import json
import os
import zipfile
from copy import deepcopy
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

from review_bundle_parser import inspect_bundle

INBOX_SCHEMA = "calorie-quick-local-review-inbox"
INBOX_SCHEMA_VERSION = 1
BRIEF_SCHEMA = "calorie-quick-implementation-brief"

CLASSIFICATIONS = ["BUG", "UI_UX", "PERFORMANCE", "DATA", "OCR", "FEATURE", "QUESTION"]
SEVERITIES = ["CRITICAL", "HIGH", "MEDIUM", "LOW"]
STATUSES = ["NEW", "TRIAGED", "NEEDS_INFO", "READY", "IN_PROGRESS", "RESOLVED", "CLOSED"]
DECISIONS = [
    "UNDECIDED",
    "ACCEPT_NEXT",
    "ACCEPT_BACKLOG",
    "NEEDS_REPRODUCTION",
    "NEEDS_PRODUCT_DECISION",
    "DEFERRED",
    "DECLINED",
    "DUPLICATE",
    "RESOLVED",
]


def default_inbox_root() -> Path:
    base = Path(os.environ.get("LOCALAPPDATA", Path.home() / "AppData" / "Local"))
    return base / "CalorieQuick" / "ReviewInbox"


def _now() -> str:
    return datetime.now(timezone.utc).isoformat()


def _title(text: str, fallback: str) -> str:
    normalized = " ".join(text.split())
    return normalized[:88] + ("…" if len(normalized) > 88 else "") if normalized else fallback


def suggest_classification(text: str) -> str:
    lowered = text.lower()
    if any(word in lowered for word in ("freeze", "lag", "stutter", "slow", "until results load")):
        return "PERFORMANCE"
    if any(word in lowered for word in ("ocr", "decode", "scan", "label")):
        return "OCR"
    if any(word in lowered for word in ("barcode", "nutrition", "fibre", "fiber", "category", "measure")):
        return "DATA"
    if any(word in lowered for word in ("closes", "minimises", "jump", "fails", "wrong", "broken", "overflow")):
        return "BUG"
    if text.rstrip().endswith("?"):
        return "QUESTION"
    return "FEATURE"


def suggest_severity(text: str, classification: str) -> str:
    lowered = text.lower()
    if any(word in lowered for word in ("data loss", "crash", "cannot open", "corrupt")):
        return "HIGH"
    if classification in {"BUG", "PERFORMANCE", "OCR", "DATA"}:
        return "MEDIUM"
    return "LOW"


def _item(
    item_id: str,
    source_type: str,
    title: str,
    description: str,
    attachments: list[str] | None = None,
    classification: str | None = None,
    severity: str | None = None,
) -> dict[str, Any]:
    suggested_class = classification or suggest_classification(description)
    suggested_severity = severity or suggest_severity(description, suggested_class)
    assistant_analysis = {
        "CATALOGUE_DATA": "The catalogue validator found a concrete data-quality issue. Confirm corrections against the package label or an authoritative source before changing shared catalogue data.",
        "SCAN_DIAGNOSTIC": "The scan was retained because OCR or image decoding did not produce a dependable prefill. Inspect the sanitized attachment for layout, language, glare, curvature, and unit patterns that need regression coverage.",
        "PERFORMANCE_DIAGNOSTIC": "This operation crossed the local slow/failure threshold. Use its duration and outcome as evidence, then reproduce with finer instrumentation before selecting an architectural fix.",
    }.get(
        source_type,
        f"The item was imported intact and classified as {suggested_class}. The suggestion is not a product decision; confirm reproduction, desired behavior, scope, and acceptance checks before promotion.",
    )
    return {
        "id": item_id,
        "sourceType": source_type,
        "title": title,
        "description": description,
        "attachments": attachments or [],
        "suggestedClassification": suggested_class,
        "suggestedSeverity": suggested_severity,
        "classification": suggested_class,
        "severity": suggested_severity,
        "status": "NEW",
        "decision": "UNDECIDED",
        "rationale": "",
        "targetVersion": "",
        "linkedReference": "",
        "assistantAnalysis": assistant_analysis,
        "analysisUpdatedAt": _now(),
        "reviewerComments": "",
        "corrections": "",
        "implementationNotes": "",
        "promote": False,
        "createdAt": _now(),
        "updatedAt": _now(),
    }


class ReviewInboxStore:
    def __init__(self, root: Path | None = None):
        self.root = (root or default_inbox_root()).resolve()
        self.bundle_root = self.root / "bundles"
        self.bundle_root.mkdir(parents=True, exist_ok=True)

    def list_reviews(self) -> list[dict[str, Any]]:
        reviews = []
        for path in self.bundle_root.glob("*/review.json"):
            try:
                review = json.loads(path.read_text(encoding="utf-8"))
                if self._ensure_analysis_fields(review):
                    self.save_review(review)
                reviews.append(review)
            except (OSError, json.JSONDecodeError):
                continue
        return sorted(reviews, key=lambda item: item.get("importedAt", ""), reverse=True)

    def import_bundle(self, archive_path: Path) -> tuple[dict[str, Any], bool]:
        archive_path = archive_path.resolve()
        report = inspect_bundle(archive_path)
        if not report.get("valid"):
            raise ValueError("Bundle validation failed:\n" + "\n".join(report.get("errors", [])))
        digest = report["archiveSha256"]
        review_dir = self.bundle_root / digest
        review_path = review_dir / "review.json"
        review_dir.mkdir(parents=True, exist_ok=True)
        if review_path.exists():
            existing = json.loads(review_path.read_text(encoding="utf-8"))
            self._ensure_analysis_fields(existing)
            existing["sourceArchive"] = str(archive_path)
            existing["lastSeenAt"] = _now()
            self.save_review(existing)
            return existing, False

        with zipfile.ZipFile(archive_path) as archive:
            support = self._read_json(archive, "support/support.json") or {}
            catalogue = self._read_json(archive, "catalogue/catalogue.json") or {}
            extracted = self._extract_media(archive, review_dir / "media", report)

        items: list[dict[str, Any]] = []
        for message in support.get("feedbackThread", []):
            if not isinstance(message, dict):
                continue
            text = str(message.get("text", "")).strip()
            message_id = str(message.get("id", "unknown"))
            attachment = extracted.get(message.get("imageBundlePath"))
            items.append(
                _item(
                    item_id=f"feedback:{message_id}",
                    source_type="FEEDBACK",
                    title=_title(text, "Feedback"),
                    description=text,
                    attachments=[attachment] if attachment else [],
                ),
            )

        food_by_id = {
            str(food.get("id")): food
            for food in catalogue.get("foods", [])
            if isinstance(food, dict) and food.get("id")
        }
        for finding in report.get("foodFindings", []):
            food_id = str(finding.get("id", "unknown"))
            food = food_by_id.get(food_id, {})
            image = food.get("image") if isinstance(food.get("image"), dict) else {}
            attachment = extracted.get(image.get("bundlePath"))
            description = "; ".join(finding.get("issues", []))
            items.append(
                _item(
                    item_id=f"data:{food_id}",
                    source_type="CATALOGUE_DATA",
                    title=f"Review data: {finding.get('name') or food_id}",
                    description=description,
                    attachments=[attachment] if attachment else [],
                    classification="DATA",
                    severity="MEDIUM",
                ),
            )

        for diagnostic in support.get("diagnostics", []):
            if not isinstance(diagnostic, dict):
                continue
            diagnostic_id = str(diagnostic.get("id", "unknown"))
            description = f"{diagnostic.get('kind', 'scan')}: {diagnostic.get('failure', 'unknown failure')}"
            attachment = extracted.get(diagnostic.get("bundlePath"))
            items.append(
                _item(
                    item_id=f"diagnostic:{diagnostic_id}",
                    source_type="SCAN_DIAGNOSTIC",
                    title=f"Failed {diagnostic.get('kind', 'scan')}",
                    description=description,
                    attachments=[attachment] if attachment else [],
                    classification="OCR",
                    severity="MEDIUM",
                ),
            )

        slow_operations = [
            operation for operation in support.get("performance", [])
            if isinstance(operation, dict) and (
                operation.get("durationMillis", 0) >= 1_500 or operation.get("outcome") != "success"
            )
        ]
        for operation in slow_operations:
            operation_id = str(operation.get("id", "unknown"))
            description = (
                f"{operation.get('operation')} took {operation.get('durationMillis')} ms; "
                f"outcome={operation.get('outcome')}; results={operation.get('resultCount')}"
            )
            items.append(
                _item(
                    item_id=f"performance:{operation_id}",
                    source_type="PERFORMANCE_DIAGNOSTIC",
                    title=f"Slow operation: {operation.get('operation')}",
                    description=description,
                    classification="PERFORMANCE",
                    severity="MEDIUM",
                ),
            )

        review = {
            "schema": INBOX_SCHEMA,
            "schemaVersion": INBOX_SCHEMA_VERSION,
            "bundleHash": digest,
            "sourceArchive": str(archive_path),
            "importedAt": _now(),
            "lastSeenAt": _now(),
            "bundleFormat": report.get("format"),
            "build": report.get("build"),
            "summary": report.get("summary"),
            "validationWarnings": report.get("warnings", []),
            "items": items,
        }
        self.save_review(review)
        return review, True

    def save_review(self, review: dict[str, Any]) -> None:
        digest = review["bundleHash"]
        review_dir = self.bundle_root / digest
        review_dir.mkdir(parents=True, exist_ok=True)
        review["updatedAt"] = _now()
        path = review_dir / "review.json"
        temporary = path.with_suffix(".tmp")
        temporary.write_text(json.dumps(review, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        temporary.replace(path)

    @staticmethod
    def _ensure_analysis_fields(review: dict[str, Any]) -> bool:
        changed = False
        for item in review.get("items", []):
            if "assistantAnalysis" not in item:
                classification = item.get("classification", "FEATURE")
                item["assistantAnalysis"] = (
                    f"The item was imported intact and classified as {classification}. "
                    "Confirm the evidence, scope, and expected behavior before promotion."
                )
                item["analysisUpdatedAt"] = _now()
                changed = True
        return changed

    def add_manual_item(self, review: dict[str, Any]) -> dict[str, Any]:
        existing = {item.get("id") for item in review.get("items", [])}
        index = 1
        while f"manual:{index}" in existing:
            index += 1
        item = _item(
            item_id=f"manual:{index}",
            source_type="MANUAL",
            title="New review item",
            description="",
            classification="FEATURE",
            severity="LOW",
        )
        review.setdefault("items", []).append(item)
        self.save_review(review)
        return item

    def export_implementation_brief(self, reviews: list[dict[str, Any]], json_path: Path) -> dict[str, Any]:
        next_items = []
        for review in reviews:
            for item in review.get("items", []):
                if item.get("decision") == "ACCEPT_NEXT":
                    next_items.append({
                        "bundleHash": review.get("bundleHash"),
                        "build": review.get("build"),
                        **deepcopy(item),
                    })
        if not next_items:
            raise ValueError("No review items are marked Next.")
        brief = {
            "schema": BRIEF_SCHEMA,
            "schemaVersion": 1,
            "generatedAt": _now(),
            "itemCount": len(next_items),
            "items": next_items,
        }
        json_path = json_path.with_suffix(".json")
        json_path.write_text(json.dumps(brief, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        json_path.with_suffix(".md").write_text(self._brief_markdown(brief), encoding="utf-8")
        return brief

    @staticmethod
    def _read_json(archive: zipfile.ZipFile, path: str) -> dict[str, Any] | None:
        try:
            value = json.loads(archive.read(path))
            return value if isinstance(value, dict) else None
        except (KeyError, UnicodeDecodeError, json.JSONDecodeError):
            return None

    @staticmethod
    def _extract_media(
        archive: zipfile.ZipFile,
        media_dir: Path,
        report: dict[str, Any],
    ) -> dict[str, str]:
        media_dir.mkdir(parents=True, exist_ok=True)
        extracted: dict[str, str] = {}
        image_paths = {entry["path"] for entry in report.get("entries", []) if entry.get("mimeType", "").startswith("image/")}
        for index, archive_path in enumerate(sorted(image_paths)):
            suffix = Path(archive_path).suffix.lower()
            if suffix not in {".jpg", ".jpeg", ".png"}:
                suffix = ".img"
            destination = media_dir / f"{index:03d}-{Path(archive_path).stem[:80]}{suffix}"
            destination.write_bytes(archive.read(archive_path))
            extracted[archive_path] = str(destination.resolve())
        return extracted

    @staticmethod
    def _brief_markdown(brief: dict[str, Any]) -> str:
        lines = [
            "# Calorie Quick implementation brief",
            "",
            f"Generated: {brief['generatedAt']}",
            f"Promoted items: {brief['itemCount']}",
            "",
        ]
        for index, item in enumerate(brief["items"], 1):
            lines.extend([
                f"## {index}. {item.get('title', 'Untitled')}",
                "",
                f"- Classification: {item.get('classification')}",
                f"- Severity: {item.get('severity')}",
                f"- Decision: {item.get('decision')}",
                f"- Target version: {item.get('targetVersion') or 'Unassigned'}",
                f"- Linked reference: {item.get('linkedReference') or 'None'}",
                f"- Source item: `{item.get('id')}`",
                f"- Source bundle: `{item.get('bundleHash')}`",
                "",
                "### Original evidence",
                "",
                item.get("description") or "No description supplied.",
            ])
            for heading, key in (
                ("Codex analysis", "assistantAnalysis"),
                ("Decision rationale", "rationale"),
                ("Reviewer comments", "reviewerComments"),
                ("Corrections", "corrections"),
                ("Implementation notes", "implementationNotes"),
            ):
                if item.get(key):
                    lines.extend(["", f"### {heading}", "", item[key]])
            lines.append("")
        return "\n".join(lines)
