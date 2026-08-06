# Testing guide

Use the **Calorie Quick Test** Desktop or Start Menu shortcut for emulator sessions. It builds, installs, and opens the latest debug APK without clearing existing app data.

Never use Gradle's `connectedDebugAndroidTest` against an emulator or phone whose diary matters: the instrumentation workflow can uninstall the package and erase its private data. Use a disposable emulator for connected tests.

## Release gate

Run:

```powershell
./gradlew test lintDebug assembleDebug assembleDebugAndroidTest assembleRelease
```

The checked-in catalogue test validates its schema, bilingual names, nutrition bounds, stable IDs, allergens, preparations, aliases, categories, and representative Romanian coverage.

## Private-alpha smoke test

1. Complete onboarding in English and Romanian; verify linked calorie and macro targets, fibre, measurement units, and username survive relaunch.
2. Confirm an empty **Recommended** list explains that only previously logged foods appear there.
3. Browse **All foods** and several category chips. Search English, Romanian, unaccented Romanian, and an alias such as `aubergine`. While a mismatched category is selected, search `coffee` and confirm the app falls back to all-category results with an explanation.
4. Add a food, then confirm only that previously used food can appear under **Recommended**. Recipe-only ingredient use must not make it recommended.
5. Search a built-in food, tap `+`, and use the labeled **Customize** action in the sheet header. Confirm **Create personal variant** clones its nutrition, fibre, allergens, and measures under a new name with no barcode. Verify the prominent photo card can take or choose an image. Then choose **Build recipe** from coffee, add milk and sugar, switch each ingredient between grams and its known units, and verify normalized grams, total yield, portions, and calculated nutrition. Neither path may alter the source template.
6. Confirm cards label standalone logs and recipe-batch uses separately.
7. Verify quantity memory: serving quantities become chips above the amount field, gram quantities remain separate, interaction count controls ordering, and the neutral field default remains unchanged.
8. Verify the quick-add sheet expands toward full height and scrolls on a small display, including a food with preparation choices and many measures.
9. Push fat over target. Confirm fat turns red in the summary and only the relevant projected overage is shown on candidate cards.
10. Create and edit a personal food with fibre, allergens, photo, teaspoon or other known/custom measures, package weight/fractions, store, price, and review opt-in.
11. Search a packaged product in Open Food Facts and scan a barcode. Confirm imported community data is reviewable before local save. Test offline/error handling.
12. Scan a nutrition label and review the OCR prefill. Incorrect or ambiguous values must remain editable or blank.
13. Create a cooked recipe, record yield and portions, add a portion and grams to the diary, then prepare another batch. Verify ingredient and standalone histories stay separate.
14. Add, edit, and remove diary entries; use the Today heading to replace the active day in place, then inspect the 7-day, 4-week, and 6-month reports.
15. Manually add a body check-in with tape measurements, import a scale screenshot, and review an adaptive-goal suggestion without accepting it.
16. Rotate, background, kill, and relaunch the app. Verify profile, foods, recipes, photos, history, and totals remain intact.
17. Enter coffee, milk, juice, and cooking oil in millilitres; verify the displayed gram equivalents use each food's density and survive relaunch.
18. Flag a failed scan, add optional feedback, export the combined review ZIP, and inspect its separate `catalogue/` and `support/` folders. Review the independent selections for scans, UI-freeze reports, crash reports, and feedback. Confirm private/non-opted-in foods and unflagged scans are absent.
19. Install the next versioned `CalorieQuick-<version>-debug.apk` with `adb install -r`; verify schema migration preserves profile, diary, personal foods, recipes, images, and feedback. Follow [UPDATES.md](UPDATES.md).
20. After an upgrade, verify the current What’s new summary appears once, opens the full release history, does not repeat after dismissal, and remains available from Settings.
21. Check the launcher icon on each physical-device launcher; its background must remain white rather than transparent, black, or launcher-tinted.
20. Tap the daily summary for today and a past day. Confirm the sheet lists each logged item with quantity, time, calories, macro contribution, and an accurate daily total rather than repeating goal progress.
21. Create a full backup from Settings. Select it for restore and verify the preview counts/profile version before cancelling. On a disposable test profile, confirm restore replaces user data while retaining the current built-in catalogue.
22. Attach an image to a feedback message, confirm the thumbnail remains after reopening Feedback, and verify the image appears in both an opted-in review export and a full backup round trip.
23. On a narrow phone, confirm the Today/date block and History, Progress, Feedback, and Settings icons share one vertically centered header row.

## Field feedback to capture

- Search terms with no useful local or Open Food Facts result.
- Time and taps from launch to a saved food.
- Awkward serving sizes, cooking methods, package splits, or recipes.
- Incorrect nutrition/allergen data and the provenance shown.
- Text that is unclear, clipped, too small, or poorly translated.
- Crashes, freezes, visual jumps, or totals that appear incorrect.

Include the app version, phone model, Android version, screenshot, and exact preceding action.

## Private-alpha boundaries

- The 191-food built-in catalogue contains rounded generic reference values; brands and home recipes vary. Package labels and weighed saved recipes take precedence.
- Open Food Facts is community data and needs user review. Network coverage is not guaranteed.
- OCR is an entry aid, never an authoritative nutrition source.
- The release APK produced locally is unsigned. Use the debug APK for direct trusted-device testing until a private signing key and distribution channel are configured.
- Full diary/profile backup and restore is not yet available. Review-export ZIPs contain opted-in catalogue items only.
- Cloud backup is disabled because the database contains private nutrition and body data.
