package com.shadow.calorietracker.ui

import com.shadow.calorietracker.R

data class ReleaseNote(
    val versionName: String,
    val titleResource: Int,
    val itemResources: List<Int>,
)

object ReleaseNotes {
    val all = listOf(
        ReleaseNote(
            versionName = "0.2.0-alpha13",
            titleResource = R.string.release_alpha13_title,
            itemResources = listOf(
                R.string.release_alpha13_item_recipe_flow,
                R.string.release_alpha13_item_methods,
                R.string.release_alpha13_item_images,
                R.string.release_alpha13_item_install,
            ),
        ),
        ReleaseNote(
            versionName = "0.2.0-alpha12",
            titleResource = R.string.release_alpha12_title,
            itemResources = listOf(
                R.string.release_alpha12_item_snapshot,
                R.string.release_alpha12_item_picker,
                R.string.release_alpha12_item_learning,
            ),
        ),
        ReleaseNote(
            versionName = "0.2.0-alpha11",
            titleResource = R.string.release_alpha11_title,
            itemResources = listOf(
                R.string.release_alpha11_item_reporting,
                R.string.release_alpha11_item_evidence,
                R.string.release_alpha11_item_export,
            ),
        ),
        ReleaseNote(
            versionName = "0.2.0-alpha10",
            titleResource = R.string.release_alpha10_title,
            itemResources = listOf(
                R.string.release_alpha10_item_auto_catalogue,
                R.string.release_alpha10_item_collection,
                R.string.release_alpha10_item_review_boundary,
            ),
        ),
        ReleaseNote(
            versionName = "0.2.0-alpha09",
            titleResource = R.string.release_alpha09_title,
            itemResources = listOf(
                R.string.release_alpha09_item_crop,
                R.string.release_alpha09_item_liquid,
                R.string.release_alpha09_item_review,
                R.string.release_alpha09_item_off_cache,
            ),
        ),
        ReleaseNote(
            versionName = "0.2.0-alpha08",
            titleResource = R.string.release_alpha08_title,
            itemResources = listOf(
                R.string.release_alpha08_item_images,
                R.string.release_alpha08_item_review,
            ),
        ),
        ReleaseNote(
            versionName = "0.2.0-alpha07",
            titleResource = R.string.release_alpha07_title,
            itemResources = listOf(
                R.string.release_alpha07_item_back,
                R.string.release_alpha07_item_sheet,
                R.string.release_alpha07_item_performance,
                R.string.release_alpha07_item_review,
            ),
        ),
        ReleaseNote(
            versionName = "0.2.0-alpha06",
            titleResource = R.string.release_alpha06_title,
            itemResources = listOf(
                R.string.release_alpha06_item_bundle,
                R.string.release_alpha06_item_privacy,
                R.string.release_alpha06_item_identity,
            ),
        ),
        ReleaseNote(
            versionName = "0.2.0-alpha05",
            titleResource = R.string.release_alpha05_title,
            itemResources = listOf(
                R.string.release_alpha05_item_changelog,
                R.string.release_alpha05_item_prompt,
                R.string.release_alpha05_item_previous,
                R.string.release_alpha05_item_icon,
                R.string.release_alpha05_item_images,
                R.string.release_alpha05_item_scrolling,
            ),
        ),
        ReleaseNote(
            versionName = "0.2.0-alpha04",
            titleResource = R.string.release_alpha04_title,
            itemResources = listOf(
                R.string.release_alpha04_item_backup,
                R.string.release_alpha04_item_feedback,
                R.string.release_alpha04_item_header,
            ),
        ),
        ReleaseNote(
            versionName = "0.2.0-alpha03",
            titleResource = R.string.release_alpha03_title,
            itemResources = listOf(
                R.string.release_alpha03_item_breakdown,
                R.string.release_alpha03_item_diagnostics,
                R.string.release_alpha03_item_entry,
            ),
        ),
    )

    val current: ReleaseNote = all.first()

    fun shouldPrompt(
        onboardingComplete: Boolean,
        lastSeenVersionCode: Int,
        currentVersionCode: Int,
    ): Boolean = onboardingComplete && lastSeenVersionCode in 0 until currentVersionCode
}
