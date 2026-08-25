package com.example.sleepcycle

import com.example.sleepcycle.data.ChronotypeProfileEntity
import com.example.sleepcycle.data.toEntity
import com.example.sleepcycle.data.toProfile
import com.example.sleepcycle.model.ChronotypeAnswers
import com.example.sleepcycle.model.ChronotypeCalculator
import com.example.sleepcycle.model.ChronotypeCategory
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class ChronotypeStorageTest {
    @Test
    fun entityMappingPreservesAnswersAndResult() {
        val profile = ChronotypeCalculator.calculate(
            ChronotypeAnswers(
                LocalTime.of(22, 0), LocalTime.of(23, 0), LocalTime.of(7, 0),
                LocalTime.of(23, 30), LocalTime.of(0, 0), LocalTime.of(8, 0), true
            ),
            nowMillis = 123L
        )
        val restored = profile.toEntity().toProfile()
        assertEquals(profile, restored)
        assertEquals(ChronotypeCategory.MORNING, restored.category)
    }

    @Test
    fun entityUsesSingleProfileIdAndNullableFields() {
        val entity = ChronotypeProfileEntity(
            workdayBedtimeMinutes = null,
            workdaySleepTimeMinutes = null,
            workdayWakeTimeMinutes = null,
            freeDayBedtimeMinutes = null,
            freeDaySleepTimeMinutes = null,
            freeDayWakeTimeMinutes = null,
            needsAlarm = null,
            midpointMinutes = null,
            category = ChronotypeCategory.INCOMPLETE.name,
            updatedAtEpochMillis = 9L
        )
        assertEquals(ChronotypeProfileEntity.SINGLE_PROFILE_ID, entity.profileId)
        assertEquals(null, entity.toProfile().midpointMinutes)
        assertEquals(ChronotypeCategory.INCOMPLETE, entity.toProfile().category)
    }
}
