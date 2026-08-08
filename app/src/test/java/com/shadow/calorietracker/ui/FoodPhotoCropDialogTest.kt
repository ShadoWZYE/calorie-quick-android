package com.shadow.calorietracker.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class FoodPhotoCropDialogTest {
    @Test
    fun independentEdgesProduceExactRectangularCrop() {
        val result = calculatePhotoCropRect(
            bitmapWidth = 1_200,
            bitmapHeight = 600,
            bounds = PhotoCropBounds(left = 0.1f, top = 0.2f, right = 0.8f, bottom = 0.9f),
        )

        assertEquals(PhotoCropRect(left = 120, top = 120, width = 840, height = 420), result)
    }

    @Test
    fun draggingOneEdgeDoesNotMoveTheOtherThree() {
        val initial = PhotoCropBounds(left = 0.1f, top = 0.2f, right = 0.9f, bottom = 0.8f)
        val result = dragPhotoCropBounds(
            initial,
            PhotoCropHandle.LEFT,
            deltaX = 0.15f,
            deltaY = 0.4f,
            minimumWidth = 0.05f,
            minimumHeight = 0.05f,
        )

        assertEquals(PhotoCropBounds(left = 0.25f, top = 0.2f, right = 0.9f, bottom = 0.8f), result)
    }

    @Test
    fun movingSelectionPreservesItsSizeAtImageBoundary() {
        val result = dragPhotoCropBounds(
            PhotoCropBounds(left = 0.2f, top = 0.2f, right = 0.7f, bottom = 0.6f),
            PhotoCropHandle.MOVE,
            deltaX = 0.8f,
            deltaY = -0.5f,
            minimumWidth = 0.05f,
            minimumHeight = 0.05f,
        )

        assertEquals(0.5f, result.left, 0.0001f)
        assertEquals(0f, result.top, 0.0001f)
        assertEquals(1f, result.right, 0.0001f)
        assertEquals(0.4f, result.bottom, 0.0001f)
    }
}
