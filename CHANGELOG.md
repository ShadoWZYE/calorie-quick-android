# Changelog

All notable user-facing changes to Calorie Quick are recorded here. The app is currently a private alpha, so features and stored-data formats may still evolve between builds.

## [Unreleased]

## [0.2.1] - 2026-08-08

### Changed

- Food searches now show 10 results at a time with **Load more**; Open Food Facts uses server pagination metadata so incomplete products cannot accidentally hide later pages.
- Food search now matches English and Romanian category terms such as fish, seafood, bakery, pantry, or beverages across Today, recipe ingredients, and previous-day entry search.
- Replaced the two recipe actions on Today with one searchable recipe browser and a compact **Add new** action.
- The recipe browser now opens at full height and only closes from a downward list gesture when that gesture begins at the top.
- Local catalogue, recipe-browser, and previous-day searches now match recipe ingredient names in either language.
- Recipe ingredient cards use a more compact, aligned layout while keeping amount, unit, cooking method, and removal controls clear.
- Recipe ingredient search now ranks exact names and prefixes ahead of description-only matches, then uses prior usage as a tie-breaker.
- New recipe ingredients default to their learned or catalogue cooking method, falling back to **Raw / unchanged** when neither exists.
- Cooking-method choices use the full sheet width in a centered two-column grid, including a centered final odd item.
- Open Food Facts liquid packages now include bottle and millilitre measures, with improved beverage categorization.
- Preparing a saved recipe starts with a compact ingredient summary; changing finished weight proportionally scales every ingredient so nutrition per gram stays stable.
- Creating a new recipe from an existing recipe now carries forward and proportionally scales its editable ingredients instead of nesting the finished dish as one opaque ingredient.
- Open Food Facts results show product photos and package quantities, and liquid nutrition/quantity labels use millilitres.
- Store-local restricted-circulation barcodes remain usable locally without merging unrelated shared products.

### Added

- Privacy-safe UI smoothness summaries in review exports. Frame data is aggregated in memory by screen and written only when the app backgrounds, complementing the existing severe-freeze reports.
- Food photos can now be cropped inside Calorie Quick with a square workspace, precise zoom slider and buttons, focal-point pinch zoom, panning, and reset instead of relying on the phone gallery to return edits.
- Optional category selection and common package-unit suggestions in the personal-food editor.
- A validated promoted catalogue generated from clean collected products and opted-in recipes; suspicious, incomplete, private, or explicitly flagged records remain quarantined.
- A bowl-and-check badge reserved for explicitly verified records, with separate Calorie Quick, opt-in contributor, or Open Food Facts provenance labels on every promoted item.
- A confirmation-based way to discard prepared recipe leftovers.
- Product-data reporting for built-in foods as well as Open Food Facts products.
- Nutrition-label scanning directly inside the new/edit personal-food form, preserving existing fields while prefilling detected values.
- Plain water with glass, bottle, gram, and millilitre measures in the built-in catalogue.

### Fixed

- Open Food Facts searches render from memory while candidates are collected in the background; unused candidates stay out of the active catalogue, preventing duplicate result sections and search-time ANRs.
- Open Food Facts searches now show persistent progress while loading, and repeating a cleared query immediately restores its in-memory results. Personal-food creation appears only after the global lookup finishes and is visually secondary to the results.
- Open Food Facts cards and their network thumbnails are published one at a time instead of drawing an entire API page in one frame; stock placeholders are withheld while photos load, reducing result-arrival stalls while keeping progress visible until the page is complete.
- Open Food Facts duplicate detection now uses memoized identity sets and precompiled text normalization instead of repeatedly rescanning and renormalizing the full local catalogue on the UI thread.
- OCR engines now initialize only when a scan starts, preventing an unavailable scanner component from crashing ordinary app startup.
- Personal-food editing no longer crashes when a newly added liquid measure enters a lazy list.
- Successful barcode lookups remain visible after the imported product is cached locally, and manual entry no longer competes with an active lookup.
- Decimal-looking package weights are normalized instead of being rejected, and archived recipes are excluded from review exports.
- Open Food Facts thumbnails are cached on device and fitted without cutting away product edges.
- Large nutrition-label and scale-report photos no longer block the interface while being prepared for scanning.
- Curved two-column nutrition labels use their table layout to select the per-100 g/ml values instead of mixing serving columns.
- Review exports clear only the diagnostics, flagged images, and feedback that were successfully included.
- Existing local and remote food photos can be prepared for editing instead of reopening the camera.
- Today requires three deliberate Back presses to leave the app; secondary screens still return normally.
- Tapping outside an active Today search dismisses focus and the keyboard without activating the item underneath.
- Tapping outside active fields now dismisses the keyboard consistently across recipe, settings, check-in, feedback, and quick-add screens.
- Open Food Facts results now appear before the matching products finish saving to the on-device catalogue.
- Cooking-method sheets dismiss from a downward list gesture only when that gesture began with the list already at the top.
- Recipe ingredient taps cannot add a stale search result while the visible results are updating.
- Recipe history no longer shows empty placeholder batches.
- Recipes created from a catalogue food retain that food's bundled image in the editor and on the saved recipe card.

## [0.2.0-alpha06] - 2026-08-06

### Security

- Review-export images are orientation-corrected, bounded to 2048 pixels, flattened, and re-encoded so EXIF, XMP, GPS, camera, and editing metadata are not shared.

## [0.2.0-alpha05] - 2026-08-06

### Added

- A permanent **What’s new** screen in Settings with offline release notes.
- A one-time release summary after an installed app is upgraded.
- This user-facing changelog.

### Changed

- Added an explicit white background to adaptive and legacy launcher icons so launchers do not replace transparency with black.

### Fixed

- Made the one-time release summary scrollable while keeping its actions reachable on smaller screens and with larger text.
- Food and catalogue images scroll more smoothly, including large photos.
- Added a direct Files/Downloads image route alongside the system photo library for food photos, label scans, scale reports, and feedback attachments.

## [0.2.0-alpha04] - 2026-08-06

### Added

- Versioned full backup and transactional restore for profiles, diary entries, body measurements, personal foods, recipes, usage history, preferences, attached images, and feedback.
- A restore preview showing the backup’s creation details and record counts before current user data is replaced.
- Optional image attachments for the local feedback thread and review export.

### Fixed

- Aligned all Today-header action icons with the selected day and subtitle.

### Security

- Backups are checked before restore, and incompatible or damaged archives are rejected.
- Backup ZIP files are not encrypted and must be stored securely.

## [0.2.0-alpha03] - 2026-08-06

### Added

- Itemized daily nutrition breakdowns instead of repeating only the summary totals.
- Saved crash and background-freeze reports for explicit inclusion in support exports.
- Manual-food creation at the end of food search results, including empty-result states.

### Fixed

- Fixed a crash that could occur while scrolling long lists.
- Improved liquid unit coverage and quick-entry behavior.

## Earlier private-alpha work

- Offline normalized food catalogue with separate personal recommendations.
- Open Food Facts text and barcode lookup with OCR-assisted nutrition-label entry.
- Home-cooked recipes, ingredient usage, meal-prep leftovers, custom measures, and food variants.
- Daily history, calendar-based day replacement, nutrition reports, body measurements, and scale-report OCR.
- Macro, fiber, allergen, adaptive-goal, and custom-target support.
- Personal catalogue images, reviewable exports, Romanian localization, and responsive mobile layouts.

[Unreleased]: https://github.com/ShadoWZYE/calorie-quick-android/compare/v0.2.1...HEAD
[0.2.1]: https://github.com/ShadoWZYE/calorie-quick-android/compare/v0.2.0-alpha06...v0.2.1
[0.2.0-alpha06]: https://github.com/ShadoWZYE/calorie-quick-android/compare/v0.2.0-alpha05...v0.2.0-alpha06
[0.2.0-alpha05]: https://github.com/ShadoWZYE/calorie-quick-android/compare/v0.2.0-alpha04...v0.2.0-alpha05
[0.2.0-alpha04]: https://github.com/ShadoWZYE/calorie-quick-android/compare/v0.2.0-alpha03...v0.2.0-alpha04
[0.2.0-alpha03]: https://github.com/ShadoWZYE/calorie-quick-android/releases/tag/v0.2.0-alpha03
