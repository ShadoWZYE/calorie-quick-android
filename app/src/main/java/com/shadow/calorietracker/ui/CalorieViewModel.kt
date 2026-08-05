package com.shadow.calorietracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shadow.calorietracker.data.AppDatabase
import com.shadow.calorietracker.data.CalorieRepository
import com.shadow.calorietracker.model.Food
import com.shadow.calorietracker.model.FoodEntry
import com.shadow.calorietracker.model.Nutrition
import com.shadow.calorietracker.model.PersonalFoodDraft
import com.shadow.calorietracker.model.QuantityUsage
import com.shadow.calorietracker.model.Serving
import com.shadow.calorietracker.model.UnitUsage
import com.shadow.calorietracker.model.UserProfile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppUiState(
    val loaded: Boolean = false,
    val profile: UserProfile? = null,
    val foods: List<Food> = emptyList(),
    val entries: List<FoodEntry> = emptyList(),
    val unitUsage: Map<String, List<UnitUsage>> = emptyMap(),
    val quantityUsage: Map<String, List<QuantityUsage>> = emptyMap(),
) {
    val totals: Nutrition = entries.fold(Nutrition.Zero) { total, entry -> total + entry.nutrition }
    val fiberIncomplete: Boolean = entries.any { it.nutrition.fiberGrams == null }
}

class CalorieViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CalorieRepository(AppDatabase.get(application))

    val uiState = combine(
        repository.profile,
        repository.foods,
        repository.todayEntries(),
        repository.unitUsage,
        repository.quantityUsage,
    ) { profile, foods, entries, unitUsage, quantityUsage ->
        AppUiState(
            loaded = true,
            profile = profile,
            foods = foods,
            entries = entries,
            unitUsage = unitUsage.groupBy(UnitUsage::foodId),
            quantityUsage = quantityUsage.groupBy(QuantityUsage::foodId),
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

    fun archivePersonalFood(foodId: String) {
        viewModelScope.launch { repository.archivePersonalFood(foodId) }
    }
}
