# Local review inbox

The Calorie Quick review inbox is a private Windows tool for turning field-test review ZIPs into deliberate implementation briefs. It does not upload bundles, create online issues, or automatically approve suggestions.

## Open it

Double-click:

```text
tools\Open Calorie Quick Review.cmd
```

The tool uses Python's built-in Windows UI and stores its private working data under:

```text
%LOCALAPPDATA%\CalorieQuick\ReviewInbox
```

This directory is outside the Git repository. It can contain feedback text and sanitized copies of opted-in images, so do not publish or sync it unintentionally.

## Import and triage

1. Select **Import review ZIP**. The existing safe parser validates paths, sizes, schemas, hashes, and image privacy before anything is added.
2. Re-importing the same archive hash opens the existing review and preserves all reviewer edits.
3. The inbox creates separate items for feedback messages, catalogue findings, failed scans, and sufficiently slow or failed performance operations.
4. Automated classification and severity are suggestions only. Review and edit:
   - classification and severity;
   - workflow status and decision;
   - decision rationale;
   - target version and linked issue/commit;
   - reviewer comments;
   - corrections or clarified expected behavior;
   - implementation notes and acceptance checks.
5. Use **Add manual item** for conclusions that do not map cleanly to one imported record.

## Decisions

- `ACCEPT_NEXT`: suitable for the next focused implementation batch.
- `ACCEPT_BACKLOG`: accepted, but not scheduled immediately.
- `NEEDS_REPRODUCTION`: evidence is insufficient to change the product safely.
- `NEEDS_PRODUCT_DECISION`: valid request whose behavior or scope is unresolved.
- `DEFERRED`: useful, but prerequisites or current priorities prevent work.
- `DECLINED`: intentionally not planned; record the reason.
- `DUPLICATE`: represented by another review item.
- `RESOLVED`: already addressed and verified.

## Promote toward implementation

An item reaches the generated implementation brief only when both conditions are true:

1. its decision is `ACCEPT_NEXT` or `ACCEPT_BACKLOG`; and
2. **Promote to the next generated implementation brief** is checked.

**Export implementation brief** writes matching JSON and Markdown files. They preserve the original evidence, reviewer additions, corrections, target, and acceptance notes. Deferred, undecided, declined, and unselected items are excluded even if parsing suggested a high severity.

The brief is still a handoff artifact—not an automatic code change or external issue submission.
