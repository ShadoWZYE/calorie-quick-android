# Debug build changelog

This log identifies small fixes delivered to private test builds. It is not shown inside the app. Confirmed changes are consolidated into `CHANGELOG.md` and one user-facing release-note entry when a stable tester build is cut.

The development entries below were consolidated into stable tester release `0.2.1`. New development continues from `0.2.2-alpha1`; prerelease entries remain internal until their target stable version passes the release gate.

## 0.2.2-alpha1-dev

- Replaced square-only photo zooming with a rectangular crop whose four edges and four corners have enlarged independent drag targets kept clear of Android's system-gesture edges.
- Consolidated the user-visible `0.2.0` alpha-era history into one stable release entry.
- Made recipe-browser and quick-add swipe dismissal require a downward gesture that begins at the top of their inner content.
- Restored manual personal-food creation after localized searches while continuing to hide it during active online lookup processing.
- Stop Open Food Facts pagination when the server returns a short page or a fetched page contributes no new product identities.
- Preserve Today search text, catalogue filter, and online results while opening a food or editor and returning with Back.

## 0.2.0-alpha16-dev

- Reworked photo cropping into an edge-to-edge square workspace with precise slider/button zoom, 10× magnification, focal-point pinch behavior, and visible save failures.
- Added a persistent Open Food Facts progress panel, delayed and de-emphasized manual entry until lookup completion, and retained the last 30 result pages in memory so cleared repeat queries restore immediately.
- Publish Open Food Facts pages to Compose one card at a time with a full-frame pause between cards, and decode/publish only one remote thumbnail at a time, avoiding the composition/layout/image burst previously caused by a ten-card state replacement.
- Paginated Today, recipe, ingredient, and previous-day food searches at 10 visible results. Open Food Facts now uses server totals/raw page size to expose **Load more results**, even when incomplete records reduce the number of usable foods on a page, and shows an explicit end-of-results message otherwise.
- Open Food Facts results now render directly from lookup memory while normalized candidates are persisted on an I/O worker. Unused candidates remain hidden from the observed active catalogue until logged or used in a recipe, avoiding Room-driven result replacement and recomposition storms.
- Limited remote food-image download, decode, and publication to one preview at a time and added an explicit loading indicator while each queued image is pending.
- Removed the category fallback layer after a remote product photo loads, preventing stock imagery from showing around fitted Open Food Facts photos.
- Creating a recipe from an existing recipe now expands and scales the source recipe's ingredients instead of inserting the finished dish as one opaque ingredient.
- Replaced the Open Food Facts result-to-catalogue nested identity scan with memoized barcode/name sets and precompiled normalization expressions; emulator ANR traces identified this as the remaining UI-thread hotspot.
- Review-parser summaries now surface saved crash, freeze, and scan-diagnostic counts instead of hiding them inside the support JSON.
- New crash, freeze, network, and jank records retain the exact app build that produced them, so old failures are no longer mistaken for current-build regressions.
- Freeze monitoring now stops once fatal-crash handling begins, avoiding duplicate “freeze” reports caused only by Android terminating the failed process.
- Deferred nutrition-label and body-scale OCR initialization until an actual scan, preventing ML Kit setup failures from crashing the app during view-model creation.
- Replaced device-specific external photo editing with a full-screen in-app crop tool supporting pan, pinch zoom, reset, camera orientation, and an explicit saved preview.
- Fixed the liquid-measure editor crash caused by an unsaveable lazy-list key.
- Kept successful barcode results visible after they are cached into the local catalogue and hid the manual-add fallback while lookup is still running.
- Accepted and normalized decimal-looking package weights, moved common package names above the fields, and excluded archived recipes from review exports.
- Added an on-device cache for Open Food Facts thumbnails and changed food photos to fit without destructive cropping.
- Made the Windows review parser emit Romanian text correctly without requiring an external UTF-8 environment override.

## 0.2.0-alpha15-dev

- Added low-overhead per-screen jank aggregation and a non-debuggable `fieldTest` APK for trustworthy device performance testing without debugger-induced noise.

- Food search matches English and Romanian catalogue-category terms across Today, recipe ingredient search, and previous-day search.
- Build identity now distinguishes debug candidates from stable tester releases.
- Debug installations no longer display a new stable-release notification for every candidate build.

## 0.2.0-alpha14-dev

- Consolidated recipe browsing, ingredient search, batch scaling, photo preparation, sheet/keyboard navigation fixes, catalogue promotion, and provenance display for regression testing.

## 0.2.0-alpha13-dev

- Separated reusable recipe editing from cooking a prepared batch.
- Added recipe photos and common cooking-method controls.

## 0.2.0-alpha12-dev

- Preserved cooking methods and method-specific nutrition in recipe ingredient snapshots.
- Refined the cooking-method grid and learned preparation defaults.

## 0.2.0-alpha11-dev

- Added explicit incorrect-product reporting and export details.

## 0.2.0-alpha10-dev

- Added automatic local collection of valid Open Food Facts responses.

## 0.2.0-alpha09-dev

- Added image preparation, liquid-label improvements, practical liquid measures, and Open Food Facts cache reuse.

## 0.2.0-alpha08-dev

- Replaced malformed catalogue thumbnails.

## 0.2.0-alpha07-dev

- Iterated Android Back behavior and quick-add sheet scrolling.
