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
