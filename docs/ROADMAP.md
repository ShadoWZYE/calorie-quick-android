# Roadmap

Milestones are outcome gates rather than promises by date.

## M0 — Product and technical spike (complete)

- Runnable English/Romanian Android app and local build/test toolchain.
- Five-second repeated-food flow, responsive quick-add sheet, and persistent Room foundation.

## M1 — Local-first private alpha (field-test candidate)

Implemented:

- First-run onboarding and settings for username, goals, linked calories/macros, fibre, language, body units, tape units, and adaptive goal reviews.
- Persistent diary with entry editing/removal, previous-day calendar browser, and 7-day/4-week/6-month reports.
- Normalized foods, nutrients, EU-14 allergens, servings, preparation variants, provenance, immutable diary/recipe snapshots, and non-destructive migrations through schema v16.
- Interaction-ranked serving/gram memory, macro-aware personal-history recommendations, and distinct standalone versus recipe-ingredient usage.
- Personal-food editor with photos, known/custom units, prepackaged weight/fractions, private store/price metadata, and review opt-in.
- One-tap customization can either clone a personal variant or seed a recipe for additions such as milk and sugar, without mutating the source catalogue.
- Meal-prep recipes with ingredient search, per-ingredient cooking methods, cooked yield, portions, repeat batches, and search/quick-add integration.
- Open Food Facts text/barcode lookup and on-device nutrition-label OCR with mandatory review.
- Manual and scale-OCR body check-ins, optional guided tape measurements, trends, and non-medical adaptive review language.
- Versioned offline catalogue of 191 English/Romanian generic foods across 15 categories, with aliases, fibre, allergens, measures, cooking ingredients, and common Romanian foods.
- Density-aware millilitre input for common bundled liquids without treating cooking oil as water.
- One offline review ZIP with explicitly opted-in foods/recipes, explicitly flagged failed-scan images, optional feedback, capped UI-freeze/crash reports, and separate machine-readable catalogue/support manifests.
- Versioned full backup/restore with preview, transactional user-data replacement, cross-device image remapping, and current built-in catalogue preservation. Backups are currently unencrypted ZIPs and must be stored securely.
- Feedback threads support contextual image attachments that travel with review exports and full backups.

Private-alpha exit gate:

- Pass the release gate and the private-alpha smoke test on a real phone.
- Keep an offline in-app release history and versioned APK names for every tester handoff.
- Complete at least two weeks of daily use without data corruption or a blocking entry flow.
- Triage missing-food/search feedback before expanding the static catalogue again.

Remaining before a broader beta:

- Full private diary/profile backup, restore, and delete controls.
- Signed release configuration, private distribution, crash reporting with explicit consent, accessibility audit, and Android 6/8/modern-device matrix.
- Reminders and performance measurements on low-end hardware.

## M2 — Catalogue and barcode beta

- Measure Open Food Facts barcode/search coverage in Romanian and English; refine normalization, cache, retries, attribution, and licensing treatment.
- Add catalogue items from observed misses instead of speculative bulk growth.
- Hosted moderation queue for opted-in submissions, abuse controls, and immutable review history.
- Optional image catalogue with licensing and storage policy.

## M3 — Optional account and ecosystem

- Optional authentication, encrypted sync, conflict resolution, and device migration while preserving local-only use.
- Health Connect integration with granular consent, widget/app shortcuts, and deletion/retention controls.

## M4 — Intelligence and experimental estimation

- Explainable meal suggestions constrained by targets, allergens, preferences, available history, and recipe inventory.
- Evaluate whole-dish image estimation only against weighed meals and ship only with honest error bounds.
