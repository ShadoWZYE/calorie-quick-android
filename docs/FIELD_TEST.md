# Private field-test handoff

## Install

Build the non-debuggable `fieldTest` APK with the release gate in `docs/TESTING.md`, enable USB debugging on the test phone, then run:

```powershell
adb install -r app/build/outputs/apk/fieldTest/CalorieQuick-0.2.1-test-fieldTest.apk
```

`-r` preserves an existing installation's private data. Do not uninstall the app between updates. Keep the same application ID and signing identity throughout the test.

The field-test build is signed with the private test key but runs without debugger overhead. It collects only aggregate per-screen frame counts while the app is foregrounded. Reports are written when the app backgrounds, are capped locally, and leave the device only when performance diagnostics are explicitly included in a review export.

Before updating a physical device, compare the candidate with the installed app without changing either one:

```powershell
powershell -ExecutionPolicy Bypass -File tools/inspect_android_update.ps1 -ApkPath "C:\path\to\CalorieQuick.apk"
```

When an emulator and phone are connected together, add `-Serial <adb-device-serial>`. The check reports the device version, package version, and whether the signing certificates match. Add `-Install` only after the compatibility result succeeds; installation still uses data-preserving replacement and never uninstalls the existing app.

Follow the versioning, signing, migration, and handoff rules in [UPDATES.md](UPDATES.md) for every tester update.

## Inspect a review export

The local validator supports both legacy exports and review-bundle schema v2. It validates paths, sizes, component schemas, image metadata, and v2 media hashes without importing data or extracting images:

```powershell
python tools/review_bundle_parser.py "C:\path\to\calorie-quick-review.zip" --output-dir build\reports\review-bundles
```

It writes a JSON report for tooling and a privacy-conscious Markdown summary that omits feedback text and image contents.

For editable triage, comments, corrections, evidence viewing, and explicit implementation promotion, use the private Windows inbox documented in [REVIEW_INBOX.md](REVIEW_INBOX.md).

## Tester expectations

- This is a private alpha, not medical advice and not a public-store release.
- Nutrition, allergens, OCR, body-composition estimates, and adaptive goals must be reviewed by the user.
- Generic catalogue values are starting points. Use package labels or weighed recipes when accuracy matters.
- Do not rely on review exports as a diary backup.

## Stop-test conditions

Stop using the affected flow and retain screenshots/logs if totals change after relaunch, entries attach to the wrong day, personal records disappear, recipe yield changes historical entries, allergen declarations are hidden, or an update cannot preserve the database. These are release-blocking defects.
