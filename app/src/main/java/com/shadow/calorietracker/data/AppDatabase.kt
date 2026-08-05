package com.shadow.calorietracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        FoodEntity::class,
        ServingEntity::class,
        DiaryEntryEntity::class,
        UserProfileEntity::class,
        ServingUsageEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
    abstract fun diaryDao(): DiaryDao
    abstract fun profileDao(): ProfileDao
    abstract fun servingUsageDao(): ServingUsageDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "calorie-quick.db",
            ).addMigrations(MIGRATION_1_2).build().also { instance = it }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE diary_entries ADD COLUMN enteredAmountMilliUnits INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL("UPDATE diary_entries SET enteredAmountMilliUnits = grams * 1000")
                db.execSQL("ALTER TABLE diary_entries ADD COLUMN unitKey TEXT NOT NULL DEFAULT 'grams'")
                db.execSQL("ALTER TABLE diary_entries ADD COLUMN unitLabelEn TEXT NOT NULL DEFAULT 'g'")
                db.execSQL("ALTER TABLE diary_entries ADD COLUMN unitLabelRo TEXT NOT NULL DEFAULT 'g'")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS serving_usage (" +
                        "id TEXT NOT NULL, foodId TEXT NOT NULL, unitKey TEXT NOT NULL, useCount INTEGER NOT NULL, " +
                        "lastUsedAtEpochMillis INTEGER NOT NULL, lastAmountMilliUnits INTEGER NOT NULL, " +
                        "PRIMARY KEY(id))",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_serving_usage_foodId ON serving_usage(foodId)",
                )
            }
        }
    }
}
