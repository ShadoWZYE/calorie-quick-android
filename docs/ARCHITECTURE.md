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

Room persists `UserProfile`, `Food`, `Serving`, `ServingUsage`, and `DiaryEntry`. Diary rows contain food-name, entered-unit, normalized-weight, and nutrition snapshots so historical totals do not change when catalogue records are edited later. Nutrition is stored as integer milligrams; weight and decimal entered amounts use scaled integers.

Each diary row retains a consumption timestamp. Repeated additions of the same food within two minutes are accumulated transactionally into one row; additions outside that window remain separate events for later meal grouping and time-based reports.

Serving choices retain unit-level use count and recency. Exact food + unit + amount combinations have separate preference records, allowing multiple reusable quick-pick presets to be ranked by interaction count with recency only as a tie-breaker. Choosing a measurement unit itself starts from a neutral base amount. Nutrition is always normalized to grams.

Planned extensions:

- `UserProfile`: unit system, locale, measurements, activity assumptions, consent/settings versions.
- `GoalPlan`: effective dates, calorie and nutrient targets, derivation method, manual override.
- `Food`: stable local ID, source/provenance, source ID/barcode, localized names, brand, image references, verification state.
- `NutrientValue`: nutrient identifier, amount, unit, basis (100 g, 100 ml, serving), source and confidence.
- `Serving`: grams/ml conversion, label, locale, user-specific favourite/last-used state.
- `DiaryEntry`: timestamp, meal, food snapshot, amount, nutrition snapshot; snapshots preserve historical totals after food edits.
- `Recipe` and `RecipeIngredient`: yield and portions backed by foods.
- `SyncOperation`: idempotency key, entity/version, operation, retry state.

### Catalogue schema for generic and packaged foods

The compact fixed columns in the current local seed table are a prototype read model, not the final catalogue contract. Before catalogue import, migrate to normalized records that support:

- `FoodProduct`: generic/branded kind, localized display names, brand, source/provenance, verification state, image references, ingredients, and region.
- `Package`: barcode, net quantity and unit, container count, package label, and optional image. A 500 ml bottle is a package, not a hard-coded gram serving.
- `MeasureOption`: food-specific units such as gram, millilitre, piece, cup, scoop, or bottle; quantity and dimension; optional conversion to mass/volume. Conversions must not assume 1 ml = 1 g without verified density.
- `NutritionPanel`: basis quantity/unit such as per 100 g, per 100 ml, or per declared serving, plus source and effective/version metadata.
- `NutrientDefinition`: canonical nutrient ID, localized label, dimension/unit family, and display group.
- `NutrientValue`: panel + nutrient + amount/unit, with explicit known/unknown state and field-level provenance. Zero is a measured/declared value; missing is not zero.
- `AllergenDeclaration`: EU allergen ID, `contains`/`may_contain`/`free_from` declaration, source, and confidence.
- `PreparationVariant`: links a base food to a method such as raw, boiled, fried, baked, or grilled. It owns preparation-specific nutrition, yield/water change, optional image, and serving conversions; it is not a duplicate base food.
- `PreparationUsage`: user-scoped use count and recency for each food + preparation pairing. The chooser defaults to the most-used valid preparation, using recency only as a tie-breaker.
- `Store`: user-scoped store identity with optional branch/location metadata; a free-text name remains valid when structured location is unwanted.
- `PriceObservation`: package/food, store, price in integer minor currency units, currency, quantity basis, and observation timestamp. “Last known price” is derived from history rather than overwriting it.
- `SharingConsent`: explicit per-record opt-in and consent version. Personal foods, stores, prices, and images remain private by default.
- `CatalogueSubmission`: immutable snapshot of an opted-in personal food or edit, with provenance and review status. Submission never promotes a record directly into the canonical catalogue.

For example, a 500 ml Pepsi-like product can store a per-100-ml panel and a `1 bottle = 500 ml` package option. Logging one bottle multiplies every nutrient by five, snapshots the full calculated panel into the diary, and shows only calories plus the user’s chosen quick metrics. The complete breakdown remains available from product details and reports.

The diary should preserve a versioned nutrition snapshot rather than only reference the mutable catalogue record. This keeps old reports stable after a manufacturer reformulates a product or an imported value is corrected.

Use integer minor units or scaled decimals for persisted nutrition, not floating point. Domain/UI models convert persisted values to `Double` only in memory.

## Search and ranking

Use Room FTS for normalized local search. Normalize diacritics for matching while preserving display text. Explicit search ranking order:

1. Exact barcode.
2. Exact/prefix matches in personal foods.
3. Recency-frequency score with time decay and meal/time-of-day context.
4. Cached central catalogue.
5. Remote search after a short debounce, merged without reshuffling items already under the user's finger.

Record chosen result position and subsequent corrections locally (with opt-in analytics later) to improve ranking.

When search is empty, Today shows recommendations instead: remaining macro needs are the primary ranking signal, foods rich in an already exceeded macro receive a strong penalty, and accumulated food frequency is the secondary preference signal. Food cards expose only projected macro problems, calculated from the declared neutral default serving or 100 g—not the user's remembered quantity—so ranking trade-offs remain visible. The first implementation is deterministic and entirely local.

## Central catalogue strategy

Put catalogue adapters behind a server-side normalization API before public launch. The client should not embed provider API secrets and should receive one versioned canonical schema. Use Open Food Facts first for European packaged/barcoded coverage and USDA FoodData Central as a complementary generic/nutrient source. Never silently merge conflicting nutrient values; retain field-level provenance and prefer the region-appropriate, most complete, most recently verified record.

Community writes should enter a moderation/data-quality pipeline. Keep personal edits private unless the user explicitly submits them.

### Preparation selection flow

Foods with multiple preparations open a first-stage 2-column method grid with concise labels and optional suggestive images. The most-used preparation is preselected. Confirming it opens the quantity sheet; foods with only one preparation skip the chooser. Preparation can change nutrition through added oil, water loss, or yield, so diary entries snapshot the selected preparation's calculated panel.

### Personal food, retail, and community boundary

Personal food creation works fully offline and initially requires only a name, nutrition basis, calories, and known macros. Barcode lookup and label OCR may prefill values, but the user confirms them. Store and price observations are optional and user-scoped. Future shopping optimization uses timestamped prices, package quantities, preferred stores, diet constraints, and recipe demand while exposing stale or missing prices.

Opt-in sharing creates a separate submission snapshot. Private store/location/price fields are excluded unless separately selected, image metadata is stripped, and the submission enters automated validation plus review before canonical publication. Rejection or correction never damages the user's private food.

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
