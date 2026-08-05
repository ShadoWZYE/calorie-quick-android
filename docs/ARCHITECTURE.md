# Architecture

## Recommended shape

Use a native Kotlin Android app with Jetpack Compose and unidirectional state flow. Start as a modular monolith: one application module now, then split stable boundaries only when build time or ownership justifies it.

```text
Compose UI -> ViewModel/state holder -> use cases -> repositories
                                              |-> Room (authoritative local store)
                                              |-> DataStore (small preferences)
                                              |-> catalogue API/cache
                                              |-> WorkManager sync queue
```

The local database is the source of truth. Every daily-log mutation commits locally first and updates the screen optimistically. Network import and eventual account sync are additive; neither may block logging.

## Data model

The first Room schema now persists `UserProfile`, `Food`, `Serving`, and `DiaryEntry`. Diary rows contain food-name and nutrition snapshots so historical totals do not change when catalogue records are edited later. Nutrition is stored as integer milligrams; weight is stored as integer grams.

Planned extensions:

- `UserProfile`: unit system, locale, measurements, activity assumptions, consent/settings versions.
- `GoalPlan`: effective dates, calorie and nutrient targets, derivation method, manual override.
- `Food`: stable local ID, source/provenance, source ID/barcode, localized names, brand, image references, verification state.
- `NutrientValue`: nutrient identifier, amount, unit, basis (100 g, 100 ml, serving), source and confidence.
- `Serving`: grams/ml conversion, label, locale, user-specific favourite/last-used state.
- `DiaryEntry`: timestamp, meal, food snapshot, amount, nutrition snapshot; snapshots preserve historical totals after food edits.
- `Recipe` and `RecipeIngredient`: yield and portions backed by foods.
- `SyncOperation`: idempotency key, entity/version, operation, retry state.

Use integer minor units or scaled decimals for persisted nutrition, not floating point. Domain/UI models convert persisted values to `Double` only in memory.

## Search and ranking

Use Room FTS for normalized local search. Normalize diacritics for matching while preserving display text. Ranking order:

1. Exact barcode.
2. Exact/prefix matches in personal foods.
3. Recency-frequency score with time decay and meal/time-of-day context.
4. Cached central catalogue.
5. Remote search after a short debounce, merged without reshuffling items already under the user's finger.

Record chosen result position and subsequent corrections locally (with opt-in analytics later) to improve ranking.

## Central catalogue strategy

Put catalogue adapters behind a server-side normalization API before public launch. The client should not embed provider API secrets and should receive one versioned canonical schema. Use Open Food Facts first for European packaged/barcoded coverage and USDA FoodData Central as a complementary generic/nutrient source. Never silently merge conflicting nutrient values; retain field-level provenance and prefer the region-appropriate, most complete, most recently verified record.

Community writes should enter a moderation/data-quality pipeline. Keep personal edits private unless the user explicitly submits them.

## Images and scanning

- Store user images in app-private storage, generate thumbnails, strip unnecessary metadata, and make upload opt-in.
- Barcode phase: Google Code Scanner for the simplest permissionless flow, or CameraX + ML Kit when a custom continuous-scanning UI becomes necessary.
- Label/ingredient phase: on-device ML Kit text recognition, followed by locale-aware parsing and an explicit confirmation screen.
- Whole-dish estimation is a later experimental feature: require portion/scale confirmation, show a range and confidence, and never present a single image-derived number as precise.

## Privacy and security

- No account or network permission is required for the initial local MVP.
- Encrypt transport, keep secrets server-side, redact logs, and minimize retained images/analytics.
- Provide export, delete, retention controls, and a privacy policy before collecting health/profile data in production.
- Health Connect is opt-in and requested per data type only when the integration is used.
- Threat-model authentication, sync conflicts, community uploads, image metadata, backups, and database migrations before those features ship.

## Quality and performance budgets

- Warm launch to interactive Today: target under 500 ms on a representative mid-range physical device.
- Local search response: target under 50 ms for the 95th percentile.
- Saved entry visible after tap: target under 100 ms; durable local commit under 250 ms.
- Zero network calls on warm launch unless a stale-while-revalidate job is due.
- Add Macrobenchmark coverage and an app-specific Baseline Profile for launch, search, and quick-add before beta.

## Testing

- Pure unit tests for nutrition math, units, goal estimates, rankings, date boundaries, and DST.
- Room migration and repository contract tests.
- Compose UI tests for the core loop in `en` and `ro`, large fonts, and TalkBack semantics.
- Contract tests using frozen catalogue payloads; do not rely on live providers in CI.
- Macrobenchmarks on a physical reference device and screenshot/accessibility checks in release gates.
