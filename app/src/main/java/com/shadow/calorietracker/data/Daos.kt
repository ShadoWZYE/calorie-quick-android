package com.shadow.calorietracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Transaction
    @Query("SELECT * FROM foods ORDER BY nameEn")
    fun observeFoods(): Flow<List<FoodWithServings>>

    @Query("SELECT COUNT(*) FROM foods")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFoods(foods: List<FoodEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertServings(servings: List<ServingEntity>)
}

@Dao
interface DiaryDao {
    @Query(
        "SELECT * FROM diary_entries " +
            "WHERE consumedAtEpochMillis >= :start AND consumedAtEpochMillis < :end " +
            "ORDER BY consumedAtEpochMillis DESC",
    )
    fun observeBetween(start: Long, end: Long): Flow<List<DiaryEntryEntity>>

    @Insert
    suspend fun insert(entry: DiaryEntryEntity)

    @Delete
    suspend fun delete(entry: DiaryEntryEntity)
}

@Dao
interface ProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun observe(): Flow<UserProfileEntity?>

    @Upsert
    suspend fun upsert(profile: UserProfileEntity)
}

