package dev.nick.stepcounter.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.nick.stepcounter.data.entity.StepsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StepsDao {


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(steps: StepsEntity): Long

    @Query("SELECT COALESCE (SUM(steps),0) FROM daily_steps WHERE timestamp BETWEEN :startOfDay AND :endOfDay")
    fun getStepsForToday(startOfDay: Long, endOfDay:Long): Flow<Int>

    @Query("SELECT * FROM daily_steps ORDER BY timestamp ASC")
    fun getAllStepsSync():List<StepsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(steps: List<StepsEntity>)

    @Query("DELETE FROM daily_steps")
    suspend fun deleteAllSteps()

    @Query("""
    SELECT
        date(timestamp / 1000, 'unixepoch', 'localtime') AS day,
        SUM(steps) AS totalSteps
    FROM daily_steps
    WHERE timestamp BETWEEN :start AND :end
    GROUP BY day
    ORDER BY day DESC
""")
    fun getDailyStepHistory(
        start: Long,
        end: Long
    ): Flow<List<DailyStepTotal>>

    @Query("""
    DELETE 
    FROM daily_steps
    WHERE timestamp < :start
""")
    suspend fun deleteOldSteps(start:Long)


}

data class DailyStepTotal(
    val day: String,
    val totalSteps: Long
)