package com.example.sleepcycle

import com.example.sleepcycle.data.SleepRecordEntity
import com.example.sleepcycle.data.SleepSettingsEntity
import com.example.sleepcycle.data.toEntity
import com.example.sleepcycle.data.toRecord
import com.example.sleepcycle.data.toSettings
import com.example.sleepcycle.model.SleepRecord
import com.example.sleepcycle.model.SleepSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        assertEquals(SleepRecordEntity::class.java.simpleName, "SleepRecordEntity")
        assertEquals(SleepSettingsEntity::class.java.simpleName, "SleepSettingsEntity")
    }

    @Test
    fun migrationVersionAndEntitiesRemainExplicit() {
        val migration = com.example.sleepcycle.data.SleepCycleDatabase.MIGRATION_1_2
        assertEquals(1, migration.startVersion)
        assertEquals(2, migration.endVersion)
    }

    @Test
    fun migrationSqlDefinesExpectedTablesColumnsAndDefaultRow() {
        val recordSql = com.example.sleepcycle.data.SleepCycleDatabase.CREATE_SLEEP_RECORD_SQL
        val settingsSql = com.example.sleepcycle.data.SleepCycleDatabase.CREATE_SLEEP_SETTINGS_SQL
        val defaultSql = com.example.sleepcycle.data.SleepCycleDatabase.INSERT_DEFAULT_SLEEP_SETTINGS_SQL
        assertTrue(recordSql.contains("CREATE TABLE IF NOT EXISTS sleep_record"))
        assertTrue(recordSql.contains("dateEpochDay INTEGER NOT NULL"))
        assertTrue(recordSql.contains("bedtimeMinutes INTEGER NOT NULL"))
        assertTrue(recordSql.contains("wakeTimeMinutes INTEGER NOT NULL"))
        assertTrue(recordSql.contains("primarySleepMinutes INTEGER NOT NULL"))
        assertTrue(recordSql.contains("napMinutes INTEGER NOT NULL"))
        assertTrue(recordSql.contains("PRIMARY KEY(dateEpochDay)"))
        assertTrue(settingsSql.contains("CREATE TABLE IF NOT EXISTS sleep_settings"))
        assertTrue(settingsSql.contains("settingsId INTEGER NOT NULL"))
        assertTrue(settingsSql.contains("targetMinutes INTEGER NOT NULL"))
        assertTrue(settingsSql.contains("PRIMARY KEY(settingsId)"))
        assertEquals("INSERT OR IGNORE INTO sleep_settings (settingsId, targetMinutes) VALUES (1, 480)", defaultSql)
    }
}
