# Roadmap

Milestones are outcome gates rather than promises by date.

## M0 — Product and technical spike (complete)

- Research and architectural decisions.
- Runnable bilingual Today/quick-add vertical slice.
- Build/test toolchain, repository hygiene, and baseline documentation.
- Validate the five-second frequent-food flow with a handful of users before expanding scope.

Exit: the prototype builds, the happy path is understandable without instruction, and product/package naming decisions are captured.

## M1 — Local-first private alpha (current)

- Room schemas and non-destructive v1→v2→v3→v4→v5 migrations, seeded local foods, normalized nutrient/allergen rows, diary/unit snapshots, interaction-ranked quantity presets, and persistence are implemented; meals and diary edit/undo remain.
- First-run onboarding, profile editing, BMR/TDEE estimate, activity/goal choice, linked manual calorie/macro targets, fibre target, and app-language settings are implemented; display units remain.
- Recents/frequency ranking, favourite servings, copy meal/day, templates.
- Extensible nutrient schema and fibre tracking with clear unknown-data states are implemented; optional total/added sugar and sodium with manual targets remain.
- Normalized generic/branded product schema with mass/volume/piece dimensions, packages/barcodes, full nutrient panels, provenance, and immutable diary snapshots.
- Offline personal-food create/edit/archive with EU-14 allergen declarations, custom gram-converted measures, and prepackaged-item split presets is implemented; preparation variants with most-used defaults and optional private store/price history remain.
- Opt-in catalogue submissions with immutable review snapshots; no automatic promotion of personal records.
- EU-14 allergen/intolerance exclusions with `contains`, `may contain`, and `unknown` confidence; recommendation filtering and warnings.
- Settings, export/delete, reminders, English/Romanian QA, accessibility.
- Unit/UI/migration tests and measured performance budgets.

Exit: a user can depend on it offline for four weeks without data loss.

## M2 — Catalogue and barcode beta

- Versioned normalization service and cache.
- Open Food Facts adapter, followed by USDA generic-food enrichment where useful.
- Barcode scanner, missing-product flow, provenance/confidence UI, image thumbnails.
- Moderation queue and validation rules for opted-in personal-food submissions.
- Retry/offline queue, abuse controls, provider attribution/licensing audit.

Exit: Romanian/English barcode lookup has measured coverage and all imported fields remain correctable.

## M3 — Optional account and ecosystem

- Optional authentication, encrypted sync, conflict resolution, device migration.
- Health Connect nutrition/body-measurement integration with granular consent.
- Widget/app shortcuts and wearable-friendly entry surfaces.
- Server observability, backups, deletion workflow, privacy/security review.

Exit: sync is idempotent, tested under offline/conflict conditions, and local-only use remains supported.

## M4 — OCR, recipes, and intelligence

- On-device label/ingredient OCR with confirmation and parsing confidence.
- Recipe builder, pantry/frequent-food recommendations, meal suggestions constrained by remaining macros and preferences.
- Nutrient flags and trend insights with careful non-medical wording.
- Clinically reviewed condition-oriented tracking helpers that tune only from explicit targets and never substitute for care advice.

Exit: recommendations are explainable and never invent unavailable ingredients or nutrition facts.

## M5 — Experimental visual estimation

- Whole-dish segmentation/recognition research, portion reference workflow, uncertainty ranges, opt-in image processing.
- Controlled evaluation against weighed meals before any public accuracy claim.

Exit: only ship if it is demonstrably faster than manual entry and error bounds are honest enough to be useful.
