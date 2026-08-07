# Testing guide

Use the **Calorie Quick Test** Desktop or Start Menu shortcut for emulator sessions. It builds, installs, and opens the latest debug APK without clearing existing app data.

Never use Gradle's `connectedDebugAndroidTest` against an emulator or phone whose diary matters: the instrumentation workflow can uninstall the package and erase its private data. Use a disposable emulator for connected tests.

## Release gate

Run:

```powershell
./gradlew test lintDebug assembleDebug assembleDebugAndroidTest assembleRelease
```

The checked-in catalogue test validates its schema, bilingual names, nutrition bounds, stable IDs, allergens, preparations, aliases, categories, and representative Romanian coverage.

Run `python -m unittest tools/test_catalogue_images.py` when changing built-in thumbnails. It validates every food image is 256×256 and rejects the displaced neighboring-tile pattern found in the original mackerel/mussels/pomegranate/sardines/shrimp batch.

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
13. Create a cooked recipe containing a food with multiple methods (for example boiled/fried egg). Confirm the method picker shows images in a 2-column grid, changing method changes the ingredient nutrition/image, and the saved batch retains that method after relaunch. Prepare another batch and verify the most-used method learns from recipe use while legacy recipes without a saved method retain their old nutrition. Add both a portion and grams to the diary; ingredient and standalone histories must stay separate.
14. Add, edit, and remove diary entries; use the Today heading to replace the active day in place, then inspect the 7-day, 4-week, and 6-month reports.
15. Manually add a body check-in with tape measurements, import a scale screenshot, and review an adaptive-goal suggestion without accepting it.
16. Rotate, background, kill, and relaunch the app. Verify profile, foods, recipes, photos, history, and totals remain intact.
17. Enter coffee, milk, juice, and cooking oil in millilitres; verify the displayed gram equivalents use each food's density and survive relaunch.
18. Flag a failed scan, add optional feedback, and export the combined review ZIP. Review the independent selections for scans, UI-freeze reports, crash reports, and feedback. Confirm private/non-opted-in foods and unflagged scans are absent. Run `python tools/review_bundle_parser.py <zip>` and confirm schema v2 passes, the exact build is recorded, every image hash matches, and no exported image retains EXIF/XMP metadata.
19. Install the next versioned `CalorieQuick-<version>-debug.apk` with `adb install -r`; verify schema migration preserves profile, diary, personal foods, recipes, images, and feedback. Follow [UPDATES.md](UPDATES.md).
20. After an upgrade, verify the current What’s new summary appears once, scrolls independently at large font sizes without hiding its actions, opens the full release history, does not repeat after dismissal, and remains available from Settings.
21. Check the launcher icon on each physical-device launcher; its background must remain white rather than transparent, black, or launcher-tinted.
22. Browse a long built-in catalogue, scroll several screens down and rapidly back up, and confirm cards re-enter smoothly without visible pauses while their images load.
23. At every image attachment point, verify both Photos/Albums and Files/Downloads routes; place a test image in Downloads and confirm it can be selected through the file browser.
20. Tap the daily summary for today and a past day. Confirm the sheet lists each logged item with quantity, time, calories, macro contribution, and an accurate daily total rather than repeating goal progress.
21. Create a full backup from Settings. Select it for restore and verify the preview counts/profile version before cancelling. On a disposable test profile, confirm restore replaces user data while retaining the current built-in catalogue.
22. Attach an image to a feedback message, confirm the thumbnail remains after reopening Feedback, and verify the image appears in both an opted-in review export and a full backup round trip.
23. On a narrow phone, confirm the Today/date block and History, Progress, Feedback, and Settings icons share one vertically centered header row.
24. From Settings, Feedback, History, Progress, food editing, recipe editing, and What’s new, press the Android Back key. Confirm it returns through the expected app screen to Today; Back on Today may use normal Android app-exit behavior.
25. Open a long quick-add sheet and repeatedly scroll upward and downward. Confirm content scrolls without dragging or jumping the entire sheet.
26. Run an Open Food Facts search, export opted-in diagnostics, and confirm support schema v8 contains a `performance` entry with duration/outcome/result count/query length but not the search text.
27. Open `tools\Open Calorie Quick Review.cmd`, import the same review ZIP twice, and confirm edits survive deduplication. Add reviewer comments and corrections, then verify only accepted and explicitly promoted items appear in the generated JSON/Markdown implementation brief.
28. Repeat the same Open Food Facts search twice and confirm the second lookup works from the local cache. Confirm valid results appear in All foods automatically, open quick add directly, and never overwrite a personal food with the same barcode.
29. Export a review ZIP and verify `open-food-facts/cache.json` and its response files are present. Import it in the Windows tool, confirm a retained `source.zip` is created, and verify each barcode and opted-in shared food appears once in `shared-food-catalogue.json` without creating an ordinary review finding.
30. Open an Open Food Facts product in quick add, choose **Report incorrect product data**, select multiple problem categories, add a note, and submit. Confirm Settings and the export preview count the report. Import the ZIP in the Windows tool and verify exactly one `PRODUCT_DATA_ISSUE` finding links the categories, note, barcode, and matching cached response path. Unreported products must remain absent from the review queue.

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
- Full diary/profile backup and restore is available separately. Review-export ZIPs remain review artifacts, not backups, and contain only the explicitly selected catalogue/support data.
- Cloud backup is disabled because the database contains private nutrition and body data.
