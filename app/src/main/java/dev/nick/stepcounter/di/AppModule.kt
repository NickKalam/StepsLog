package dev.nick.stepcounter.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.nick.stepcounter.data.dao.StepsDao
import dev.nick.stepcounter.data.database.StepsDatabase
import dev.nick.stepcounter.data.repository.StepsRepository
import dev.nick.stepcounter.data.repository.UserPreferencesRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_daily_steps_timestamp` ON `daily_steps` (`timestamp`)")
        }
    }
    @Provides
    @Singleton
    fun provideStepsDatabase(@ApplicationContext context: Context): StepsDatabase {
        return Room.databaseBuilder(
            context,
            StepsDatabase::class.java,
            "steps_database"
        ).addMigrations(MIGRATION_1_2)
        //.fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    @Singleton
    fun provideStepsDao(database: StepsDatabase): StepsDao {
        return database.stepsDao()
    }

    @Provides
    @Singleton
    fun provideUserMeasurements(@ApplicationContext context: Context): dev.nick.stepcounter.data.datastore.UserMeasurements {
        return dev.nick.stepcounter.data.datastore.UserMeasurements(context)
    }

    @Provides
    @Singleton
    fun provideStepsRepository(stepsDao: StepsDao): StepsRepository {
        return StepsRepository(stepsDao)
    }

    @Provides
    @Singleton
    fun provideUserPreferencesRepository(userMeasurements: dev.nick.stepcounter.data.datastore.UserMeasurements): UserPreferencesRepository {
        return UserPreferencesRepository(userMeasurements)
    }

    @Provides
    @Singleton
    fun provideTimeProvider(realTimeProvider: dev.nick.stepcounter.util.RealTimeProvider): dev.nick.stepcounter.domain.util.TimeProvider {
        return realTimeProvider
    }

}
