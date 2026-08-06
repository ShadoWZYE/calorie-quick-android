# Private field-test handoff

## Install

Build `app/build/outputs/apk/debug/app-debug.apk` with the release gate in `docs/TESTING.md`, enable USB debugging on the test phone, then run:

```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

`-r` preserves an existing installation's private data. Do not uninstall the app between updates. Keep the same application ID and signing identity throughout the test.

## Tester expectations

- This is a private alpha, not medical advice and not a public-store release.
- Nutrition, allergens, OCR, body-composition estimates, and adaptive goals must be reviewed by the user.
- Generic catalogue values are starting points. Use package labels or weighed recipes when accuracy matters.
- Do not rely on review exports as a diary backup.

## Stop-test conditions

Stop using the affected flow and retain screenshots/logs if totals change after relaunch, entries attach to the wrong day, personal records disappear, recipe yield changes historical entries, allergen declarations are hidden, or an update cannot preserve the database. These are release-blocking defects.
