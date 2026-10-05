package dev.nick.stepcounter.util

import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class RealTimeProvider @Inject constructor() : dev.nick.stepcounter.domain.util.TimeProvider {
    override fun currentTimeMillis(): Long {
        return System.currentTimeMillis()
    }

    override fun getMidnightToday(): Long {
        return LocalDate.now(ZoneId.systemDefault())
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }
}