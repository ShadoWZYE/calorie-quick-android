"""Private local storage and bundle ingestion for the Calorie Quick review inbox."""

from __future__ import annotations

import json
import os
import hashlib
import shutil
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


def read_bundle_json(archive_path: Path, member: str) -> dict[str, Any] | None:
    try:
        with zipfile.ZipFile(archive_path) as archive:
            value = json.loads(archive.read(member))
            return value if isinstance(value, dict) else None
    except (OSError, KeyError, zipfile.BadZipFile, UnicodeDecodeError, json.JSONDecodeError):
        return None


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
        "OPEN_FOOD_FACTS_CACHE": "The complete API response was retained as temporary evidence. Compare the package fields, normalize names, category, nutrition, allergens, and practical measures, then explicitly approve it before promoting anything into a built-in or shared catalogue.",
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
    def __init__(self, root: Path | None = None, implementation_status_path: Path | None = None):
        self.root = (root or default_inbox_root()).resolve()
        self.bundle_root = self.root / "bundles"
        self.shared_catalogue_path = self.root / "shared-food-catalogue.json"
        self.implementation_status_path = implementation_status_path or Path(__file__).with_name(
            "review_implementation_status.json",
        )
        self.bundle_root.mkdir(parents=True, exist_ok=True)

    def list_reviews(self) -> list[dict[str, Any]]:
        reviews = []
        for path in self.bundle_root.glob("*/review.json"):
            try:
                review = json.loads(path.read_text(encoding="utf-8"))
                changed = self._ensure_analysis_fields(review)
                changed = self._reconcile_implementation_status(review) or changed
                if changed:
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
        retained_archive = review_dir / "source.zip"
        if archive_path != retained_archive.resolve():
            shutil.copy2(archive_path, retained_archive)
        shared_count = self._merge_shared_catalogue(
            catalogue=read_bundle_json(archive_path, "catalogue/catalogue.json") or {},
            collected=report.get("openFoodFactsProducts", []),
            bundle_hash=digest,
            retained_archive=retained_archive,
        )
        if review_path.exists():
            existing = json.loads(review_path.read_text(encoding="utf-8"))
            self._ensure_analysis_fields(existing)
            self._reconcile_implementation_status(existing)
            existing["sourceArchive"] = str(retained_archive)
            existing["sourceArchiveOriginal"] = str(archive_path)
            existing.setdefault("summary", {})["sharedCatalogueCount"] = shared_count
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
            "sourceArchive": str(retained_archive),
            "sourceArchiveOriginal": str(archive_path),
            "importedAt": _now(),
            "lastSeenAt": _now(),
            "bundleFormat": report.get("format"),
            "build": report.get("build"),
            "summary": report.get("summary"),
            "validationWarnings": report.get("warnings", []),
            "items": items,
        }
        review["summary"]["sharedCatalogueCount"] = shared_count
        self._reconcile_implementation_status(review)
        self.save_review(review)
        return review, True

    def shared_catalogue_count(self) -> int:
        try:
            catalogue = json.loads(self.shared_catalogue_path.read_text(encoding="utf-8"))
            return len(catalogue.get("foods", {}))
        except (OSError, json.JSONDecodeError):
            return 0

    def _merge_shared_catalogue(
        self,
        catalogue: dict[str, Any],
        collected: list[dict[str, Any]],
        bundle_hash: str,
        retained_archive: Path,
    ) -> int:
        try:
            shared = json.loads(self.shared_catalogue_path.read_text(encoding="utf-8"))
            if shared.get("schema") != "calorie-quick-local-shared-food-catalogue":
                raise ValueError("unsupported shared catalogue")
        except (OSError, json.JSONDecodeError, ValueError):
            shared = {
                "schema": "calorie-quick-local-shared-food-catalogue",
                "schemaVersion": 1,
                "foods": {},
            }
        records = shared.setdefault("foods", {})
        observed_at = _now()

        def identity(barcode: str, name: str, brand: str) -> str:
            if barcode:
                return f"barcode:{barcode}"
            normalized = "|".join((name, brand)).lower().strip()
            return "identity:" + hashlib.sha256(normalized.encode("utf-8")).hexdigest()

        def source(path: str, source_type: str) -> dict[str, Any]:
            return {
                "bundleHash": bundle_hash,
                "archive": str(retained_archive),
                "path": path,
                "sourceType": source_type,
                "observedAt": observed_at,
            }

        for food in catalogue.get("foods", []):
            if not isinstance(food, dict) or not food.get("selectedForThisExport", True):
                continue
            names = food.get("name") if isinstance(food.get("name"), dict) else {}
            name = str(names.get("en") or names.get("ro") or food.get("id") or "")
            barcode = str(food.get("barcode") or "")
            key = identity(barcode, name, str(food.get("brand") or ""))
            record = records.setdefault(key, {
                "id": key, "firstSeenAt": observed_at, "sources": [],
            })
            record.update(
                barcode=barcode or None,
                displayName=name,
                normalizedFood=deepcopy(food),
                lastSeenAt=observed_at,
            )
            evidence = source("catalogue/catalogue.json", str(food.get("sourceType") or "SHARED"))
            if not any(item.get("bundleHash") == bundle_hash and item.get("path") == evidence["path"] for item in record["sources"]):
                record["sources"].append(evidence)

        for product in collected:
            code = str(product.get("code") or "")
            if not code:
                continue
            key = identity(code, str(product.get("name") or ""), str(product.get("brand") or ""))
            record = records.setdefault(key, {
                "id": key, "firstSeenAt": observed_at, "sources": [],
            })
            record.update(
                barcode=code,
                displayName=product.get("name") or record.get("displayName") or code,
                openFoodFactsProduct=deepcopy(product.get("product") or {}),
                lastSeenAt=observed_at,
            )
            evidence = source(str(product.get("rawPath") or ""), "OPEN_FOOD_FACTS")
            if not any(item.get("bundleHash") == bundle_hash and item.get("path") == evidence["path"] for item in record["sources"]):
                record["sources"].append(evidence)

        shared["updatedAt"] = observed_at
        shared["foodCount"] = len(records)
        temporary = self.shared_catalogue_path.with_suffix(".tmp")
        temporary.write_text(json.dumps(shared, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        temporary.replace(self.shared_catalogue_path)
        return len(records)

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

    def _reconcile_implementation_status(self, review: dict[str, Any]) -> bool:
        try:
            registry = json.loads(self.implementation_status_path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            return False
        entries = registry.get("items", {})
        changed = False
        for item in review.get("items", []):
            implementation = entries.get(item.get("id"))
            if not isinstance(implementation, dict):
                continue
            state = implementation.get("state")
            if state == "TESTING" and item.get("decision") != "RESOLVED":
                updates = {
                    "decision": "NEEDS_REPRODUCTION",
                    "status": "NEEDS_INFO",
                    "promote": False,
                }
            elif state == "DONE":
                updates = {
                    "decision": "RESOLVED",
                    "status": "RESOLVED",
                    "promote": False,
                }
            else:
                updates = {}
            updates["implementationState"] = state
            reference = implementation.get("reference")
            if reference:
                updates["linkedReference"] = reference
            item_changed = False
            for key, value in updates.items():
                if item.get(key) != value:
                    item[key] = value
                    changed = True
                    item_changed = True
            if item_changed:
                item["implementationStatusUpdatedAt"] = _now()
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
