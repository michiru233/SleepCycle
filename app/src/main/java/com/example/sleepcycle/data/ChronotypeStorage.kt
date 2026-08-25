package com.example.sleepcycle.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.sleepcycle.model.ChronotypeAnswers
import com.example.sleepcycle.model.ChronotypeCategory
import com.example.sleepcycle.model.ChronotypeProfile
import java.time.LocalTime

@Entity(tableName = "chronotype_profile")
data class ChronotypeProfileEntity(
    @androidx.room.PrimaryKey val profileId: Int = SINGLE_PROFILE_ID,
    val workdayBedtimeMinutes: Int?,
    val workdaySleepTimeMinutes: Int?,
    val workdayWakeTimeMinutes: Int?,
    val freeDayBedtimeMinutes: Int?,
    val freeDaySleepTimeMinutes: Int?,
    val freeDayWakeTimeMinutes: Int?,
    val needsAlarm: Boolean?,
    val midpointMinutes: Int?,
    val category: String,
    val updatedAtEpochMillis: Long
) {
    companion object {
        const val SINGLE_PROFILE_ID = 1
    }
}

fun ChronotypeProfile.toEntity(): ChronotypeProfileEntity = ChronotypeProfileEntity(
    workdayBedtimeMinutes = answers.workdayBedtime?.toMinuteOfDay(),
    workdaySleepTimeMinutes = answers.workdaySleepTime?.toMinuteOfDay(),
    workdayWakeTimeMinutes = answers.workdayWakeTime?.toMinuteOfDay(),
    freeDayBedtimeMinutes = answers.freeDayBedtime?.toMinuteOfDay(),
    freeDaySleepTimeMinutes = answers.freeDaySleepTime?.toMinuteOfDay(),
    freeDayWakeTimeMinutes = answers.freeDayWakeTime?.toMinuteOfDay(),
    needsAlarm = answers.needsAlarm,
    midpointMinutes = midpointMinutes,
    category = category.name,
    updatedAtEpochMillis = updatedAtEpochMillis
)

fun ChronotypeProfileEntity.toProfile(): ChronotypeProfile = ChronotypeProfile(
    answers = ChronotypeAnswers(
        workdayBedtime = workdayBedtimeMinutes?.toLocalTime(),
        workdaySleepTime = workdaySleepTimeMinutes?.toLocalTime(),
        workdayWakeTime = workdayWakeTimeMinutes?.toLocalTime(),
        freeDayBedtime = freeDayBedtimeMinutes?.toLocalTime(),
        freeDaySleepTime = freeDaySleepTimeMinutes?.toLocalTime(),
        freeDayWakeTime = freeDayWakeTimeMinutes?.toLocalTime(),
        needsAlarm = needsAlarm
    ),
    midpointMinutes = midpointMinutes,
    category = ChronotypeCategory.entries.firstOrNull { it.name == category } ?: ChronotypeCategory.INCOMPLETE,
    updatedAtEpochMillis = updatedAtEpochMillis
)

@Dao
interface ChronotypeProfileDao {
    @Query("SELECT * FROM chronotype_profile WHERE profileId = 1")
    suspend fun getProfile(): ChronotypeProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: ChronotypeProfileEntity)
}

@Database(
    entities = [ChronotypeProfileEntity::class, SleepRecordEntity::class, SleepSettingsEntity::class],
    version = 2,
    exportSchema = false
)
abstract class SleepCycleDatabase : RoomDatabase() {
    abstract fun chronotypeProfileDao(): ChronotypeProfileDao
    abstract fun sleepRecordDao(): SleepRecordDao
    abstract fun sleepSettingsDao(): SleepSettingsDao

    companion object {
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS sleep_record (dateEpochDay INTEGER NOT NULL, bedtimeMinutes INTEGER NOT NULL, wakeTimeMinutes INTEGER NOT NULL, primarySleepMinutes INTEGER NOT NULL, napMinutes INTEGER NOT NULL, PRIMARY KEY(dateEpochDay))"
                )
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS sleep_settings (settingsId INTEGER NOT NULL, targetMinutes INTEGER NOT NULL, PRIMARY KEY(settingsId))"
                )
                database.execSQL("INSERT OR IGNORE INTO sleep_settings (settingsId, targetMinutes) VALUES (1, 480)")
            }
        }
    }
}

interface ChronotypeProfileRepository {
    suspend fun load(): ChronotypeProfile?
    suspend fun save(profile: ChronotypeProfile)
}

class RoomChronotypeProfileRepository(
    private val dao: ChronotypeProfileDao
) : ChronotypeProfileRepository {
    override suspend fun load(): ChronotypeProfile? = dao.getProfile()?.toProfile()
    override suspend fun save(profile: ChronotypeProfile) = dao.saveProfile(profile.toEntity())
}

class InMemoryChronotypeProfileRepository(
    initialProfile: ChronotypeProfile? = null,
    private val failOnSave: Boolean = false
) : ChronotypeProfileRepository {
    private var profile: ChronotypeProfile? = initialProfile

    override suspend fun load(): ChronotypeProfile? = profile

    override suspend fun save(profile: ChronotypeProfile) {
        if (failOnSave) throw IllegalStateException("时间型档案保存失败")
        this.profile = profile
    }
}

private fun LocalTime.toMinuteOfDay(): Int = hour * 60 + minute
private fun Int.toLocalTime(): LocalTime = LocalTime.of(this / 60, this % 60)
