# Research notes and decisions

Research checked 2026-08-05. Links are primary documentation or original research where possible.

## Decisions

### Native Android, Kotlin, Compose

Google recommends Compose for modern Android UI and a layered architecture with repositories, coroutines/flows, and unidirectional data flow. A native app gives the best access to widgets, app shortcuts, CameraX/ML Kit, Health Connect, background work, and Android performance tooling. Cross-platform code is unnecessary while Android is the only committed client.

Source: https://developer.android.com/topic/architecture/recommendations

### Local database is authoritative

Room provides the SQLite abstraction needed for robust local data and FTS-backed search. Network sources populate or refresh local records; they do not own the diary. DataStore is reserved for small preferences, while WorkManager handles deferred catalogue/sync operations.

Source: https://developer.android.com/jetpack/androidx/releases/room

### Provider strategy

Open Food Facts is the strongest first candidate for European packaged products, barcodes, ingredients and images. Its current v3 API is recommended for new integrations, but volunteer data has no accuracy guarantee. Its database is ODbL and product images are CC BY-SA, so attribution/share-alike and database-combination implications need a legal/licensing design review.

USDA FoodData Central is useful for generic and US foods and provides public-domain/CC0 data. It requires a protected API key and defaults to 1,000 requests/hour/IP, which reinforces the decision to put it behind our service rather than ship the key in the app.

Sources:

- https://openfoodfacts.github.io/documentation/docs/Product-Opener/api/
- https://fdc.nal.usda.gov/api-guide/

### Energy estimates are guidance, not truth

The Mifflin–St Jeor equation was derived from 498 healthy adults aged 19–78 and predicts resting energy expenditure, not a guaranteed daily expenditure. The app should name the method, expose assumptions, allow manual targets, distinguish BMR/REE from activity-adjusted estimates, and avoid applying adult equations to unsupported populations.

Source: https://pubmed.ncbi.nlm.nih.gov/2305711/

### Scanning sequence

ML Kit barcode scanning runs on-device and supports standard retail formats. The permissionless Google Code Scanner is the fastest initial implementation; a custom CameraX pipeline is justified later for continuous scan and overlay UX. Limit formats to retail codes when using ML Kit to reduce latency. Text recognition should be a separate assisted-entry flow with user confirmation.

Source: https://developers.google.com/ml-kit/vision/barcode-scanning/android

### Health data integration is later and optional

Health Connect supports nutrition and body measurements and gives the user granular control. Availability differs by Android version, publishing requires health-data declarations and a privacy policy, and apps should request only the data types they use. It is valuable for weight/activity context but is not needed for the fast local core.

Sources:

- https://developer.android.com/health-and-fitness/health-connect
- https://developer.android.com/health-and-fitness/health-connect/publish

### Performance must be measured

Compose ships useful profiles, but an app-specific Baseline Profile should cover launch, search, and add-entry. Android reports roughly 30% code-execution improvement from first launch for profiled paths; actual benefit must be verified with Macrobenchmark on physical hardware.

Source: https://developer.android.com/develop/ui/compose/performance/baseline-profiles

## Key risks

- Catalogue correctness and serving-unit ambiguity can undermine trust faster than missing coverage.
- ODbL/share-alike obligations may constrain how a combined central database is distributed.
- Reminders and aggressive deficits can create harmful behaviour; clinical/nutrition review is required before recommendations ship.
- Romanian search needs diacritic-tolerant matching, local brands, translated generic foods, and label conventions—not UI translation alone.
- Whole-dish vision has unavoidable portion-size ambiguity; it should report ranges and confidence.
- Sync/community features substantially expand security, moderation, deletion, and operational scope.

## Deferred investigations

- Validate Romanian barcode coverage with a representative retail basket.
- Get counsel on Open Food Facts ODbL compatibility with the planned normalized catalogue.
- Define nutrient ontology and EU/US label unit mapping.
- Usability-test quantity controls (grams, millilitres, household servings, pieces) with Romanian and English users.
- Commission a registered dietitian review of goal guardrails, warnings, and copy.

