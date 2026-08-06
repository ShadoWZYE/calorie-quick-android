package com.shadow.calorietracker.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.shadow.calorietracker.model.Food
import com.shadow.calorietracker.model.FoodImage
import com.shadow.calorietracker.model.FoodImageSource
import com.shadow.calorietracker.model.FoodProvenance
import com.shadow.calorietracker.model.FoodSourceType
import com.shadow.calorietracker.model.LocalizedText
import com.shadow.calorietracker.model.Nutrition
import com.shadow.calorietracker.model.Serving
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DiscoveredFoodCatalogueTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: CalorieRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = CalorieRepository(database)
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `valid API result is appended as shared non-personal catalogue food`() = runBlocking {
        val food = discoveredFood("5941234567890")

        assertEquals(1, repository.upsertDiscoveredFoods(listOf(food)))

        val stored = repository.foods.first().single()
        assertFalse(stored.isPersonal)
        assertEquals(FoodSourceType.OPEN_FOOD_FACTS, stored.provenance.type)
        assertEquals("beverages", stored.categoryKey)
        assertEquals("https://images.openfoodfacts.org/example.jpg", stored.image?.remoteUrl)
        assertEquals(330.0, stored.servings.single().grams, 0.001)
    }

    @Test
    fun `personal barcode match is never overwritten by API discovery`() = runBlocking {
        database.foodDao().upsertFood(
            FoodEntity(
                id = "personal-existing",
                nameEn = "My drink",
                nameRo = "Băutura mea",
                detailEn = "Personal food",
                detailRo = "Aliment personal",
                caloriesPer100g = 10,
                proteinMilligramsPer100g = 0,
                carbsMilligramsPer100g = 2_000,
                fatMilligramsPer100g = 0,
                barcode = "5941234567890",
                isPersonal = true,
                sourceType = FoodSourceType.PERSONAL.name,
            ),
        )

        assertEquals(0, repository.upsertDiscoveredFoods(listOf(discoveredFood("5941234567890"))))

        assertEquals("My drink", database.foodDao().findFood("personal-existing")?.nameEn)
        assertEquals(null, database.foodDao().findFood("off-5941234567890"))
    }

    private fun discoveredFood(barcode: String) = Food(
        id = "off-$barcode",
        names = LocalizedText("Shared drink", "Băutură partajată"),
        details = LocalizedText("Example", "Example"),
        nutritionPer100g = Nutrition(42, 0.0, 10.0, 0.0, 0.2),
        servings = listOf(Serving("off-$barcode-package", LocalizedText("1 package", "1 ambalaj"), 330.0, isPackage = true)),
        brand = "Example",
        barcode = barcode,
        isPackaged = true,
        provenance = FoodProvenance(FoodSourceType.OPEN_FOOD_FACTS, barcode),
        image = FoodImage(FoodImageSource.REMOTE, remoteUrl = "https://images.openfoodfacts.org/example.jpg"),
        categoryKey = "beverages",
    )
}
