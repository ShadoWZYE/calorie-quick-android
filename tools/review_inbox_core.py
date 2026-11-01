"""Private local storage and bundle ingestion for the Calorie Quick review inbox."""

from __future__ import annotations

import json
import os
import hashlib
import re
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

_VERSION_PATTERN = re.compile(r"^(\d+)\.(\d+)\.(\d+)(?:-(alpha|beta)(\d+))?", re.IGNORECASE)

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


def _version_order(value: str | None) -> tuple[int, int, int, int, int]:
    """Order stable and prerelease builds without depending on packaging libraries."""
    match = _VERSION_PATTERN.match(str(value or "").strip())
    if not match:
        return (-1, -1, -1, -1, -1)
    major, minor, patch = (int(match.group(index)) for index in range(1, 4))
    stage = {"alpha": 0, "beta": 1, None: 2}[match.group(4).lower() if match.group(4) else None]
    prerelease = int(match.group(5) or 0)
    return (major, minor, patch, stage, prerelease)


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
        "PRODUCT_DATA_ISSUE": "A tester explicitly marked this product as incorrect. Use the linked cached response as evidence, apply the reviewer’s categories and note, then correct or suppress the shared record without interrupting automatic ingestion of unflagged products.",
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
                changed = self._repair_jank_attribution(review) or changed
                changed = self._remove_stale_performance_findings(review) or changed
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
            self._repair_jank_attribution(existing)
            self._remove_stale_performance_findings(existing)
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

        products_by_code = {
            str(product.get("code")): product
            for product in report.get("openFoodFactsProducts", [])
            if isinstance(product, dict) and product.get("code")
        }
        for issue in report.get("productDataIssues", []):
            if not isinstance(issue, dict):
                continue
            issue_id = str(issue.get("id") or "unknown")
            barcode = str(issue.get("barcode") or "")
            product = products_by_code.get(barcode, {})
            reasons = ", ".join(str(value) for value in issue.get("reasonCodes", []))
            raw_paths = product.get("rawPaths", [])
            description = (
                f"barcode={barcode}; reasons={reasons or 'unspecified'}; "
                f"comment={issue.get('comment') or 'none'}; raw responses={', '.join(raw_paths) or 'not present'}"
            )
            items.append(
                _item(
                    item_id=f"product-issue:{issue_id}",
                    source_type="PRODUCT_DATA_ISSUE",
                    title=f"Incorrect product: {issue.get('productName') or product.get('name') or barcode}",
                    description=description,
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

        current_version = str((report.get("build") or {}).get("versionName") or "")
        has_current_performance = any(
            isinstance(operation, dict) and operation.get("appVersion") == current_version
            for operation in support.get("performance", [])
        )
        slow_operations = [
            operation for operation in support.get("performance", [])
            if isinstance(operation, dict)
            and (
                not current_version
                or operation.get("appVersion") == current_version
                or not operation.get("appVersion") and not has_current_performance
            )
            and (
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

        has_current_jank = any(
            isinstance(session, dict) and session.get("appVersion") == current_version
            for session in support.get("uiJank", [])
        )
        for session in support.get("uiJank", []):
            if not isinstance(session, dict):
                continue
            if current_version and (
                session.get("appVersion") and session.get("appVersion") != current_version
                or not session.get("appVersion") and has_current_jank
            ):
                continue
            jank_rate = float(session.get("jankRate") or 0)
            severe_frames = int(session.get("severeFrames") or 0)
            max_frame = int(session.get("maxFrameDurationMillis") or 0)
            if jank_rate < 0.05 and severe_frames < 2 and max_frame < 150:
                continue
            screens = [item for item in session.get("screens", []) if isinstance(item, dict)]
            # A tiny screen sample can report a 100% rate from a single transition frame and
            # falsely take ownership of a long session. Attribute the finding to the screen
            # responsible for the most accumulated jank, using severity and duration as
            # fallbacks for older bundles.
            worst = max(
                screens,
                key=lambda item: (
                    int(item.get("totalJankDurationMillis") or 0),
                    int(item.get("severeFrames") or 0),
                    int(item.get("maxFrameDurationMillis") or 0),
                    int(item.get("jankyFrames") or 0),
                ),
                default={},
            )
            session_id = str(session.get("id") or "unknown")
            description = (
                f"screen={worst.get('screen') or 'unknown'}; "
                f"janky frames={session.get('jankyFrames', 0)}/{session.get('totalFrames', 0)} "
                f"({jank_rate * 100:.1f}%); severe frames={severe_frames}; "
                f"maximum frame={max_frame} ms"
            )
            items.append(
                _item(
                    item_id=f"jank:{session_id}",
                    source_type="PERFORMANCE_DIAGNOSTIC",
                    title=f"UI stutter: {worst.get('screen') or 'unknown screen'}",
                    description=description,
                    classification="PERFORMANCE",
                    severity="MEDIUM" if severe_frames or max_frame >= 150 else "LOW",
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
        reconciliation = self._advance_backlog_for_new_bundle(review)
        review["summary"].update(reconciliation)
        self._reconcile_implementation_status(review)
        self.save_review(review)
        return review, True

    def _advance_backlog_for_new_bundle(self, new_review: dict[str, Any]) -> dict[str, int]:
        """Use a genuinely newer field package as confirmation and planning boundary.

        Implemented findings in TESTING are considered resolved when they do not recur in the
        next newer review package. Deliberately deferred items are moved to Next so "Later" is a
        temporary planning choice rather than a permanent hiding place.
        """
        new_version = str((new_review.get("build") or {}).get("versionName") or "")
        new_order = _version_order(new_version)
        if new_order[0] < 0:
            return {"autoResolvedTestingCount": 0, "rolledForwardCount": 0}
        current_ids = {str(item.get("id")) for item in new_review.get("items", [])}
        auto_resolved = 0
        try:
            registry = json.loads(self.implementation_status_path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            registry = {"schema": "calorie-quick-review-implementation-status", "schemaVersion": 1, "items": {}}
        entries = registry.setdefault("items", {})
        registry_changed = False
        for item_id, implementation in entries.items():
            if not isinstance(implementation, dict) or implementation.get("state") != "TESTING":
                continue
            if _version_order(implementation.get("reference")) >= new_order:
                continue
            if item_id in current_ids:
                implementation["lastRepeatedIn"] = new_version
                registry_changed = True
                continue
            implementation.update({
                "state": "DONE",
                "confirmedByVersion": new_version,
                "resolution": "Not reported again in the next newer review package.",
                "confirmedAt": _now(),
            })
            registry_changed = True
            auto_resolved += 1
        if registry_changed:
            temporary = self.implementation_status_path.with_suffix(".tmp")
            temporary.write_text(json.dumps(registry, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
            temporary.replace(self.implementation_status_path)

        rolled_forward = 0
        for path in self.bundle_root.glob("*/review.json"):
            if path.parent.name == new_review.get("bundleHash"):
                continue
            try:
                previous = json.loads(path.read_text(encoding="utf-8"))
            except (OSError, json.JSONDecodeError):
                continue
            previous_version = str((previous.get("build") or {}).get("versionName") or "")
            if _version_order(previous_version) >= new_order:
                continue
            changed = self._reconcile_implementation_status(previous)
            for item in previous.get("items", []):
                if item.get("decision") != "ACCEPT_BACKLOG":
                    continue
                item.update({
                    "decision": "ACCEPT_NEXT",
                    "status": "READY",
                    "promote": True,
                    "rolledForwardAt": _now(),
                    "rolledForwardByVersion": new_version,
                })
                changed = True
                rolled_forward += 1
            if changed:
                self.save_review(previous)
        return {"autoResolvedTestingCount": auto_resolved, "rolledForwardCount": rolled_forward}

    def shared_catalogue_count(self) -> int:
        try:
            catalogue = json.loads(self.shared_catalogue_path.read_text(encoding="utf-8"))
            return len(catalogue.get("foods", {})) + len(catalogue.get("recipes", {}))
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
                "schemaVersion": 2,
                "foods": {},
                "recipes": {},
            }
        records = shared.setdefault("foods", {})
        recipe_records = shared.setdefault("recipes", {})
        observed_at = _now()

        def identity(barcode: str, name: str, brand: str) -> str:
            restricted = len(barcode) >= 2 and barcode[:2].isdigit() and 20 <= int(barcode[:2]) <= 29
            if barcode and not restricted:
                return f"barcode:{barcode}"
            normalized = "|".join((name, brand)).lower().strip()
            return "identity:" + hashlib.sha256(normalized.encode("utf-8")).hexdigest()

        # Older inbox versions used every barcode as a global key. Re-key any
        # retained restricted-circulation record before adding new evidence.
        for old_key, record in list(records.items()):
            barcode = str(record.get("barcode") or "")
            restricted = len(barcode) >= 2 and barcode[:2].isdigit() and 20 <= int(barcode[:2]) <= 29
            if not restricted or not old_key.startswith("barcode:"):
                continue
            normalized_food = record.get("normalizedFood") if isinstance(record.get("normalizedFood"), dict) else {}
            names = normalized_food.get("name") if isinstance(normalized_food.get("name"), dict) else {}
            name = str(record.get("displayName") or names.get("en") or names.get("ro") or "")
            brand = str(normalized_food.get("brand") or "")
            new_key = identity("", name, brand)
            record["id"] = new_key
            records.pop(old_key)
            records.setdefault(new_key, record)

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

        for recipe in catalogue.get("recipes", []):
            if not isinstance(recipe, dict) or not recipe.get("selectedForThisExport", True):
                continue
            recipe_id = str(recipe.get("foodId") or "").strip()
            if not recipe_id:
                continue
            record = recipe_records.setdefault(recipe_id, {
                "id": recipe_id, "firstSeenAt": observed_at, "sources": [],
            })
            record.update(
                displayName=str(recipe.get("name") or recipe_id),
                normalizedRecipe=deepcopy(recipe),
                lastSeenAt=observed_at,
            )
            evidence = source("catalogue/catalogue.json", "OPT_IN_RECIPE")
            if not any(item.get("bundleHash") == bundle_hash and item.get("path") == evidence["path"] for item in record["sources"]):
                record["sources"].append(evidence)

        shared["updatedAt"] = observed_at
        shared["schemaVersion"] = 2
        shared["foodCount"] = len(records)
        shared["recipeCount"] = len(recipe_records)
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

    def _repair_jank_attribution(self, review: dict[str, Any]) -> bool:
        """Repair findings created before screen attribution used accumulated impact."""
        archive_path = Path(str(review.get("sourceArchive") or ""))
        if not archive_path.is_file():
            return False
        try:
            with zipfile.ZipFile(archive_path) as archive:
                support = self._read_json(archive, "support/support.json") or {}
        except (OSError, zipfile.BadZipFile):
            return False
        sessions = {
            str(session.get("id")): session
            for session in support.get("uiJank", [])
            if isinstance(session, dict) and session.get("id")
        }
        changed = False
        for item in review.get("items", []):
            item_id = str(item.get("id") or "")
            if not item_id.startswith("jank:"):
                continue
            session = sessions.get(item_id.removeprefix("jank:"))
            if not session:
                continue
            screens = [screen for screen in session.get("screens", []) if isinstance(screen, dict)]
            worst = max(
                screens,
                key=lambda screen: (
                    int(screen.get("totalJankDurationMillis") or 0),
                    int(screen.get("severeFrames") or 0),
                    int(screen.get("maxFrameDurationMillis") or 0),
                    int(screen.get("jankyFrames") or 0),
                ),
                default={},
            )
            screen_name = str(worst.get("screen") or "unknown")
            description = (
                f"screen={screen_name}; "
                f"janky frames={session.get('jankyFrames', 0)}/{session.get('totalFrames', 0)} "
                f"({float(session.get('jankRate') or 0) * 100:.1f}%); "
                f"severe frames={session.get('severeFrames', 0)}; "
                f"maximum frame={session.get('maxFrameDurationMillis', 0)} ms"
            )
            updates = {"title": f"UI stutter: {screen_name}", "description": description}
            for key, value in updates.items():
                if item.get(key) != value:
                    item[key] = value
                    changed = True
        return changed

    def _remove_stale_performance_findings(self, review: dict[str, Any]) -> bool:
        """Keep historical telemetry in the ZIP without presenting it as current-version work."""
        current_version = str((review.get("build") or {}).get("versionName") or "")
        archive_path = Path(str(review.get("sourceArchive") or ""))
        if not current_version or not archive_path.is_file():
            return False
        try:
            with zipfile.ZipFile(archive_path) as archive:
                support = self._read_json(archive, "support/support.json") or {}
        except (OSError, zipfile.BadZipFile):
            return False
        versions_by_id = {}
        performance_records = [
            record for record in support.get("performance", []) if isinstance(record, dict)
        ]
        jank_records = [record for record in support.get("uiJank", []) if isinstance(record, dict)]
        for record in [*performance_records, *jank_records]:
            if isinstance(record, dict) and record.get("id"):
                versions_by_id[str(record["id"])] = str(record.get("appVersion") or "")
        current_ids_exist = {
            "performance": any(record.get("appVersion") == current_version for record in performance_records),
            "jank": any(record.get("appVersion") == current_version for record in jank_records),
        }
        retained = []
        changed = False
        for item in review.get("items", []):
            item_id = str(item.get("id") or "")
            if item.get("sourceType") != "PERFORMANCE_DIAGNOSTIC":
                retained.append(item)
                continue
            raw_id = item_id.split(":", 1)[-1]
            recorded_version = versions_by_id.get(raw_id, "")
            kind = item_id.split(":", 1)[0]
            if recorded_version and recorded_version != current_version or (
                not recorded_version and current_ids_exist.get(kind, False)
            ):
                changed = True
                continue
            retained.append(item)
        if changed:
            review["items"] = retained
        return changed

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
