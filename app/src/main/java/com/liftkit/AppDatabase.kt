package com.liftkit

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

@Database(entities = [RoutineDayEntity::class, WorkoutLogEntity::class, ProteinSettings::class, DailyProteinLog::class], version = 3)
@androidx.room.TypeConverters(TypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun routineDao(): RoutineDao
    abstract fun workoutLogDao(): WorkoutLogDao
    abstract fun proteinSettingsDao(): ProteinSettingsDao
    abstract fun dailyProteinLogDao(): DailyProteinLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val appContext = context.applicationContext
                migrateLegacyDatabase(appContext)
                val instance = Room.databaseBuilder(
                    appContext,
                    AppDatabase::class.java,
                    DB_NAME
                )
                .fallbackToDestructiveMigration() // Allow destructive migration for schema changes
                .build()
                INSTANCE = instance
                instance
            }
        }

        private const val DB_NAME = "lift_kit_db"
        private const val LEGACY_DB_NAME = "gymbuddy_db"

        private fun migrateLegacyDatabase(context: Context) {
            val current = context.getDatabasePath(DB_NAME)
            if (current.exists()) return
            val legacy = context.getDatabasePath(LEGACY_DB_NAME)
            if (!legacy.exists()) return
            current.parentFile?.mkdirs()
            for (suffix in listOf("", "-wal", "-shm")) {
                val from = File(legacy.path + suffix)
                if (!from.exists()) continue
                from.copyTo(File(current.path + suffix), overwrite = false)
            }
        }
    }
}
