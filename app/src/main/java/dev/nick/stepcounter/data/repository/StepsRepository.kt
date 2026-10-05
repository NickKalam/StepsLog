package dev.nick.stepcounter.data.repository

import dev.nick.stepcounter.data.dao.DailyStepTotal
import dev.nick.stepcounter.data.dao.StepsDao
import dev.nick.stepcounter.data.entity.StepsEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId

class StepsRepository (private val stepsDao: StepsDao) {

    suspend fun saveSteps(steps: Int,timestamp:Long=System.currentTimeMillis()) {
        val entity = StepsEntity(steps = steps, timestamp =timestamp)
        stepsDao.insert(entity)
    }

    suspend fun deleteOldSteps() {
        val zone = ZoneId.systemDefault()
        val oneYearAgo = LocalDate.now(zone)
            .minusDays(365)
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()

        stepsDao.deleteOldSteps(oneYearAgo)
    }

    fun getStepsForToday(startOfDay:Long,endOfDay:Long) : Flow<Int> = stepsDao.getStepsForToday(startOfDay,endOfDay)

    fun getAllStepsSync(): List<StepsEntity> =stepsDao.getAllStepsSync()

    fun getStepHistory(days: Long): Flow<List<DailyStepTotal>> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)

        val start = today.minusDays(days - 1)
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()

        val end = today.plusDays(1)
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()

        return stepsDao.getDailyStepHistory(start, end)
    }
    suspend fun restoreHistory(stepRecords: List<Pair<Long, Int>>) {
        val steps = stepRecords.map { record ->
            StepsEntity(timestamp = record.first, steps = record.second)
        }
        stepsDao.deleteAllSteps()
        stepsDao.insertAll(steps)
    }
}