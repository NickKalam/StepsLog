package dev.nick.stepcounter.domain.util

interface TimeProvider {
    fun currentTimeMillis(): Long
    fun getMidnightToday(): Long
}