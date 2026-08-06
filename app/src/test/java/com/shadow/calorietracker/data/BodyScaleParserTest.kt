package com.shadow.calorietracker.data

import java.time.LocalDateTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BodyScaleParserTest {
    @Test
    fun `Fitdays comparison report selects current column and newest timestamp`() {
        val result = BodyScaleParser.parse(FITDAYS_REPORT, ZoneOffset.UTC)
        val measurement = requireNotNull(result.measurement)

        assertEquals(77.0, measurement.weightKg, 0.001)
        assertEquals(24.3, measurement.bmi!!, 0.001)
        assertEquals(17.0, measurement.bodyFatPercent!!, 0.001)
        assertEquals(13.1, measurement.fatMassKg!!, 0.001)
        assertEquals(63.9, measurement.fatFreeMassKg!!, 0.001)
        assertEquals(60.8, measurement.muscleMassKg!!, 0.001)
        assertEquals(78.9, measurement.musclePercent!!, 0.001)
        assertEquals(53.6, measurement.skeletalMusclePercent!!, 0.001)
        assertEquals(3.2, measurement.boneMassKg!!, 0.001)
        assertEquals(14.6, measurement.proteinMassKg!!, 0.001)
        assertEquals(18.9, measurement.proteinPercent!!, 0.001)
        assertEquals(46.2, measurement.waterMassKg!!, 0.001)
        assertEquals(60.0, measurement.bodyWaterPercent!!, 0.001)
        assertEquals(14.8, measurement.subcutaneousFatPercent!!, 0.001)
        assertEquals(7.3, measurement.visceralFat!!, 0.001)
        assertEquals(1_751, measurement.bmrCalories)
        assertEquals(26, measurement.bodyAge)
        assertEquals(
            LocalDateTime.of(2026, 8, 4, 19, 31).toInstant(ZoneOffset.UTC).toEpochMilli(),
            measurement.measuredAtEpochMillis,
        )
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun `missing weight prevents unsafe prefill`() {
        val result = BodyScaleParser.parse("BMI 23.2 24.3 1.1", ZoneOffset.UTC)

        assertNull(result.measurement)
        assertTrue(BodyScaleWarning.WEIGHT_MISSING in result.warnings)
    }

    @Test
    fun `single-column manual style text remains usable with review warning`() {
        val result = BodyScaleParser.parse("Weight 71.4 kg\nBody Fat 18.2%", ZoneOffset.UTC)

        assertEquals(71.4, result.measurement!!.weightKg, 0.001)
        assertEquals(18.2, result.measurement!!.bodyFatPercent!!, 0.001)
        assertTrue(BodyScaleWarning.DATE_MISSING in result.warnings)
        assertTrue(BodyScaleWarning.LIMITED_METRICS in result.warnings)
        assertFalse(BodyScaleWarning.WEIGHT_MISSING in result.warnings)
    }

    @Test
    fun `split comparison values stop before the next metric label`() {
        val result = BodyScaleParser.parse(
            "Weight\n73.35 kg\n77.00 kg\n3.65 kg\nBMI\n23.2\n24.3\n1.1",
            ZoneOffset.UTC,
        )

        assertEquals(77.0, result.measurement!!.weightKg, 0.001)
        assertEquals(24.3, result.measurement!!.bmi!!, 0.001)
    }

    private companion object {
        val FITDAYS_REPORT = """
            H. Georgian
            19:31 Aug.4,2026
            Weight 73.35kg 77.00kg 3.65kg
            BMI 23.2 24.3 1.1
            Body Fat 15.2% 17.0% 1.9%
            Fat Mass 11.1kg 13.1kg 2.0kg
            Fat-free Body Weight 62.2kg 63.9kg 1.7kg
            Muscle Mass 59.1kg 60.8kg 1.7kg
            Muscle Rate 80.6% 78.9% 1.7%
            Skeletal Muscle 54.8% 53.6% 1.2%
            Bone Mass 3.1kg 3.2kg 0.1kg
            Protein Mass 14.2kg 14.6kg 0.4kg
            Protein 19.3% 18.9% 0.4%
            Water Weight 44.9kg 46.2kg 1.3kg
            Body Water 61.2% 60.0% 1.2%
            Subcutaneous fat 13.3% 14.8% 1.5%
            Visceral Fat 6.2 7.3 1.1
            BMR 1713kcal 1751kcal 38.0kcal
            Body age 23 26 3.0
        """.trimIndent()
    }
}
