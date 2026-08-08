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
            versionName = "0.2.2",
            titleResource = R.string.release_022_title,
            itemResources = listOf(
                R.string.release_022_item_crop,
                R.string.release_022_item_sheets,
                R.string.release_022_item_search,
                R.string.release_022_item_history,
            ),
        ),
        ReleaseNote(
            versionName = "0.2.1",
            titleResource = R.string.release_021_title,
            itemResources = listOf(
                R.string.release_021_item_recipes,
                R.string.release_021_item_foods,
                R.string.release_021_item_capture,
                R.string.release_021_item_history,
                R.string.release_021_item_reliability,
            ),
        ),
        ReleaseNote(
            versionName = "0.2.0",
            titleResource = R.string.release_020_title,
            itemResources = listOf(
                R.string.release_020_item_tracking,
                R.string.release_020_item_catalogue,
                R.string.release_020_item_recipes,
                R.string.release_020_item_capture,
                R.string.release_020_item_portability,
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
