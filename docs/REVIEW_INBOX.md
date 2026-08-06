# Local review inbox

The Calorie Quick review inbox is a private Windows tool for turning field-test review ZIPs into deliberate implementation briefs. It does not upload bundles, create online issues, or automatically approve suggestions.

## Open it

Double-click:

```text
tools\Open Calorie Quick Review.cmd
```

The launcher uses the windowless Python runtime when available. Opening it again focuses the existing review window instead of creating another Python process or command window.

The tool uses Python's built-in Windows UI and stores its private working data under:

```text
%LOCALAPPDATA%\CalorieQuick\ReviewInbox
```

This directory is outside the Git repository. It can contain feedback text and sanitized copies of opted-in images, so do not publish or sync it unintentionally.

## Import and triage

1. Select **Import review ZIP**. The existing safe parser validates paths, sizes, schemas, hashes, and image privacy before anything is added.
2. Re-importing the same archive hash opens the existing review and preserves all reviewer edits.
3. The inbox creates separate items for feedback messages, explicitly reported incorrect products, flagged catalogue findings, failed scans, and sufficiently slow or failed performance operations. Ordinary Open Food Facts products are collected automatically and do not become findings.
4. Read the original evidence and the separate read-only Codex analysis, then choose **Not decided**, **Next**, **Later**, **Needs testing**, or **Done**. Add a note or correction when useful.
5. **Next** items are included in the implementation brief automatically. Classification, severity, target/reference, structured corrections, and implementation notes remain available under **Advanced**.
6. Use **Add a review item** for conclusions that do not map cleanly to one imported record.

Items listed in `tools/review_implementation_status.json` are reconciled whenever the inbox opens. Implemented work moves from **Next** to **Needs testing** automatically; a finding you have already marked **Done** is never moved backward. The linked alpha version is filled in automatically as well.

The right-hand review panel scrolls independently with the mouse wheel anywhere under the pointer. Use **Previous** and **Next** at its top to work through the currently filtered findings without returning to the list each time. The numbered header summarizes the intended flow, and **How this works** provides the same guidance inside the tool.

## Simple decisions

- **Not decided**: no decision yet.
- **Next**: include in the next focused implementation brief.
- **Later**: accepted, but not scheduled now.
- **Needs testing**: gather clearer reproduction evidence first.
- **Done**: fixed and verified.

## Create the next implementation brief

**Create implementation brief** writes matching JSON and Markdown files under the private local inbox by default. Only **Next** items are included. The files preserve the original evidence, Codex analysis, reviewer additions, corrections, target, and acceptance notes.

The brief is a local working artifact—not an automatic code change or external issue submission.

Every successfully imported archive is retained as `bundles/<archive hash>/source.zip`. Opted-in personal foods and full Open Food Facts products are merged automatically into `%LOCALAPPDATA%\CalorieQuick\ReviewInbox\shared-food-catalogue.json`, deduplicated by barcode when available. Its records retain source-bundle and internal-response paths, allowing the complete evidence to be recovered later.

This shared catalogue is an automated local staging database, not a manual decision queue. Only products explicitly flagged as wrong create findings for deliberate review. Each such finding includes the selected problem categories, the tester's optional note, and the matching raw Open Food Facts response paths retained inside the source ZIP.
