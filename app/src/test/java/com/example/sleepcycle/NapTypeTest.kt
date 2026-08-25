package com.example.sleepcycle

import com.example.sleepcycle.model.NapType
import com.example.sleepcycle.model.SLEEP_INERTIA_GUIDANCE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NapTypeTest {

    @Test
    fun presetsExposeTheFourSupportedNapChoices() {
        assertEquals(4, NapType.entries.size)
        assertEquals(10, NapType.TEN_MINUTES.durationMinutes)
        assertEquals(20, NapType.TWENTY_MINUTES.durationMinutes)
        assertEquals(20, NapType.COFFEE_NAP.durationMinutes)
        assertEquals(90, NapType.ONE_CYCLE_90_MINUTES.durationMinutes)
    }

    @Test
    fun coffeeNapIsTheOnlyPresetRequiringCoffeeGuidance() {
        assertTrue(NapType.COFFEE_NAP.isCoffeeNap)
        assertFalse(NapType.TEN_MINUTES.isCoffeeNap)
        assertFalse(NapType.TWENTY_MINUTES.isCoffeeNap)
        assertFalse(NapType.ONE_CYCLE_90_MINUTES.isCoffeeNap)
        assertTrue(NapType.COFFEE_NAP.description.contains("咖啡"))
    }

    @Test
    fun everyPresetHasScientificWakeUpGuidanceAndIndividualDifferenceDisclaimer() {
        NapType.entries.forEach { napType ->
            assertTrue(napType.label.isNotBlank())
            assertTrue(napType.description.isNotBlank())
            assertTrue(napType.wakeUpTip.isNotBlank())
        }
        assertTrue(SLEEP_INERTIA_GUIDANCE.contains("15–60 分钟"))
        assertTrue(SLEEP_INERTIA_GUIDANCE.contains("个体差异，仅供参考"))
    }
}
