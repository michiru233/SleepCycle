package com.example.sleepcycle.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Database
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

private fun LocalTime.toMinuteOfDay(): Int = hour * 60 + minute
private fun Int.toLocalTime(): LocalTime = LocalTime.of(this / 60, this % 60)

@Dao
interface ChronotypeProfileDao {
    @Query("SELECT * FROM chronotype_profile WHERE profileId = 1")
    suspend fun getProfile(): ChronotypeProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: ChronotypeProfileEntity)
}

@Database(entities = [ChronotypeProfileEntity::class], version = 1, exportSchema = false)
abstract class SleepCycleDatabase : RoomDatabase() {
    abstract fun chronotypeProfileDao(): ChronotypeProfileDao
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
