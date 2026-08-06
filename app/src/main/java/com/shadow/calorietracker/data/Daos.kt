package com.shadow.calorietracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Transaction
    @Query("SELECT * FROM foods WHERE archived = 0 ORDER BY nameEn")
    fun observeFoods(): Flow<List<FoodWithServings>>

    @Query("SELECT COUNT(*) FROM foods")
    suspend fun count(): Int

    @Query("SELECT * FROM foods WHERE barcode = :barcode AND archived = 0 LIMIT 1")
    suspend fun findActiveByBarcode(barcode: String): FoodEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFoods(foods: List<FoodEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertServings(servings: List<ServingEntity>)

    @Upsert
    suspend fun upsertServings(servings: List<ServingEntity>)

    @Upsert
    suspend fun upsertServingPresets(presets: List<ServingPresetEntity>)

    @Query("DELETE FROM serving_presets WHERE servingId IN (SELECT id FROM servings WHERE foodId = :foodId)")
    suspend fun deleteServingPresets(foodId: String)

    @Query("DELETE FROM servings WHERE foodId = :foodId")
    suspend fun deleteServings(foodId: String)

    @Upsert
    suspend fun upsertFood(food: FoodEntity)

    @Upsert
    suspend fun upsertNutrients(values: List<NutrientValueEntity>)

    @Upsert
    suspend fun upsertAllergens(values: List<AllergenDeclarationEntity>)

    @Query("DELETE FROM nutrient_values WHERE foodId = :foodId")
    suspend fun deleteNutrients(foodId: String)

    @Query("DELETE FROM allergen_declarations WHERE foodId = :foodId")
    suspend fun deleteAllergens(foodId: String)

    @Query("UPDATE foods SET archived = 1, updatedAtEpochMillis = :updatedAt WHERE id = :foodId AND isPersonal = 1")
    suspend fun archivePersonalFood(foodId: String, updatedAt: Long)
}

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipes")
    fun observeRecipes(): Flow<List<RecipeEntity>>

    @Query("SELECT * FROM recipe_batches")
    fun observeBatches(): Flow<List<RecipeBatchEntity>>

    @Query("SELECT * FROM recipe_ingredients")
    fun observeIngredients(): Flow<List<RecipeIngredientEntity>>

    @Query("SELECT * FROM recipe_ingredient_allergens")
    fun observeIngredientAllergens(): Flow<List<RecipeIngredientAllergenEntity>>

    @Query("SELECT * FROM recipes WHERE foodId = :foodId LIMIT 1")
    suspend fun findRecipe(foodId: String): RecipeEntity?

    @Query("SELECT COUNT(*) FROM recipe_batches WHERE recipeFoodId = :foodId")
    suspend fun countBatches(foodId: String): Int

    @Query("SELECT * FROM recipe_batches WHERE id = :batchId LIMIT 1")
    suspend fun findBatch(batchId: String): RecipeBatchEntity?

    @Query("UPDATE recipe_batches SET remainingGrams = remainingGrams - :grams WHERE id = :batchId AND remainingGrams >= :grams")
    suspend fun consumeBatch(batchId: String, grams: Int): Int

    @Query("UPDATE recipe_batches SET remainingGrams = MIN(cookedYieldGrams, remainingGrams + :grams) WHERE id = :batchId")
    suspend fun restoreBatch(batchId: String, grams: Int)

    @Upsert
    suspend fun upsertRecipe(recipe: RecipeEntity)

    @Insert
    suspend fun insertBatch(batch: RecipeBatchEntity)

    @Insert
    suspend fun insertIngredients(ingredients: List<RecipeIngredientEntity>)

    @Insert
    suspend fun insertIngredientAllergens(allergens: List<RecipeIngredientAllergenEntity>)
}

@Dao
interface DiaryDao {
    @Query("SELECT * FROM diary_entries ORDER BY consumedAtEpochMillis DESC")
    fun observeAll(): Flow<List<DiaryEntryEntity>>

    @Query(
        "SELECT * FROM diary_entries " +
            "WHERE consumedAtEpochMillis >= :start AND consumedAtEpochMillis < :end " +
            "ORDER BY consumedAtEpochMillis DESC",
    )
    fun observeBetween(start: Long, end: Long): Flow<List<DiaryEntryEntity>>

    @Insert
    suspend fun insert(entry: DiaryEntryEntity)

    @Query(
        "SELECT * FROM diary_entries " +
            "WHERE foodId = :foodId AND consumedAtEpochMillis >= :cutoff " +
            "ORDER BY consumedAtEpochMillis DESC LIMIT 1",
    )
    suspend fun findRecent(foodId: String, cutoff: Long): DiaryEntryEntity?

    @Update
    suspend fun update(entry: DiaryEntryEntity)

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

@Dao
interface ServingUsageDao {
    @Query("SELECT * FROM serving_usage")
    fun observeAll(): Flow<List<ServingUsageEntity>>

    @Query(
        "UPDATE serving_usage SET useCount = useCount + 1, " +
            "lastUsedAtEpochMillis = :usedAt, lastAmountMilliUnits = :amountMilliUnits WHERE id = :id",
    )
    suspend fun increment(id: String, usedAt: Long, amountMilliUnits: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(usage: ServingUsageEntity)

    @Query("UPDATE serving_usage SET useCount = useCount - 1 WHERE id = :id")
    suspend fun decrement(id: String)

    @Query("DELETE FROM serving_usage WHERE id = :id AND useCount <= 0")
    suspend fun deleteIfUnused(id: String)
}

@Dao
interface QuantityUsageDao {
    @Query("SELECT * FROM quantity_usage")
    fun observeAll(): Flow<List<QuantityUsageEntity>>

    @Query(
        "UPDATE quantity_usage SET useCount = useCount + 1, " +
            "lastUsedAtEpochMillis = :usedAt WHERE id = :id",
    )
    suspend fun increment(id: String, usedAt: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(usage: QuantityUsageEntity)
}
