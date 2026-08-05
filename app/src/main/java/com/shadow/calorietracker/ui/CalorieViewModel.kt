package com.shadow.calorietracker.ui

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shadow.calorietracker.data.AppDatabase
import com.shadow.calorietracker.data.CalorieRepository
import com.shadow.calorietracker.data.OpenFoodFactsClient
import com.shadow.calorietracker.data.OpenFoodFactsException
import com.shadow.calorietracker.data.NutritionLabelOcr
import com.shadow.calorietracker.data.NutritionLabelPrefill
import com.shadow.calorietracker.model.Food
import com.shadow.calorietracker.model.FoodEntry
import com.shadow.calorietracker.model.Nutrition
import com.shadow.calorietracker.model.PersonalFoodDraft
import com.shadow.calorietracker.model.QuantityUsage
import com.shadow.calorietracker.model.RecipeDraft
import com.shadow.calorietracker.model.RecipeTemplate
import com.shadow.calorietracker.model.Serving
import com.shadow.calorietracker.model.UnitUsage
import com.shadow.calorietracker.model.UserProfile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class AppUiState(
    val loaded: Boolean = false,
    val profile: UserProfile? = null,
    val foods: List<Food> = emptyList(),
    val entries: List<FoodEntry> = emptyList(),
    val unitUsage: Map<String, List<UnitUsage>> = emptyMap(),
    val quantityUsage: Map<String, List<QuantityUsage>> = emptyMap(),
    val recipes: Map<String, RecipeTemplate> = emptyMap(),
) {
    val totals: Nutrition = entries.fold(Nutrition.Zero) { total, entry -> total + entry.nutrition }
    val fiberIncomplete: Boolean = entries.any { it.nutrition.fiberGrams == null }
}

private data class CatalogueState(
    val profile: UserProfile?,
    val foods: List<Food>,
    val entries: List<FoodEntry>,
)

private data class UsageAndRecipeState(
    val unitUsage: List<UnitUsage>,
    val quantityUsage: List<QuantityUsage>,
    val recipes: List<RecipeTemplate>,
)

enum class FoodLookupStatus { IDLE, SEARCHING, SUCCESS, ERROR, RATE_LIMITED }

data class FoodLookupState(
    val query: String = "",
    val status: FoodLookupStatus = FoodLookupStatus.IDLE,
    val results: List<Food> = emptyList(),
    val isBarcodeLookup: Boolean = false,
)

enum class NutritionLabelScanStatus { IDLE, PROCESSING, SUCCESS, ERROR }

data class NutritionLabelScanState(
    val status: NutritionLabelScanStatus = NutritionLabelScanStatus.IDLE,
    val prefill: NutritionLabelPrefill? = null,
)

class CalorieViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CalorieRepository(AppDatabase.get(application))
    private val openFoodFacts = OpenFoodFactsClient()
    private val _foodLookupState = MutableStateFlow(FoodLookupState())
    val foodLookupState: StateFlow<FoodLookupState> = _foodLookupState.asStateFlow()
    private var lookupJob: Job? = null
    private val nutritionLabelOcr = NutritionLabelOcr(application)
    private val _nutritionLabelScanState = MutableStateFlow(NutritionLabelScanState())
    val nutritionLabelScanState: StateFlow<NutritionLabelScanState> = _nutritionLabelScanState.asStateFlow()
    private var nutritionLabelScanJob: Job? = null

    val uiState = combine(
        combine(repository.profile, repository.foods, repository.todayEntries(), ::CatalogueState),
        combine(repository.unitUsage, repository.quantityUsage, repository.recipes, ::UsageAndRecipeState),
    ) { catalogue, usage ->
        AppUiState(
            loaded = true,
            profile = catalogue.profile,
            foods = catalogue.foods,
            entries = catalogue.entries,
            unitUsage = usage.unitUsage.groupBy(UnitUsage::foodId),
            quantityUsage = usage.quantityUsage.groupBy(QuantityUsage::foodId),
            recipes = usage.recipes.associateBy(RecipeTemplate::foodId),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState())

    init {
        viewModelScope.launch { repository.seedFoods() }
    }

    fun saveProfile(profile: UserProfile) {
        viewModelScope.launch { repository.saveProfile(profile) }
    }

    fun addEntry(food: Food, amount: Double, serving: Serving?) {
        viewModelScope.launch { repository.addEntry(food, amount, serving) }
    }

    fun deleteEntry(entry: FoodEntry) {
        viewModelScope.launch { repository.deleteEntry(entry) }
    }

    fun savePersonalFood(draft: PersonalFoodDraft) {
        viewModelScope.launch { repository.savePersonalFood(draft) }
    }

    fun saveRecipe(draft: RecipeDraft) {
        viewModelScope.launch { repository.saveRecipe(draft) }
    }

    fun archivePersonalFood(foodId: String) {
        viewModelScope.launch { repository.archivePersonalFood(foodId) }
    }

    fun clearFoodLookup() {
        lookupJob?.cancel()
        _foodLookupState.value = FoodLookupState()
    }

    fun searchOpenFoodFacts(query: String, locale: String) {
        val normalized = query.trim()
        if (normalized.length < 2) return
        lookupJob?.cancel()
        _foodLookupState.value = FoodLookupState(normalized, FoodLookupStatus.SEARCHING)
        lookupJob = viewModelScope.launch {
            _foodLookupState.value = try {
                FoodLookupState(
                    query = normalized,
                    status = FoodLookupStatus.SUCCESS,
                    results = openFoodFacts.search(normalized, locale),
                )
            } catch (_: OpenFoodFactsException.RateLimited) {
                FoodLookupState(normalized, FoodLookupStatus.RATE_LIMITED)
            } catch (error: Exception) {
                Log.w("OpenFoodFacts", "Search failed", error)
                FoodLookupState(normalized, FoodLookupStatus.ERROR)
            }
        }
    }

    fun lookupBarcode(barcode: String, locale: String) {
        val normalized = barcode.filter(Char::isDigit)
        lookupJob?.cancel()
        _foodLookupState.value = FoodLookupState(
            query = normalized,
            status = FoodLookupStatus.SEARCHING,
            isBarcodeLookup = true,
        )
        lookupJob = viewModelScope.launch {
            _foodLookupState.value = try {
                FoodLookupState(
                    query = normalized,
                    status = FoodLookupStatus.SUCCESS,
                    results = listOfNotNull(openFoodFacts.productByBarcode(normalized, locale)),
                    isBarcodeLookup = true,
                )
            } catch (_: OpenFoodFactsException.RateLimited) {
                FoodLookupState(normalized, FoodLookupStatus.RATE_LIMITED, isBarcodeLookup = true)
            } catch (error: Exception) {
                Log.w("OpenFoodFacts", "Barcode lookup failed", error)
                FoodLookupState(normalized, FoodLookupStatus.ERROR, isBarcodeLookup = true)
            }
        }
    }

    fun scanNutritionLabel(uri: Uri, suggestedName: String) {
        nutritionLabelScanJob?.cancel()
        _nutritionLabelScanState.value = NutritionLabelScanState(NutritionLabelScanStatus.PROCESSING)
        nutritionLabelScanJob = viewModelScope.launch {
            _nutritionLabelScanState.value = try {
                NutritionLabelScanState(
                    NutritionLabelScanStatus.SUCCESS,
                    nutritionLabelOcr.scan(uri, suggestedName),
                )
            } catch (error: Exception) {
                Log.w("NutritionLabelOcr", "Label scan failed", error)
                NutritionLabelScanState(NutritionLabelScanStatus.ERROR)
            } finally {
                if (uri.authority == "${getApplication<Application>().packageName}.fileprovider") {
                    runCatching { getApplication<Application>().contentResolver.delete(uri, null, null) }
                }
            }
        }
    }

    fun clearNutritionLabelScan() {
        nutritionLabelScanJob?.cancel()
        _nutritionLabelScanState.value = NutritionLabelScanState()
    }

    override fun onCleared() {
        nutritionLabelOcr.close()
        super.onCleared()
    }
}
