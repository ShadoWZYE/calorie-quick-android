# Product brief

## North-star outcome

A returning user can log a frequent food in under five seconds and a new packaged food in under fifteen seconds. The app should launch into an immediately useful, offline Today screen and never require account creation for the core loop.

## Initial audience

Adults tracking calories and macronutrients for general wellness, starting with English and Romanian speakers. This is not a diagnostic or treatment product. Estimates and recommendations must show their assumptions and advise users with medical, pregnancy, eating-disorder, or youth-specific needs to consult a qualified professional.

## Core loop

1. Open Today.
2. Search or tap a frequent/recent food.
3. Confirm a remembered serving or enter grams.
4. See calories and macros update immediately.
5. Undo or edit without navigating away.

## MVP scope

- Optional onboarding for units, age/date of birth, height, weight, activity level, goal, and preferred weekly rate.
- Transparent resting-energy estimate (Mifflin–St Jeor for eligible adults) plus configurable activity/goal adjustment.
- Today log with meals, copy-from-yesterday, recent/frequent foods, serving memory, edit, undo, and offline persistence.
- Personal foods and recipes with calories, protein, carbohydrates, fat, fibre, sodium, and optional micronutrients.
- Search that ranks exact barcode, personal foods, frequent foods, recent foods, then cached catalogue results.
- English and Romanian UI, metric-first with configurable units.
- User-controlled reminders and calm threshold warnings.
- Export/delete local data and a clear data provenance label on imported foods.

## Useful additions

- Home-screen quick-add widget and Android app shortcuts.
- Favourite portions ("my bowl", "one scoop") and remembered last amount.
- Templates for recurring meals and copy day/meal.
- Streak-free consistency insights so missed days are not punitive.
- Offline barcode cache and a visible "verified / community / user-entered" confidence marker.
- Data-quality checks: calories versus macro-derived energy, implausible serving sizes, and missing units.
- Accessibility: large tap targets, dynamic type, TalkBack labels, colour-independent status, and left/right-handed quick actions.

## Product metrics

- Median warm-launch-to-saved-entry time.
- Taps and keystrokes per saved entry, split between frequent, searched, and new foods.
- Search success in the first three results and zero-result rate by locale.
- Correction/deletion rate after import (a proxy for catalogue quality).
- Offline success rate and crash-free sessions.
- Reminder opt-in, dismissal, and disable rates; never optimize for notification volume.

## Open product decisions

- Final public name, package/application ID, visual identity, and publisher.
- Whether accounts/sync are optional forever or required for community contributions.
- How conservative target guardrails should be and which clinical reviewer signs them off.
- The first central catalogue regions after Romania and the source-merging policy.
- Monetization and whether it changes catalogue, sync, or image-retention design.

