package dev.nick.stepcounter.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.nick.stepcounter.data.dao.StepsDao
import dev.nick.stepcounter.data.entity.StepsEntity

@Database(
    entities = [
        StepsEntity::class
    ],
    version = 2
)

abstract class StepsDatabase : RoomDatabase() {
    abstract fun stepsDao(): StepsDao
}
