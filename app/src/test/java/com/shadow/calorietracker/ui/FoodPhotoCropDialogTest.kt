package com.shadow.calorietracker.ui

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Test

class FoodPhotoCropDialogTest {
    @Test
    fun wideImageStartsWithCenteredSquareCrop() {
        val result = calculatePhotoCropRect(1_200, 600, 600, zoom = 2f, offset = Offset.Zero)

        assertEquals(PhotoCropRect(left = 300, top = 0, size = 600), result)
    }

    @Test
    fun horizontalPanMovesCropWithinBitmapBounds() {
        val result = calculatePhotoCropRect(1_200, 600, 600, zoom = 2f, offset = Offset(-300f, 0f))

        assertEquals(PhotoCropRect(left = 600, top = 0, size = 600), result)
    }
}
