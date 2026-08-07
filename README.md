# Calorie Quick

**A local-first Android nutrition tracker built to make honest food logging feel almost effortless.**

Calorie Quick opens directly to the day you are tracking. Frequent foods take a few seconds to log, unfamiliar packaged foods can be found by name or barcode, and home-cooked dishes remain reusable recipes instead of becoming one-off calculations. The app keeps the diary useful offline and makes sharing diagnostic or catalogue data an explicit choice.

The project is currently a private Android alpha for English and Romanian speakers. It is suitable for hands-on testing, but it is not medical software and is not yet a public-store release.

## Why it is interesting

Most calorie trackers make the easy case tolerable and the real-life case awkward. Calorie Quick is being designed around both:

- a remembered serving or gram quantity can be logged in one tap;
- recommendations come only from foods the user has actually logged and account for the day's remaining targets;
- generic foods, packaged products, personal variants, and recipes share one searchable workflow;
- recipes separate the reusable template, a fresh cooking run, finished weight, portions, and remaining leftovers;
- historical diary entries retain nutrition snapshots, so later catalogue corrections do not rewrite the past;
- provenance distinguishes bundled Calorie Quick data, imported community data, user opt-ins, and records that were actually reviewed;
- private data stays on the device unless the user deliberately exports or shares it.

## What works today

### Fast daily tracking

- Today and past-day diary views with calendar navigation, editing, deletion, and per-item nutrition breakdowns.
- Calories, protein, carbohydrates, fat, and fibre targets with visible overages.
- Interaction-ranked quantity memory, food-specific units, grams, millilitres with known density, package fractions, and common household measures.
- Local recommendations based on previously logged foods; the bundled catalogue remains separately searchable.
- Seven-day, four-week, and six-month history summaries.

### Foods, search, and capture

- A versioned offline English/Romanian catalogue of 192 generic foods and ingredients, including aliases, categories, images, allergens, fibre, units, and preparation variants.
- Search across names, aliases, categories, and recipe ingredients, with all-category fallback when a selected filter has no matches.
- Open Food Facts name and barcode lookup with local response caching and mandatory review before save.
- Nutrition-label OCR that prefills the same editable personal-food form.
- Personal foods with photos, custom measures, package size and splits, optional store/price notes, allergen declarations, and per-record sharing consent.
- Create a personal variant or start a recipe from an existing catalogue result without changing the original.

### Home cooking and meal preparation

- Reusable recipe templates with searchable ingredients, ingredient amounts/units, per-ingredient cooking methods, allergens, photos, cooked yield, and portions.
- A separate **prepare fresh batch** flow that preserves ingredient calories while scaling nutrition density from the measured finished weight.
- Leftover inventory for quick logging by portion or grams; empty batches do not clutter the home screen.
- Recipe use and standalone food use are tracked separately.

### Goals, measurements, and privacy

- Onboarding and auto-saving settings for identity label, language, units, activity, goal, and estimated or custom targets.
- Linked calorie/macro recalculation with discrepancy warnings; fibre remains independently configurable.
- Manual body check-ins, optional guided tape measurements, scale-screenshot OCR, trends, and non-medical adaptive goal reviews.
- Local feedback threads with optional screenshots.
- Full user backup/restore, distinct from a review export.
- Explicitly scoped review ZIPs containing only selected foods/recipes and selected support data; exported images are sanitized of EXIF/XMP metadata.

## Current testing focus

The current consolidated private-alpha release is `0.2.1`. Before broader distribution, the project is concentrating on:

1. sustained real-device testing of search, recipes, OCR, updates, and preserve-data migrations;
2. UI smoothness on older hardware using privacy-safe aggregate jank and severe-freeze reports;
3. observed catalogue gaps and incorrect-product reports rather than speculative data growth;
4. accessibility, large-text, narrow-screen, and Android-version coverage;
5. production signing and a controlled private distribution channel.

See the [roadmap](docs/ROADMAP.md), [testing guide](docs/TESTING.md), and [changelog](CHANGELOG.md) for the detailed state.

## Technology and data model

- Kotlin, Jetpack Compose, Room, and a single-app modular-monolith architecture.
- Android 6.0 (API 23) minimum; compile/target SDK 37.
- Room is the authoritative local store. Diary and recipe snapshots protect historical integrity.
- Nutrition is normalized to mass internally, while the UI preserves the unit the user entered.
- Bundled catalogue data is versioned and migrated without overwriting personal records.
- Network sources are additive: logging, history, recipes, and the personal catalogue remain usable offline.

More detail lives in [architecture](docs/ARCHITECTURE.md), [catalogue policy](docs/CATALOGUE.md), and the [product brief](docs/PRODUCT.md).

## Build and test

Prerequisites: Android Studio 2026.1.3 or newer, JDK 17, Android SDK 37, and an Android 6.0+ device or emulator.

```powershell
# Unit tests and the device-test candidate
.\gradlew.bat testDebugUnitTest assembleFieldTest lintFieldTest

# Local development APK
.\gradlew.bat assembleDebug
```

Artifacts:

- `app/build/outputs/apk/fieldTest/CalorieQuick-<version>-test-fieldTest.apk` — non-debuggable, test-signed real-device candidate used for meaningful performance measurements.
- `app/build/outputs/apk/debug/CalorieQuick-<version>-dev-debug.apk` — developer/emulator build with debugging support.
- `app/build/outputs/apk/release/CalorieQuick-<version>-release.apk` — consolidated stable-alpha build; publish only after the release gate.

For Windows emulator work, double-click `Launch Calorie Quick.bat` or run:

```powershell
.\tools\launch-test.ps1
```

Never run connected instrumentation tests against a phone whose diary matters; Android test installation can remove app data. Follow [FIELD_TEST.md](docs/FIELD_TEST.md) and [UPDATES.md](docs/UPDATES.md) for preserve-data device updates.

## Repository map

```text
app/                         Android application and tests
app/src/main/assets/         Versioned built-in/promoted catalogue data
docs/                        Product, architecture, test, update, and data policies
tools/                       Catalogue, review-bundle, promotion, and Windows test tools
CHANGELOG.md                 Consolidated user-facing release history
docs/DEBUG_CHANGELOG.md      Internal development-build history
```

## Data contribution and review direction

During private testing, Open Food Facts responses and explicitly opted-in personal foods/recipes can be bundled into one review archive. The local Windows review process normalizes and accumulates those records automatically. Ordinary unflagged products do not create manual-review work; suspicious or explicitly reported records do. A future hosted catalogue can build on the same provenance and verification states without pretending that user opt-in alone means Calorie Quick verified the data.

## Road ahead

The next milestones are a stable private alpha, a hosted moderation/catalogue pipeline, optional encrypted sync, Health Connect and quick-entry surfaces, and eventually explainable suggestions constrained by targets, allergens, history, and available recipes. Public release also requires clinical-language review, accessibility validation, production key management, provider attribution/licensing checks, and a clear privacy policy.

Calorie Quick should become more useful as it learns the user's real foods—without requiring the user's private diary to become the product.
