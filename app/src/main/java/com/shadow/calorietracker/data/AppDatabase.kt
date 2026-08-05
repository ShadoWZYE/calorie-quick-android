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
        QuantityUsageEntity::class,
        NutrientValueEntity::class,
        AllergenDeclarationEntity::class,
        ServingPresetEntity::class,
        RecipeEntity::class,
        RecipeBatchEntity::class,
        RecipeIngredientEntity::class,
        RecipeIngredientAllergenEntity::class,
    ],
    version = 8,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
    abstract fun diaryDao(): DiaryDao
    abstract fun profileDao(): ProfileDao
    abstract fun servingUsageDao(): ServingUsageDao
    abstract fun quantityUsageDao(): QuantityUsageDao
    abstract fun recipeDao(): RecipeDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "calorie-quick.db",
            ).addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7,
                MIGRATION_7_8,
            ).build().also { instance = it }
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

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS quantity_usage (" +
                        "id TEXT NOT NULL, foodId TEXT NOT NULL, unitKey TEXT NOT NULL, " +
                        "amountMilliUnits INTEGER NOT NULL, useCount INTEGER NOT NULL, " +
                        "lastUsedAtEpochMillis INTEGER NOT NULL, PRIMARY KEY(id))",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_quantity_usage_foodId ON quantity_usage(foodId)",
                )
                db.execSQL(
                    "INSERT OR IGNORE INTO quantity_usage " +
                        "(id, foodId, unitKey, amountMilliUnits, useCount, lastUsedAtEpochMillis) " +
                        "SELECT foodId || '|' || unitKey || '|' || lastAmountMilliUnits, " +
                        "foodId, unitKey, lastAmountMilliUnits, useCount, lastUsedAtEpochMillis " +
                        "FROM serving_usage",
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE user_profile ADD COLUMN targetMode TEXT NOT NULL DEFAULT 'ESTIMATED'",
                )
            }
        }


        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE foods ADD COLUMN brand TEXT")
                db.execSQL("ALTER TABLE foods ADD COLUMN barcode TEXT")
                db.execSQL("ALTER TABLE foods ADD COLUMN isPersonal INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE foods ADD COLUMN archived INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE foods ADD COLUMN updatedAtEpochMillis INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE diary_entries ADD COLUMN fiberMilligrams INTEGER")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN fiberGoalGrams INTEGER NOT NULL DEFAULT 25")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS nutrient_values (" +
                        "id TEXT NOT NULL, foodId TEXT NOT NULL, nutrientKey TEXT NOT NULL, " +
                        "amountMilliUnitsPer100g INTEGER NOT NULL, source TEXT NOT NULL, PRIMARY KEY(id))",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_nutrient_values_foodId ON nutrient_values(foodId)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS allergen_declarations (" +
                        "id TEXT NOT NULL, foodId TEXT NOT NULL, allergenKey TEXT NOT NULL, " +
                        "declaration TEXT NOT NULL, PRIMARY KEY(id))",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_allergen_declarations_foodId ON allergen_declarations(foodId)")
                listOf(
                    "calories" to "caloriesPer100g",
                    "protein" to "proteinMilligramsPer100g",
                    "carbs" to "carbsMilligramsPer100g",
                    "fat" to "fatMilligramsPer100g",
                ).forEach { (key, column) ->
                    db.execSQL(
                        "INSERT INTO nutrient_values (id, foodId, nutrientKey, amountMilliUnitsPer100g, source) " +
                            "SELECT id || '|$key', id, '$key', $column, 'legacy_seed' FROM foods",
                    )
                }
                mapOf("greek-yogurt" to 0, "banana" to 2_600, "chicken-breast" to 0, "oats" to 10_100,
                    "eggs" to 0, "rice" to 400).forEach { (foodId, amount) ->
                    db.execSQL(
                        "INSERT INTO nutrient_values VALUES ('$foodId|fiber', '$foodId', 'fiber', $amount, 'legacy_seed')",
                    )
                }
                db.execSQL("INSERT INTO allergen_declarations VALUES ('eggs|EGGS', 'eggs', 'EGGS', 'CONTAINS')")
                db.execSQL("INSERT INTO allergen_declarations VALUES ('greek-yogurt|MILK', 'greek-yogurt', 'MILK', 'CONTAINS')")
            }
        }


        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE foods ADD COLUMN isPackaged INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE servings ADD COLUMN isPackage INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS serving_presets (" +
                        "id TEXT NOT NULL, servingId TEXT NOT NULL, amountMilliUnits INTEGER NOT NULL, PRIMARY KEY(id))",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_serving_presets_servingId ON serving_presets(servingId)")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE foods ADD COLUMN sourceType TEXT NOT NULL DEFAULT 'BUILT_IN'")
                db.execSQL("ALTER TABLE foods ADD COLUMN sourceId TEXT")
                db.execSQL("ALTER TABLE foods ADD COLUMN importedAtEpochMillis INTEGER")
                db.execSQL("UPDATE foods SET sourceType = 'PERSONAL' WHERE isPersonal = 1")
                db.execSQL(
                    "UPDATE foods SET sourceType = 'OPEN_FOOD_FACTS', " +
                        "sourceId = COALESCE(barcode, SUBSTR(id, 5)), " +
                        "importedAtEpochMillis = updatedAtEpochMillis WHERE id LIKE 'off-%'",
                )
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS recipes (" +
                        "foodId TEXT NOT NULL, name TEXT NOT NULL, activeBatchId TEXT NOT NULL, createdAtEpochMillis INTEGER NOT NULL, " +
                        "updatedAtEpochMillis INTEGER NOT NULL, PRIMARY KEY(foodId))",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS recipe_batches (" +
                        "id TEXT NOT NULL, recipeFoodId TEXT NOT NULL, cookedYieldGrams INTEGER NOT NULL, " +
                        "portionCount INTEGER NOT NULL, cookedAtEpochMillis INTEGER NOT NULL, PRIMARY KEY(id))",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recipe_batches_recipeFoodId ON recipe_batches(recipeFoodId)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS recipe_ingredients (" +
                        "id TEXT NOT NULL, batchId TEXT NOT NULL, foodId TEXT NOT NULL, foodNameEn TEXT NOT NULL, " +
                        "foodNameRo TEXT NOT NULL, grams INTEGER NOT NULL, caloriesPer100g INTEGER NOT NULL, " +
                        "proteinMilligramsPer100g INTEGER NOT NULL, carbsMilligramsPer100g INTEGER NOT NULL, " +
                        "fatMilligramsPer100g INTEGER NOT NULL, fiberMilligramsPer100g INTEGER, " +
                        "sortOrder INTEGER NOT NULL, PRIMARY KEY(id))",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recipe_ingredients_batchId ON recipe_ingredients(batchId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recipe_ingredients_foodId ON recipe_ingredients(foodId)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS recipe_ingredient_allergens (" +
                        "id TEXT NOT NULL, recipeIngredientId TEXT NOT NULL, allergenKey TEXT NOT NULL, " +
                        "declaration TEXT NOT NULL, PRIMARY KEY(id))",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_recipe_ingredient_allergens_recipeIngredientId " +
                        "ON recipe_ingredient_allergens(recipeIngredientId)",
                )
            }
        }
    }
}
