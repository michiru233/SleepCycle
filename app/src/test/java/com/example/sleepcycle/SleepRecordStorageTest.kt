package com.example.sleepcycle

import com.example.sleepcycle.data.SleepRecordEntity
import com.example.sleepcycle.data.SleepSettingsEntity
import com.example.sleepcycle.data.toEntity
import com.example.sleepcycle.data.toRecord
import com.example.sleepcycle.data.toSettings
import com.example.sleepcycle.model.SleepRecord
import com.example.sleepcycle.model.SleepSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class SleepRecordStorageTest {
    @Test
    fun mappingPreservesCrossMidnightRecord() {
        val record = SleepRecord(LocalDate.of(2026, 8, 20), LocalTime.of(23, 30), LocalTime.of(7, 0), 430, 20)
        assertEquals(record, record.toEntity().toRecord())
        assertEquals(450, record.clockDurationMinutes)
        assertEquals(195, record.midpointMinutes)
    }

    @Test
    fun settingsMappingAndValidation() {
        assertEquals(SleepSettings(450), SleepSettings(450).toEntity().toSettings())
        assertThrows(IllegalArgumentException::class.java) { SleepSettings(455) }
        assertThrows(IllegalArgumentException::class.java) { SleepSettings(330) }
    }

    @Test
    fun migrationCreatesNewTablesWithoutDroppingChronotype() {
        val sql = com.example.sleepcycle.data.SleepCycleDatabase.MIGRATION_1_2
        assertEquals(1, sql.startVersion)
        assertEquals(2, sql.endVersion)
        assertEquals(SleepRecordEntity::class.java.simpleName, "SleepRecordEntity")
        assertEquals(SleepSettingsEntity::class.java.simpleName, "SleepSettingsEntity")
    }
}
