package com.shadow.calorietracker.ui

import android.app.Application
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shadow.calorietracker.data.AppDatabase
import com.shadow.calorietracker.data.CalorieRepository
import com.shadow.calorietracker.data.CatalogueExporter
import com.shadow.calorietracker.data.CatalogueExportResult
import com.shadow.calorietracker.data.FoodImageStore
import com.shadow.calorietracker.data.FullBackupExportResult
import com.shadow.calorietracker.data.FullBackupManager
import com.shadow.calorietracker.data.FullBackupPreview
import com.shadow.calorietracker.data.InvalidFullBackupException
import com.shadow.calorietracker.data.OpenFoodFactsClient
import com.shadow.calorietracker.data.OpenFoodFactsResponseCache
import com.shadow.calorietracker.data.OpenFoodFactsException
import com.shadow.calorietracker.data.NutritionLabelOcr
import com.shadow.calorietracker.data.NutritionLabelPrefill
import com.shadow.calorietracker.data.BodyScaleOcr
import com.shadow.calorietracker.data.BodyScalePrefill
import com.shadow.calorietracker.data.SupportDiagnosticStore
import com.shadow.calorietracker.data.FeedbackMessage
import com.shadow.calorietracker.data.BuiltInCatalogueImporter
import com.shadow.calorietracker.model.Food
import com.shadow.calorietracker.model.BodyMeasurement
import com.shadow.calorietracker.model.FoodEntry
import com.shadow.calorietracker.model.DailyNutritionSummary
import com.shadow.calorietracker.model.HistoryReportCalculator
import com.shadow.calorietracker.model.Nutrition
import com.shadow.calorietracker.model.PersonalFoodDraft
import com.shadow.calorietracker.model.QuantityUsage
import com.shadow.calorietracker.model.PreparationUsage
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.Instant

data class AppUiState(
    val loaded: Boolean = false,
    val profile: UserProfile? = null,
    val foods: List<Food> = emptyList(),
    val entries: List<FoodEntry> = emptyList(),
    val allEntries: List<FoodEntry> = emptyList(),
    val historyDays: List<DailyNutritionSummary> = emptyList(),
    val unitUsage: Map<String, List<UnitUsage>> = emptyMap(),
    val quantityUsage: Map<String, List<QuantityUsage>> = emptyMap(),
    val recipes: Map<String, RecipeTemplate> = emptyMap(),
    val ingredientUsage: Map<String, Int> = emptyMap(),
    val bodyMeasurements: List<BodyMeasurement> = emptyList(),
    val preparationUsage: Map<String, List<PreparationUsage>> = emptyMap(),
) {
    val totals: Nutrition = entries.fold(Nutrition.Zero) { total, entry -> total + entry.nutrition }
    val fiberIncomplete: Boolean = entries.any { it.nutrition.fiberGrams == null }
}

private data class CatalogueState(
    val profile: UserProfile?,
    val foods: List<Food>,
    val allEntries: List<FoodEntry>,
    val bodyMeasurements: List<BodyMeasurement>,
)

private data class UsageAndRecipeState(
    val unitUsage: List<UnitUsage>,
    val quantityUsage: List<QuantityUsage>,
    val recipes: List<RecipeTemplate>,
    val ingredientUsage: Map<String, Int>,
    val preparationUsage: List<PreparationUsage>,
)

enum class FoodLookupStatus { IDLE, SEARCHING, SUCCESS, ERROR, RATE_LIMITED, SERVICE_UNAVAILABLE }

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
    val stagedImagePath: String? = null,
    val flaggedForSupport: Boolean = false,
)

enum class BodyScaleScanStatus { IDLE, PROCESSING, SUCCESS, ERROR }

data class BodyScaleScanState(
    val status: BodyScaleScanStatus = BodyScaleScanStatus.IDLE,
    val prefill: BodyScalePrefill? = null,
    val stagedImagePath: String? = null,
    val flaggedForSupport: Boolean = false,
)

enum class CatalogueExportStatus { IDLE, EXPORTING, SUCCESS, ERROR }
data class CatalogueExportState(
    val status: CatalogueExportStatus = CatalogueExportStatus.IDLE,
    val result: CatalogueExportResult? = null,
)

data class SupportExportState(
    val savedDiagnosticCount: Int = 0,
    val savedPerformanceCount: Int = 0,
    val savedFreezeCount: Int = 0,
    val savedCrashCount: Int = 0,
)

enum class FullBackupStatus { IDLE, EXPORTING, EXPORTED, INSPECTING, PREVIEW_READY, RESTORING, RESTORED, INVALID, ERROR }

data class FullBackupState(
    val status: FullBackupStatus = FullBackupStatus.IDLE,
    val preview: FullBackupPreview? = null,
    val exportResult: FullBackupExportResult? = null,
)

class CalorieViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.get(application)
    private val repository = CalorieRepository(database, BuiltInCatalogueImporter(application, database))
    private val openFoodFactsCache = OpenFoodFactsResponseCache(application)
    private val openFoodFacts = OpenFoodFactsClient(openFoodFactsCache)
    private val foodImageStore = FoodImageStore(application)
    private val supportDiagnostics = SupportDiagnosticStore(application)
    private val catalogueExporter = CatalogueExporter(application, database, supportDiagnostics, openFoodFactsCache)
    private val fullBackupManager = FullBackupManager(application, database, supportDiagnostics)
    private val _catalogueExportState = MutableStateFlow(CatalogueExportState())
    val catalogueExportState: StateFlow<CatalogueExportState> = _catalogueExportState.asStateFlow()
    private val _supportExportState = MutableStateFlow(
        SupportExportState(
            savedDiagnosticCount = supportDiagnostics.count(),
            savedPerformanceCount = supportDiagnostics.performanceCount(),
            savedFreezeCount = supportDiagnostics.freezeCount(),
            savedCrashCount = supportDiagnostics.crashCount(),
        ),
    )
    val supportExportState: StateFlow<SupportExportState> = _supportExportState.asStateFlow()
    private val _fullBackupState = MutableStateFlow(FullBackupState())
    val fullBackupState: StateFlow<FullBackupState> = _fullBackupState.asStateFlow()
    private var pendingRestoreUri: Uri? = null
    private val _feedbackMessages = MutableStateFlow(supportDiagnostics.feedbackMessages())
    val feedbackMessages: StateFlow<List<FeedbackMessage>> = _feedbackMessages.asStateFlow()
    private val _foodLookupState = MutableStateFlow(FoodLookupState())
    val foodLookupState: StateFlow<FoodLookupState> = _foodLookupState.asStateFlow()
    private var lookupJob: Job? = null
    private val nutritionLabelOcr = NutritionLabelOcr(application)
    private val _nutritionLabelScanState = MutableStateFlow(NutritionLabelScanState())
    val nutritionLabelScanState: StateFlow<NutritionLabelScanState> = _nutritionLabelScanState.asStateFlow()
    private var nutritionLabelScanJob: Job? = null
    private val bodyScaleOcr = BodyScaleOcr(application)
    private val _bodyScaleScanState = MutableStateFlow(BodyScaleScanState())
    val bodyScaleScanState: StateFlow<BodyScaleScanState> = _bodyScaleScanState.asStateFlow()
    private var bodyScaleScanJob: Job? = null

    val uiState = combine(
        combine(repository.profile, repository.foods, repository.allEntries, repository.bodyMeasurements, ::CatalogueState),
        combine(
            repository.unitUsage,
            repository.quantityUsage,
            repository.recipes,
            repository.ingredientUsage,
            repository.preparationUsage,
            ::UsageAndRecipeState,
        ),
    ) { catalogue, usage ->
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)
        val todayEntries = catalogue.allEntries.filter {
            Instant.ofEpochMilli(it.consumedAtEpochMillis).atZone(zoneId).toLocalDate() == today
        }
        AppUiState(
            loaded = true,
            profile = catalogue.profile,
            foods = catalogue.foods,
            entries = todayEntries,
            allEntries = catalogue.allEntries,
            historyDays = HistoryReportCalculator.daily(catalogue.allEntries, zoneId),
            unitUsage = usage.unitUsage.groupBy(UnitUsage::foodId),
            quantityUsage = usage.quantityUsage.groupBy(QuantityUsage::foodId),
            recipes = usage.recipes.associateBy(RecipeTemplate::foodId),
            ingredientUsage = usage.ingredientUsage,
            bodyMeasurements = catalogue.bodyMeasurements,
            preparationUsage = usage.preparationUsage.groupBy(PreparationUsage::foodId),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState())

    init {
        viewModelScope.launch { repository.seedFoods() }
    }

    fun saveProfile(profile: UserProfile) {
        viewModelScope.launch { repository.saveProfile(profile) }
    }

    fun addEntry(
        food: Food,
        amount: Double,
        serving: Serving?,
        recipeBatchId: String? = null,
        consumedAtEpochMillis: Long = System.currentTimeMillis(),
    ) {
        viewModelScope.launch { repository.addEntry(food, amount, serving, recipeBatchId, consumedAtEpochMillis) }
    }

    fun updateEntry(entry: FoodEntry, enteredAmount: Double, consumedAtEpochMillis: Long) {
        viewModelScope.launch { repository.updateEntry(entry, enteredAmount, consumedAtEpochMillis) }
    }

    fun deleteEntry(entry: FoodEntry) {
        viewModelScope.launch { repository.deleteEntry(entry) }
    }

    fun saveBodyMeasurement(measurement: BodyMeasurement) {
        viewModelScope.launch { repository.saveBodyMeasurement(measurement) }
    }

    fun deleteBodyMeasurement(measurement: BodyMeasurement) {
        viewModelScope.launch { repository.deleteBodyMeasurement(measurement) }
    }

    fun scanBodyScaleReport(uri: Uri) {
        bodyScaleScanJob?.cancel()
        _bodyScaleScanState.value = BodyScaleScanState(BodyScaleScanStatus.PROCESSING)
        bodyScaleScanJob = viewModelScope.launch {
            var stagedPath: String? = null
            _bodyScaleScanState.value = try {
                val staged = supportDiagnostics.stage(uri, "body-scale")
                stagedPath = staged.absolutePath
                val prefill = bodyScaleOcr.scan(Uri.fromFile(staged))
                if (prefill.measurement != null) supportDiagnostics.discard(stagedPath)
                BodyScaleScanState(
                    status = BodyScaleScanStatus.SUCCESS,
                    prefill = prefill,
                    stagedImagePath = stagedPath.takeIf { prefill.measurement == null },
                )
            } catch (error: Exception) {
                Log.w("BodyScaleOcr", "Scale report scan failed", error)
                BodyScaleScanState(BodyScaleScanStatus.ERROR, stagedImagePath = stagedPath)
            }
        }
    }

    fun flagBodyScaleScanForSupport() {
        val state = _bodyScaleScanState.value
        val path = state.stagedImagePath ?: return
        if (supportDiagnostics.flag(path, "body-scale", "ocr-or-parser-failure")) {
            _bodyScaleScanState.value = state.copy(stagedImagePath = null, flaggedForSupport = true)
            refreshSupportCount()
        }
    }

    fun clearBodyScaleScan() {
        bodyScaleScanJob?.cancel()
        supportDiagnostics.discard(_bodyScaleScanState.value.stagedImagePath)
        _bodyScaleScanState.value = BodyScaleScanState()
    }

    fun savePersonalFood(draft: PersonalFoodDraft) {
        viewModelScope.launch {
            val pendingUri = draft.pendingImageUri?.let(Uri::parse)
            val image = pendingUri?.let(foodImageStore::import) ?: draft.image
            repository.savePersonalFood(draft.copy(image = image, pendingImageUri = null))
            if (pendingUri?.authority == "${getApplication<Application>().packageName}.fileprovider") {
                runCatching { getApplication<Application>().contentResolver.delete(pendingUri, null, null) }
            }
        }
    }

    fun saveRecipe(draft: RecipeDraft) {
        viewModelScope.launch { repository.saveRecipe(draft) }
    }

    fun archivePersonalFood(foodId: String) {
        viewModelScope.launch { repository.archivePersonalFood(foodId) }
    }

    fun exportReviewCatalogue(
        uri: Uri,
        selectedFoodIds: Set<String>? = null,
        selectedRecipeIds: Set<String>? = null,
        includeDiagnostics: Boolean = true,
        includeFreezeReports: Boolean = true,
        includeCrashReports: Boolean = true,
        includeFeedback: Boolean = true,
    ) {
        _catalogueExportState.value = CatalogueExportState(CatalogueExportStatus.EXPORTING)
        viewModelScope.launch {
            _catalogueExportState.value = try {
                CatalogueExportState(
                    CatalogueExportStatus.SUCCESS,
                    catalogueExporter.exportTo(
                        uri,
                        selectedFoodIds = selectedFoodIds,
                        selectedRecipeIds = selectedRecipeIds,
                        includeDiagnostics = includeDiagnostics,
                        includeFreezeReports = includeFreezeReports,
                        includeCrashReports = includeCrashReports,
                        includeFeedback = includeFeedback,
                    ),
                )
            } catch (error: Exception) {
                Log.w("CatalogueExport", "Catalogue export failed", error)
                CatalogueExportState(CatalogueExportStatus.ERROR)
            }
        }
    }

    fun clearCatalogueExportStatus() {
        _catalogueExportState.value = CatalogueExportState()
    }

    fun exportFullBackup(uri: Uri, languageTags: String) {
        _fullBackupState.value = FullBackupState(FullBackupStatus.EXPORTING)
        viewModelScope.launch {
            _fullBackupState.value = try {
                val result = fullBackupManager.exportTo(uri, languageTags)
                FullBackupState(FullBackupStatus.EXPORTED, result.preview, result)
            } catch (error: Exception) {
                Log.w("FullBackup", "Full backup export failed", error)
                FullBackupState(FullBackupStatus.ERROR)
            }
        }
    }

    fun inspectFullBackup(uri: Uri) {
        pendingRestoreUri = uri
        _fullBackupState.value = FullBackupState(FullBackupStatus.INSPECTING)
        viewModelScope.launch {
            _fullBackupState.value = try {
                FullBackupState(FullBackupStatus.PREVIEW_READY, fullBackupManager.inspect(uri))
            } catch (error: InvalidFullBackupException) {
                Log.w("FullBackup", "Invalid backup selected", error)
                pendingRestoreUri = null
                FullBackupState(FullBackupStatus.INVALID)
            } catch (error: Exception) {
                Log.w("FullBackup", "Backup inspection failed", error)
                pendingRestoreUri = null
                FullBackupState(FullBackupStatus.ERROR)
            }
        }
    }

    fun restoreInspectedFullBackup() {
        val uri = pendingRestoreUri ?: return
        _fullBackupState.value = _fullBackupState.value.copy(status = FullBackupStatus.RESTORING)
        viewModelScope.launch {
            _fullBackupState.value = try {
                val result = fullBackupManager.restore(uri)
                pendingRestoreUri = null
                _feedbackMessages.value = supportDiagnostics.feedbackMessages()
                refreshSupportState()
                FullBackupState(FullBackupStatus.RESTORED, result.preview)
            } catch (error: InvalidFullBackupException) {
                Log.w("FullBackup", "Backup restore validation failed", error)
                pendingRestoreUri = null
                FullBackupState(FullBackupStatus.INVALID)
            } catch (error: Exception) {
                Log.w("FullBackup", "Backup restore failed", error)
                pendingRestoreUri = null
                FullBackupState(FullBackupStatus.ERROR)
            }
        }
    }

    fun clearFullBackupState() {
        pendingRestoreUri = null
        _fullBackupState.value = FullBackupState()
    }

    fun addFeedbackMessage(text: String, image: Uri?) {
        viewModelScope.launch(Dispatchers.IO) {
            _feedbackMessages.value = supportDiagnostics.addFeedback(text, image)
        }
    }

    fun deleteFeedbackMessage(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _feedbackMessages.value = supportDiagnostics.deleteFeedback(id)
        }
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
            val startedAt = SystemClock.elapsedRealtime()
            var outcome = "success"
            var resultCount = 0
            try {
                val results = openFoodFacts.search(normalized, locale)
                resultCount = results.size
                _foodLookupState.value = FoodLookupState(normalized, FoodLookupStatus.SUCCESS, results)
            } catch (_: OpenFoodFactsException.RateLimited) {
                outcome = "rate-limited"
                _foodLookupState.value = FoodLookupState(normalized, FoodLookupStatus.RATE_LIMITED)
            } catch (_: OpenFoodFactsException.ServiceUnavailable) {
                outcome = "service-unavailable"
                _foodLookupState.value = FoodLookupState(normalized, FoodLookupStatus.SERVICE_UNAVAILABLE)
            } catch (cancelled: CancellationException) {
                outcome = "cancelled"
                throw cancelled
            } catch (error: Exception) {
                outcome = "error-${error.javaClass.simpleName}"
                Log.w("OpenFoodFacts", "Search failed", error)
                _foodLookupState.value = FoodLookupState(normalized, FoodLookupStatus.ERROR)
            } finally {
                val duration = SystemClock.elapsedRealtime() - startedAt
                viewModelScope.launch(Dispatchers.IO) {
                    supportDiagnostics.recordNetworkOperation(
                        operation = "open-food-facts-search",
                        durationMillis = duration,
                        outcome = outcome,
                        resultCount = resultCount,
                        queryLength = normalized.length,
                    )
                }
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
            val startedAt = SystemClock.elapsedRealtime()
            var outcome = "success"
            var resultCount = 0
            try {
                val results = listOfNotNull(openFoodFacts.productByBarcode(normalized, locale))
                resultCount = results.size
                _foodLookupState.value = FoodLookupState(
                    normalized,
                    FoodLookupStatus.SUCCESS,
                    results,
                    isBarcodeLookup = true,
                )
            } catch (_: OpenFoodFactsException.RateLimited) {
                outcome = "rate-limited"
                _foodLookupState.value = FoodLookupState(normalized, FoodLookupStatus.RATE_LIMITED, isBarcodeLookup = true)
            } catch (_: OpenFoodFactsException.ServiceUnavailable) {
                outcome = "service-unavailable"
                _foodLookupState.value = FoodLookupState(normalized, FoodLookupStatus.SERVICE_UNAVAILABLE, isBarcodeLookup = true)
            } catch (cancelled: CancellationException) {
                outcome = "cancelled"
                throw cancelled
            } catch (error: Exception) {
                outcome = "error-${error.javaClass.simpleName}"
                Log.w("OpenFoodFacts", "Barcode lookup failed", error)
                _foodLookupState.value = FoodLookupState(normalized, FoodLookupStatus.ERROR, isBarcodeLookup = true)
            } finally {
                val duration = SystemClock.elapsedRealtime() - startedAt
                viewModelScope.launch(Dispatchers.IO) {
                    supportDiagnostics.recordNetworkOperation(
                        operation = "open-food-facts-barcode",
                        durationMillis = duration,
                        outcome = outcome,
                        resultCount = resultCount,
                        queryLength = normalized.length,
                    )
                }
            }
        }
    }

    fun scanNutritionLabel(uri: Uri, suggestedName: String) {
        nutritionLabelScanJob?.cancel()
        _nutritionLabelScanState.value = NutritionLabelScanState(NutritionLabelScanStatus.PROCESSING)
        nutritionLabelScanJob = viewModelScope.launch {
            var stagedPath: String? = null
            _nutritionLabelScanState.value = try {
                val staged = supportDiagnostics.stage(uri, "nutrition-label")
                stagedPath = staged.absolutePath
                val prefill = nutritionLabelOcr.scan(Uri.fromFile(staged), suggestedName)
                if (prefill.warnings.isEmpty()) supportDiagnostics.discard(stagedPath)
                NutritionLabelScanState(
                    status = NutritionLabelScanStatus.SUCCESS,
                    prefill = prefill,
                    stagedImagePath = stagedPath.takeIf { prefill.warnings.isNotEmpty() },
                )
            } catch (error: Exception) {
                Log.w("NutritionLabelOcr", "Label scan failed", error)
                NutritionLabelScanState(NutritionLabelScanStatus.ERROR, stagedImagePath = stagedPath)
            } finally {
                if (uri.authority == "${getApplication<Application>().packageName}.fileprovider") {
                    runCatching { getApplication<Application>().contentResolver.delete(uri, null, null) }
                }
            }
        }
    }

    fun flagNutritionLabelScanForSupport() {
        val state = _nutritionLabelScanState.value
        val path = state.stagedImagePath ?: return
        if (supportDiagnostics.flag(path, "nutrition-label", "ocr-or-image-decode-failure")) {
            _nutritionLabelScanState.value = state.copy(stagedImagePath = null, flaggedForSupport = true)
            refreshSupportCount()
        }
    }

    fun clearNutritionLabelScan() {
        nutritionLabelScanJob?.cancel()
        supportDiagnostics.discard(_nutritionLabelScanState.value.stagedImagePath)
        _nutritionLabelScanState.value = NutritionLabelScanState()
    }

    fun clearSavedDiagnostics() {
        supportDiagnostics.clear()
        supportDiagnostics.clearPerformanceDiagnostics()
        refreshSupportState()
    }

    fun clearSavedFreezeReports() {
        supportDiagnostics.clearFreezes()
        refreshSupportState()
    }

    fun clearSavedCrashReports() {
        supportDiagnostics.clearCrashes()
        refreshSupportState()
    }

    fun refreshSupportState() {
        _supportExportState.value = SupportExportState(
            savedDiagnosticCount = supportDiagnostics.count(),
            savedPerformanceCount = supportDiagnostics.performanceCount(),
            savedFreezeCount = supportDiagnostics.freezeCount(),
            savedCrashCount = supportDiagnostics.crashCount(),
        )
    }

    private fun refreshSupportCount() = refreshSupportState()

    override fun onCleared() {
        nutritionLabelOcr.close()
        bodyScaleOcr.close()
        super.onCleared()
    }
}
