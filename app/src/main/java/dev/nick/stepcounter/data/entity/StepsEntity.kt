package dev.nick.stepcounter.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_steps",
    indices=[Index(value=["timestamp"])]//For faster searches
)
data class StepsEntity(
    @PrimaryKey(autoGenerate = true) val stepsId: Int = 0,
    val steps: Int,
    val timestamp: Long = System.currentTimeMillis()
)
