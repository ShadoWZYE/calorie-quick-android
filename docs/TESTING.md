# Testing guide

Use the **Calorie Quick Test** Desktop or Start Menu shortcut for each test
session. It builds, installs, and opens the latest code in the API 37 emulator.

## Current smoke test

1. Confirm the app opens directly to **Today** without a setup/login barrier.
2. Scroll the frequent-food list and confirm text is readable and cards respond.
3. Search for an English name such as `banana` and a Romanian name such as
   `banană`; both should find the same food.
4. Open a food, try each serving preset, enter a custom gram value, and add it.
5. Confirm calories and all three macros update immediately.
6. Add several foods, scroll to today's entries, and remove one.
7. Rotate the emulator and note any layout or state problems.
8. Change the emulator language to Romanian, relaunch, and inspect translations.

## Feedback to capture

- Time from launch to the first saved food.
- Any step that needed explanation or felt like too many taps.
- Search terms that did not produce the expected food.
- Serving sizes or units that felt awkward.
- Text that is unclear, clipped, too small, or poorly translated.
- Crashes, freezes, visual jumps, or totals that appear incorrect.

Screenshots plus the exact action immediately before a problem are ideal.

## Known prototype limitations

- Entries are currently in memory and reset when the app process is recreated.
- The daily goal and macro targets are hard-coded demo values.
- Only the Today screen and quick-add sheet exist.
- Sample foods are bundled; there is no persistent personal catalogue yet.

