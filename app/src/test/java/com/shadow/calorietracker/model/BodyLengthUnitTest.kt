package com.shadow.calorietracker.model

import org.junit.Assert.assertEquals
import org.junit.Test

class BodyLengthUnitTest {
    @Test
    fun convertsWithoutChangingCanonicalCentimeters() {
        assertEquals(814.0, BodyLengthUnit.MILLIMETERS.fromCentimeters(81.4), 0.0001)
        assertEquals(81.4, BodyLengthUnit.CENTIMETERS.fromCentimeters(81.4), 0.0001)
        assertEquals(32.047, BodyLengthUnit.INCHES.fromCentimeters(81.4), 0.001)

        assertEquals(81.4, BodyLengthUnit.MILLIMETERS.toCentimeters(814.0), 0.0001)
        assertEquals(81.28, BodyLengthUnit.INCHES.toCentimeters(32.0), 0.0001)
    }
}
