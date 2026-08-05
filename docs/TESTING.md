# Testing guide

Use the **Calorie Quick Test** Desktop or Start Menu shortcut for each test
session. It builds, installs, and opens the latest code in the API 37 emulator.

## Current smoke test

1. On a fresh install, complete the short profile setup and confirm the estimated target changes with activity and goal.
2. Switch to **Română** during setup, then back to English from the gear-shaped **Settings** screen.
3. Scroll the recommended-food list and confirm text is readable and cards respond.
4. Search for an English name such as `banana` and a Romanian name such as
   `banană`; both should find the same food.
5. Open Banana and try **1 medium banana**, **1 small banana**, and an exact gram value.
6. Open Whole egg, enter `3`, choose **large egg** from the unit suffix, and add it. Reopen Whole egg and confirm `3 × large egg · 150 g` appears as a quick-pick chip while the editable field starts at `1 large egg`.
7. Add a custom gram amount, reopen the food, and confirm that amount appears as its own gram quick-pick chip while choosing `g` starts at `100 g`.
8. Confirm calories and all three macros update immediately.
9. Add several foods, scroll to today's entries, and remove one.
10. Add the same food twice within two minutes. Confirm it becomes one timestamped entry whose weight and nutrition are the sum of both additions.
11. Confirm a macro turns red after exceeding its target. Confirm **Recommended** avoids foods rich in that macro, then tap the summary card and inspect the macro breakdown.
12. Rotate the emulator. Confirm the profile, entries, and total remain intact and the screen still fits.
13. Close and relaunch the app. Confirm the same profile and today's entries remain.

## Feedback to capture

- Time from launch to the first saved food.
- Any step that needed explanation or felt like too many taps.
- Search terms that did not produce the expected food.
- Serving sizes or units that felt awkward.
- Text that is unclear, clipped, too small, or poorly translated.
- Crashes, freezes, visual jumps, or totals that appear incorrect.

Screenshots plus the exact action immediately before a problem are ideal.

## Known prototype limitations

- The calorie target is formula-derived; manual calorie/macro overrides are not available yet.
- Diary entries can be added or deleted but not edited, grouped into meals, or undone. Rapid repeat additions of the same food merge within two minutes; later additions remain separate.
- The local catalogue is persistent but currently contains only bundled sample foods; users cannot create foods yet.
- Reports, reminders, export/delete controls, and broader migration coverage remain for the private-alpha milestone.
