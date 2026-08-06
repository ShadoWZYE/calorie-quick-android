# App update plan

## Private field testing

- Give every tester build a higher `versionCode` and a readable pre-release `versionName`.
- Build and sign every APK with the same key, application ID, and signing lineage.
- Install updates with `adb install -r app/build/outputs/apk/debug/app-debug.apk`; never uninstall first.
- Run all Room migrations in normal startup and include migration coverage in the release gate. An update must be rejected if it cannot preserve the existing database.
- Put the APK, version, commit, checksum, migration notes, and a short changelog together in each handoff.
- Ask testers to export their review bundle before a risky schema change. This is diagnostic data, not a full diary backup.

Debug-signed APKs are suitable only while one trusted machine produces every field-test build. Moving to another build machine or distribution service requires retaining that debug key or installing a separately signed app, which Android treats as a different trust lineage.

## Broader private alpha

Use a private release signing key held outside the repository and distribute through a closed testing channel such as Google Play Internal/Closed Testing. That provides authenticated, incremental updates and rollback visibility while keeping the app unavailable to the public. Use Play App Signing for the distribution key and retain a separate upload key.

Before switching existing testers from debug to release signing, choose one of these explicit migrations:

1. Keep the debug track until full encrypted backup/restore exists, then export, install the release-signed app, and restore.
2. Start the release-signed package as a separate application ID during alpha and migrate test data deliberately.

Android will not install an APK signed by a different key over the existing package. Do not work around this with an uninstall because that erases local data.

## In-app update experience

- During private testing, show the installed version in Settings and link to release notes; installation remains user initiated.
- With Play closed testing, use Play's normal automatic updates first. Add an in-app flexible-update prompt only after real testing shows it is needed.
- Reserve immediate/forced updates for a proven data-corruption or security issue. Nutrition catalogue refreshes should be versioned data updates and should not require a forced app upgrade.
- Never download and execute arbitrary APKs from the app. Non-Play distribution needs signed artifacts, checksum verification, and explicit user consent.

## Release checklist

1. Increment `versionCode` and update `versionName`.
2. Add and test every database migration from the last distributed schema.
3. Run the full release gate and a preserve-data upgrade smoke test on a real phone.
4. Verify review exports, photos, recipes, diary history, and settings before and after update.
5. Publish the signed artifact, SHA-256 checksum, commit, migration notes, and changelog.
6. Keep the previous known-good artifact available; database downgrades are unsupported unless a deliberate reverse migration exists.
