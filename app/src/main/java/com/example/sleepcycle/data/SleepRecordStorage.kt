package com.example.sleepcycle.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.sleepcycle.model.SleepRecord
import com.example.sleepcycle.model.SleepSettings
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "sleep_record", primaryKeys = ["dateEpochDay"])
data class SleepRecordEntity(
    val dateEpochDay: Long,
    val bedtimeMinutes: Int,
    val wakeTimeMinutes: Int,
    val primarySleepMinutes: Int,
    val napMinutes: Int
)

fun SleepRecord.toEntity(): SleepRecordEntity = SleepRecordEntity(
    dateEpochDay = date.toEpochDay(),
    bedtimeMinutes = bedtime.toMinuteOfDay(),
    wakeTimeMinutes = wakeTime.toMinuteOfDay(),
    primarySleepMinutes = primarySleepMinutes,
    napMinutes = napMinutes
)

fun SleepRecordEntity.toRecord(): SleepRecord = SleepRecord(
    date = LocalDate.ofEpochDay(dateEpochDay),
    bedtime = bedtimeMinutes.toLocalTime(),
    wakeTime = wakeTimeMinutes.toLocalTime(),
    primarySleepMinutes = primarySleepMinutes,
    napMinutes = napMinutes
)

@Entity(tableName = "sleep_settings")
data class SleepSettingsEntity(
    @androidx.room.PrimaryKey val settingsId: Int = SINGLE_SETTINGS_ID,
    val targetMinutes: Int = SleepSettings().targetMinutes
) {
    companion object {
        const val SINGLE_SETTINGS_ID = 1
    }
}

fun SleepSettings.toEntity(): SleepSettingsEntity = SleepSettingsEntity(targetMinutes = targetMinutes)
fun SleepSettingsEntity.toSettings(): SleepSettings = SleepSettings(targetMinutes)

@Dao
interface SleepRecordDao {
    @Query("SELECT * FROM sleep_record ORDER BY dateEpochDay DESC")
    suspend fun getAll(): List<SleepRecordEntity>

    @Query("SELECT * FROM sleep_record WHERE dateEpochDay = :dateEpochDay")
    suspend fun getByDate(dateEpochDay: Long): SleepRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: SleepRecordEntity)

    @Query("DELETE FROM sleep_record WHERE dateEpochDay = :dateEpochDay")
    suspend fun deleteByDate(dateEpochDay: Long)
}

@Dao
interface SleepSettingsDao {
    @Query("SELECT * FROM sleep_settings WHERE settingsId = 1")
    suspend fun get(): SleepSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(settings: SleepSettingsEntity)
}

interface SleepRecordRepository {
    suspend fun loadRecords(): List<SleepRecord>
    suspend fun getRecord(date: LocalDate): SleepRecord?
    suspend fun saveRecord(record: SleepRecord)
    suspend fun deleteRecord(date: LocalDate)
    suspend fun loadSettings(): SleepSettings
    suspend fun saveSettings(settings: SleepSettings)
}

class RoomSleepRecordRepository(
    private val recordDao: SleepRecordDao,
    private val settingsDao: SleepSettingsDao
) : SleepRecordRepository {
    override suspend fun loadRecords(): List<SleepRecord> = recordDao.getAll().map(SleepRecordEntity::toRecord)
    override suspend fun getRecord(date: LocalDate): SleepRecord? = recordDao.getByDate(date.toEpochDay())?.toRecord()
    override suspend fun saveRecord(record: SleepRecord) = recordDao.upsert(record.toEntity())
    override suspend fun deleteRecord(date: LocalDate) = recordDao.deleteByDate(date.toEpochDay())
    override suspend fun loadSettings(): SleepSettings = settingsDao.get()?.toSettings() ?: SleepSettings()
    override suspend fun saveSettings(settings: SleepSettings) = settingsDao.save(settings.toEntity())
}

class InMemorySleepRecordRepository(
    initialRecords: List<SleepRecord> = emptyList(),
    initialSettings: SleepSettings = SleepSettings(),
    private val failOnRead: Boolean = false,
    private val failOnWrite: Boolean = false
) : SleepRecordRepository {
    private val records = initialRecords.associateBy { it.date }.toMutableMap()
    private var settings = initialSettings

    override suspend fun loadRecords(): List<SleepRecord> {
        if (failOnRead) throw IllegalStateException("睡眠记录读取失败")
        return records.values.sortedByDescending { it.date }
    }

    override suspend fun getRecord(date: LocalDate): SleepRecord? {
        if (failOnRead) throw IllegalStateException("睡眠记录读取失败")
        return records[date]
    }

    override suspend fun saveRecord(record: SleepRecord) {
        if (failOnWrite) throw IllegalStateException("睡眠记录保存失败")
        records[record.date] = record
    }

    override suspend fun deleteRecord(date: LocalDate) {
        if (failOnWrite) throw IllegalStateException("睡眠记录删除失败")
        records.remove(date)
    }

    override suspend fun loadSettings(): SleepSettings {
        if (failOnRead) throw IllegalStateException("睡眠设置读取失败")
        return settings
    }

    override suspend fun saveSettings(settings: SleepSettings) {
        if (failOnWrite) throw IllegalStateException("睡眠设置保存失败")
        this.settings = settings
    }
}

private fun LocalTime.toMinuteOfDay(): Int = hour * 60 + minute
private fun Int.toLocalTime(): LocalTime = LocalTime.of(this / 60, this % 60)
