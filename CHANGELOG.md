# Changelog

All notable user-facing changes to Calorie Quick are recorded here. The app is currently a private alpha, so features and stored-data formats may still evolve between builds.

## [Unreleased]

### Planned

- Private release signing and a controlled tester update channel.
- Wider physical-device and accessibility testing.
- Continued catalogue coverage and image refinement based on field feedback.

## [0.2.0-alpha05] - 2026-08-06

### Added

- A permanent **What’s new** screen in Settings with offline release notes.
- A one-time release summary after an installed app is upgraded.
- This user-facing changelog.

### Changed

- Consolidated the deployment notes for the backup, feedback-image, diagnostics, and layout work included in the preceding test builds.
- Added an explicit white background to adaptive and legacy launcher icons so launchers do not replace transparency with black.

### Fixed

- Made the one-time release summary scrollable while keeping its actions reachable on smaller screens and with larger text.
- Moved food-photo and catalogue-image decoding off the main thread, downsampled large local photos, and retained a bounded preview cache to reduce stutter when cards re-enter the screen.

## [0.2.0-alpha04] - 2026-08-06

### Added

- Versioned full backup and transactional restore for profiles, diary entries, body measurements, personal foods, recipes, usage history, preferences, attached images, and feedback.
- A restore preview showing the backup’s creation details and record counts before current user data is replaced.
- Optional image attachments for the local feedback thread and review export.

### Fixed

- Aligned all Today-header action icons with the selected day and subtitle.

### Security

- Backup archives are validated before restore and reject incompatible schemas, unsafe archive paths, unexpected tables, excessive sizes, and malformed image mappings.
- Backup ZIP files are not encrypted and must be stored securely.

## [0.2.0-alpha03] - 2026-08-06

### Added

- Itemized daily nutrition breakdowns instead of repeating only the summary totals.
- Saved crash and background-freeze reports for explicit inclusion in support exports.
- Manual-food creation at the end of food search results, including empty-result states.

### Fixed

- Hardened field diagnostics and upgraded the Compose runtime to avoid a lazy-list prefetch crash.
- Improved liquid unit coverage and quick-entry behavior.

## Earlier private-alpha work

- Offline normalized food catalogue with separate personal recommendations.
- Open Food Facts text and barcode lookup with OCR-assisted nutrition-label entry.
- Home-cooked recipes, ingredient usage, meal-prep leftovers, custom measures, and food variants.
- Daily history, calendar-based day replacement, nutrition reports, body measurements, and scale-report OCR.
- Macro, fiber, allergen, adaptive-goal, and custom-target support.
- Personal catalogue images, reviewable exports, Romanian localization, and responsive mobile layouts.

[Unreleased]: https://github.com/ShadoWZYE/calorie-quick-android/compare/v0.2.0-alpha05...HEAD
[0.2.0-alpha05]: https://github.com/ShadoWZYE/calorie-quick-android/compare/v0.2.0-alpha04...v0.2.0-alpha05
[0.2.0-alpha04]: https://github.com/ShadoWZYE/calorie-quick-android/compare/v0.2.0-alpha03...v0.2.0-alpha04
[0.2.0-alpha03]: https://github.com/ShadoWZYE/calorie-quick-android/releases/tag/v0.2.0-alpha03
