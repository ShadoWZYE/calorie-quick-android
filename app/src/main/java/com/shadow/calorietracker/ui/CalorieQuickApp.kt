package com.shadow.calorietracker.ui

import android.net.Uri
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatDelegate
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shadow.calorietracker.R
import com.shadow.calorietracker.model.ActivityLevel
import com.shadow.calorietracker.model.AdaptiveGoalReview
import com.shadow.calorietracker.model.AdaptiveGoalReviewer
import com.shadow.calorietracker.model.BodyMeasurement
import com.shadow.calorietracker.model.BodyMeasurementSource
import com.shadow.calorietracker.model.BodyLengthUnit
import com.shadow.calorietracker.model.Allergen
import com.shadow.calorietracker.model.AllergenDeclaration
import com.shadow.calorietracker.model.CommonMeasure
import com.shadow.calorietracker.model.EnergyEstimator
import com.shadow.calorietracker.model.Food
import com.shadow.calorietracker.model.FoodEntry
import com.shadow.calorietracker.model.FoodProvenance
import com.shadow.calorietracker.model.FoodSourceType
import com.shadow.calorietracker.model.FormulaSex
import com.shadow.calorietracker.model.FoodRecommender
import com.shadow.calorietracker.model.GoalType
import com.shadow.calorietracker.model.HistoryReportCalculator
import com.shadow.calorietracker.model.LocalizedText
import com.shadow.calorietracker.model.MacroKind
import com.shadow.calorietracker.model.Nutrition
import com.shadow.calorietracker.model.PersonalFoodDraft
import com.shadow.calorietracker.model.PersonalMeasure
import com.shadow.calorietracker.model.QuantityUsage
import com.shadow.calorietracker.model.PreparationUsage
import com.shadow.calorietracker.model.RecipeCalculator
import com.shadow.calorietracker.model.RecipeDraft
import com.shadow.calorietracker.model.RecipeIngredientDraft
import com.shadow.calorietracker.model.RecipeTemplate
import com.shadow.calorietracker.model.ReviewStatus
import com.shadow.calorietracker.model.Serving
import com.shadow.calorietracker.model.TargetMode
import com.shadow.calorietracker.model.UnitUsage
import com.shadow.calorietracker.model.UserProfile
import com.shadow.calorietracker.model.projectedMacroOverages
import com.shadow.calorietracker.data.GRAMS_UNIT_KEY
import com.shadow.calorietracker.data.NutritionLabelPrefill
import com.shadow.calorietracker.data.NutritionLabelWarning
import com.shadow.calorietracker.data.BodyScalePrefill
import com.shadow.calorietracker.data.BodyScaleWarning
import com.shadow.calorietracker.data.FeedbackMessage
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import java.text.DateFormat
import java.text.NumberFormat
import java.io.File
import java.util.Locale
import java.util.Date
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

private enum class AppScreen { TODAY, HISTORY, PROGRESS, SETTINGS, FEEDBACK, FOOD_EDITOR, RECIPE_EDITOR }

private data class ExportSelection(
    val foodIds: Set<String>,
    val recipeIds: Set<String>,
    val includeDiagnostics: Boolean,
    val includeFeedback: Boolean,
)

@Composable
fun CalorieQuickApp(viewModel: CalorieViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lookupState by viewModel.foodLookupState.collectAsStateWithLifecycle()
    val nutritionLabelScanState by viewModel.nutritionLabelScanState.collectAsStateWithLifecycle()
    val bodyScaleScanState by viewModel.bodyScaleScanState.collectAsStateWithLifecycle()
    val catalogueExportState by viewModel.catalogueExportState.collectAsStateWithLifecycle()
    val supportExportState by viewModel.supportExportState.collectAsStateWithLifecycle()
    val feedbackMessages by viewModel.feedbackMessages.collectAsStateWithLifecycle()
    val profile = state.profile
    var screenName by rememberSaveable { mutableStateOf(AppScreen.TODAY.name) }
    var editingFood by remember { mutableStateOf<Food?>(null) }
    var creatingFoodVariant by rememberSaveable { mutableStateOf(false) }
    var newFoodName by rememberSaveable { mutableStateOf("") }
    var newFoodBarcode by rememberSaveable { mutableStateOf("") }
    var nutritionLabelPrefill by remember { mutableStateOf<NutritionLabelPrefill?>(null) }
    var editingRecipe by remember { mutableStateOf<RecipeTemplate?>(null) }
    var recipeSeedFood by remember { mutableStateOf<Food?>(null) }

    LaunchedEffect(nutritionLabelScanState.status, nutritionLabelScanState.prefill) {
        if (nutritionLabelScanState.status == NutritionLabelScanStatus.SUCCESS) {
            nutritionLabelScanState.prefill?.let { prefill ->
                editingFood = null
                nutritionLabelPrefill = prefill
                newFoodName = prefill.suggestedName
                newFoodBarcode = prefill.barcode.orEmpty()
                screenName = AppScreen.FOOD_EDITOR.name
            }
        }
    }

    when {
        !state.loaded -> LoadingScreen()
        profile == null || !profile.onboardingComplete -> OnboardingScreen(viewModel::saveProfile)
        screenName == AppScreen.SETTINGS.name -> SettingsScreen(
            profile = profile,
            foods = state.foods,
            recipes = state.recipes.values.toList(),
            feedbackCount = feedbackMessages.size,
            exportState = catalogueExportState,
            supportExportState = supportExportState,
            onBack = { screenName = AppScreen.TODAY.name },
            onSave = viewModel::saveProfile,
            onExport = { uri, selection ->
                viewModel.exportReviewCatalogue(
                    uri,
                    selectedFoodIds = selection.foodIds,
                    selectedRecipeIds = selection.recipeIds,
                    includeDiagnostics = selection.includeDiagnostics,
                    includeFeedback = selection.includeFeedback,
                )
            },
            onClearSavedDiagnostics = viewModel::clearSavedDiagnostics,
        )
        screenName == AppScreen.FEEDBACK.name -> FeedbackScreen(
            messages = feedbackMessages,
            onBack = { screenName = AppScreen.TODAY.name },
            onSend = viewModel::addFeedbackMessage,
            onDelete = viewModel::deleteFeedbackMessage,
        )
        screenName == AppScreen.HISTORY.name -> HistoryScreen(
            state = state,
            onBack = { screenName = AppScreen.TODAY.name },
            onDelete = viewModel::deleteEntry,
            onAdd = viewModel::addEntry,
            onUpdate = viewModel::updateEntry,
        )
        screenName == AppScreen.PROGRESS.name -> BodyProgressScreen(
            state = state,
            scanState = bodyScaleScanState,
            onBack = { screenName = AppScreen.TODAY.name },
            onSave = viewModel::saveBodyMeasurement,
            onDelete = viewModel::deleteBodyMeasurement,
            onScan = viewModel::scanBodyScaleReport,
            onClearScan = viewModel::clearBodyScaleScan,
            onFlagFailedScan = viewModel::flagBodyScaleScanForSupport,
            onSaveProfile = viewModel::saveProfile,
        )
        screenName == AppScreen.FOOD_EDITOR.name -> PersonalFoodEditorScreen(
            initial = editingFood,
            isTemplateCopy = creatingFoodVariant,
            initialName = newFoodName,
            initialBarcode = newFoodBarcode,
            nutritionLabelPrefill = nutritionLabelPrefill,
            nutritionLabelScanState = nutritionLabelScanState,
            existingFoods = state.foods,
            onBack = {
                viewModel.clearNutritionLabelScan()
                screenName = AppScreen.TODAY.name
            },
            onSave = {
                viewModel.savePersonalFood(it)
                viewModel.clearNutritionLabelScan()
                screenName = AppScreen.TODAY.name
            },
            onArchive = { foodId ->
                viewModel.archivePersonalFood(foodId)
                viewModel.clearNutritionLabelScan()
                screenName = AppScreen.TODAY.name
            },
            onFlagScan = viewModel::flagNutritionLabelScanForSupport,
            onOpenExistingFood = {
                viewModel.clearNutritionLabelScan()
                editingFood = it
                creatingFoodVariant = false
                nutritionLabelPrefill = null
                newFoodName = ""
                newFoodBarcode = ""
            },
        )
        screenName == AppScreen.RECIPE_EDITOR.name -> RecipeEditorScreen(
            initial = editingRecipe,
            seedFood = recipeSeedFood,
            foods = state.foods,
            ingredientUsage = state.ingredientUsage,
            onBack = { screenName = AppScreen.TODAY.name },
            onSave = {
                viewModel.saveRecipe(it)
                screenName = AppScreen.TODAY.name
            },
            onArchive = { foodId ->
                viewModel.archivePersonalFood(foodId)
                screenName = AppScreen.TODAY.name
            },
        )
        else -> TodayScreen(
            state = state,
            onOpenSettings = { screenName = AppScreen.SETTINGS.name },
            onOpenHistory = { screenName = AppScreen.HISTORY.name },
            onOpenProgress = { screenName = AppScreen.PROGRESS.name },
            onOpenFeedback = { screenName = AppScreen.FEEDBACK.name },
            onAdd = { food, amount, serving, batchId, consumedAt ->
                viewModel.addEntry(food, amount, serving, batchId, consumedAt)
            },
            onDelete = viewModel::deleteEntry,
            onUpdate = viewModel::updateEntry,
            onCreateFood = {
                editingFood = null
                creatingFoodVariant = false
                nutritionLabelPrefill = null
                newFoodName = it.first
                newFoodBarcode = it.second.orEmpty()
                screenName = AppScreen.FOOD_EDITOR.name
            },
            onEditFood = {
                if (it.provenance.type == FoodSourceType.RECIPE) {
                    state.recipes[it.id]?.let { recipe ->
                        editingRecipe = recipe
                        recipeSeedFood = null
                        screenName = AppScreen.RECIPE_EDITOR.name
                    }
                } else {
                    editingFood = it
                    creatingFoodVariant = false
                    nutritionLabelPrefill = null
                    newFoodBarcode = ""
                    screenName = AppScreen.FOOD_EDITOR.name
                }
            },
            onCreateVariant = {
                editingFood = it
                creatingFoodVariant = true
                nutritionLabelPrefill = null
                newFoodName = ""
                newFoodBarcode = ""
                screenName = AppScreen.FOOD_EDITOR.name
            },
            onCreateRecipe = {
                editingRecipe = null
                recipeSeedFood = null
                screenName = AppScreen.RECIPE_EDITOR.name
            },
            onCreateRecipeFromFood = {
                editingRecipe = null
                recipeSeedFood = it
                screenName = AppScreen.RECIPE_EDITOR.name
            },
            lookupState = lookupState,
            nutritionLabelScanState = nutritionLabelScanState,
            onSearchGlobal = viewModel::searchOpenFoodFacts,
            onLookupBarcode = viewModel::lookupBarcode,
            onClearLookup = viewModel::clearFoodLookup,
            onScanNutritionLabel = viewModel::scanNutritionLabel,
            onClearNutritionLabelScan = viewModel::clearNutritionLabelScan,
            onFlagFailedNutritionScan = viewModel::flagNutritionLabelScanForSupport,
            onImportFood = {
                editingFood = it
                nutritionLabelPrefill = null
                newFoodName = ""
                newFoodBarcode = ""
                screenName = AppScreen.FOOD_EDITOR.name
            },
        )
    }
}

@Composable
private fun LoadingScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { CircularProgressIndicator() }
}

@Composable
private fun OnboardingScreen(onSave: (UserProfile) -> Unit) {
    ProfileForm(
        title = stringResource(R.string.onboarding_title),
        subtitle = stringResource(R.string.onboarding_subtitle),
        initial = null,
        actionLabel = stringResource(R.string.finish_setup),
        showLanguageIntro = true,
        onSave = onSave,
    )
}

@Composable
private fun SettingsScreen(
    profile: UserProfile,
    foods: List<Food>,
    recipes: List<RecipeTemplate>,
    feedbackCount: Int,
    exportState: CatalogueExportState,
    supportExportState: SupportExportState,
    onBack: () -> Unit,
    onSave: (UserProfile) -> Unit,
    onExport: (Uri, ExportSelection) -> Unit,
    onClearSavedDiagnostics: () -> Unit,
) {
    var showExportReview by rememberSaveable { mutableStateOf(false) }
    var pendingSelection by remember { mutableStateOf<ExportSelection?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        val selection = pendingSelection
        if (uri != null && selection != null) onExport(uri, selection)
        pendingSelection = null
    }
    ProfileForm(
        title = stringResource(R.string.settings),
        subtitle = stringResource(R.string.settings_subtitle),
        initial = profile,
        actionLabel = stringResource(R.string.save_changes),
        showLanguageIntro = false,
        autoSave = true,
        onBack = onBack,
        onSave = onSave,
        exportState = exportState,
        supportExportState = supportExportState,
        onExport = {
            showExportReview = true
        },
        onClearSavedDiagnostics = onClearSavedDiagnostics,
    )
    if (showExportReview) {
        ExportReviewDialog(
            foods = foods.filter { it.isPersonal && it.provenance.type != FoodSourceType.RECIPE },
            recipes = recipes,
            recipeFoods = foods.associateBy(Food::id),
            diagnosticCount = supportExportState.savedDiagnosticCount,
            feedbackCount = feedbackCount,
            onDismiss = { showExportReview = false },
            onContinue = { selection ->
                pendingSelection = selection
                showExportReview = false
                exportLauncher.launch("calorie-quick-review-${LocalDate.now()}.zip")
            },
        )
    }
}

@Composable
private fun ExportReviewDialog(
    foods: List<Food>,
    recipes: List<RecipeTemplate>,
    recipeFoods: Map<String, Food>,
    diagnosticCount: Int,
    feedbackCount: Int,
    onDismiss: () -> Unit,
    onContinue: (ExportSelection) -> Unit,
) {
    val locale = LocalLocale.current.platformLocale
    var selectedFoodIds by remember(foods) {
        mutableStateOf(foods.filter { it.reviewStatus == ReviewStatus.READY_FOR_REVIEW }.map(Food::id).toSet())
    }
    var selectedRecipeIds by remember(recipes) {
        mutableStateOf(recipes.filter { it.reviewStatus == ReviewStatus.READY_FOR_REVIEW }.map(RecipeTemplate::foodId).toSet())
    }
    var includeDiagnostics by remember(diagnosticCount) { mutableStateOf(diagnosticCount > 0) }
    var includeFeedback by remember(feedbackCount) { mutableStateOf(feedbackCount > 0) }
    val anythingSelected = selectedFoodIds.isNotEmpty() || selectedRecipeIds.isNotEmpty() ||
        includeDiagnostics && diagnosticCount > 0 || includeFeedback && feedbackCount > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.review_export_contents)) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Text(
                        stringResource(R.string.review_export_contents_hint),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (foods.isNotEmpty()) {
                    item { Text(stringResource(R.string.personal_foods), fontWeight = FontWeight.Bold) }
                    items(foods.sortedBy { it.name(locale) }, key = { "export-food-${it.id}" }) { food ->
                        ExportReviewRow(
                            title = food.name(locale),
                            subtitle = stringResource(
                                if (food.image?.localPath != null) R.string.includes_attached_image else R.string.nutrition_and_measures,
                            ),
                            selected = food.id in selectedFoodIds,
                            image = { FoodVisual(food, Modifier.width(48.dp).height(48.dp).clip(RoundedCornerShape(10.dp))) },
                            onToggle = { selected ->
                                selectedFoodIds = if (selected) selectedFoodIds + food.id else selectedFoodIds - food.id
                            },
                        )
                    }
                }
                if (recipes.isNotEmpty()) {
                    item { Text(stringResource(R.string.recipes), fontWeight = FontWeight.Bold) }
                    items(recipes.sortedBy(RecipeTemplate::name), key = { "export-recipe-${it.foodId}" }) { recipe ->
                        ExportReviewRow(
                            title = recipe.name,
                            subtitle = pluralStringResource(
                                R.plurals.recipe_ingredient_count,
                                recipe.ingredients.size,
                                recipe.ingredients.size,
                            ),
                            selected = recipe.foodId in selectedRecipeIds,
                            image = recipeFoods[recipe.foodId]?.let { food ->
                                { FoodVisual(food, Modifier.width(48.dp).height(48.dp).clip(RoundedCornerShape(10.dp))) }
                            },
                            onToggle = { selected ->
                                selectedRecipeIds = if (selected) selectedRecipeIds + recipe.foodId else selectedRecipeIds - recipe.foodId
                            },
                        )
                    }
                }
                item { Text(stringResource(R.string.support_data), fontWeight = FontWeight.Bold) }
                item {
                    ExportReviewRow(
                        title = stringResource(R.string.flagged_scan_images),
                        subtitle = stringResource(R.string.item_count, diagnosticCount),
                        selected = includeDiagnostics && diagnosticCount > 0,
                        enabled = diagnosticCount > 0,
                        onToggle = { includeDiagnostics = it },
                    )
                }
                item {
                    ExportReviewRow(
                        title = stringResource(R.string.feedback_thread),
                        subtitle = stringResource(R.string.message_count, feedbackCount),
                        selected = includeFeedback && feedbackCount > 0,
                        enabled = feedbackCount > 0,
                        onToggle = { includeFeedback = it },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = anythingSelected,
                onClick = {
                    onContinue(
                        ExportSelection(
                            selectedFoodIds,
                            selectedRecipeIds,
                            includeDiagnostics && diagnosticCount > 0,
                            includeFeedback && feedbackCount > 0,
                        ),
                    )
                },
            ) { Text(stringResource(R.string.continue_to_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun ExportReviewRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    enabled: Boolean = true,
    image: (@Composable () -> Unit)? = null,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) { onToggle(!selected) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Checkbox(checked = selected, onCheckedChange = onToggle, enabled = enabled)
        image?.invoke()
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PersonalFoodEditorScreen(
    initial: Food?,
    isTemplateCopy: Boolean,
    initialName: String,
    initialBarcode: String,
    nutritionLabelPrefill: NutritionLabelPrefill?,
    nutritionLabelScanState: NutritionLabelScanState,
    existingFoods: List<Food>,
    onBack: () -> Unit,
    onSave: (PersonalFoodDraft) -> Unit,
    onArchive: (String) -> Unit,
    onFlagScan: () -> Unit,
    onOpenExistingFood: (Food) -> Unit,
) {
    val context = LocalContext.current
    val locale = LocalLocale.current.platformLocale
    val variantName = initial?.let { stringResource(R.string.personal_variant_name, it.name(locale)) }.orEmpty()
    val initialPackageMeasure = initial?.takeIf(Food::isPackaged)?.servings?.firstOrNull(Serving::isPackage)
    var name by rememberSaveable(initial?.id, isTemplateCopy, initialName, nutritionLabelPrefill) {
        mutableStateOf(
            if (isTemplateCopy) variantName
            else initial?.name(locale) ?: nutritionLabelPrefill?.suggestedName ?: initialName,
        )
    }
    var brand by rememberSaveable(initial?.id) { mutableStateOf(initial?.brand.orEmpty()) }
    var barcode by rememberSaveable(initial?.id, isTemplateCopy, initialBarcode, nutritionLabelPrefill) {
        mutableStateOf(if (isTemplateCopy) "" else initial?.barcode ?: nutritionLabelPrefill?.barcode ?: initialBarcode)
    }
    var calories by rememberSaveable(initial?.id, nutritionLabelPrefill) {
        mutableStateOf(initial?.nutritionPer100g?.calories?.toString() ?: nutritionLabelPrefill?.caloriesPer100g?.toString().orEmpty())
    }
    var protein by rememberSaveable(initial?.id, nutritionLabelPrefill) {
        mutableStateOf(initial?.nutritionPer100g?.proteinGrams?.editableValue() ?: nutritionLabelPrefill?.proteinPer100g?.editableValue().orEmpty())
    }
    var carbs by rememberSaveable(initial?.id, nutritionLabelPrefill) {
        mutableStateOf(initial?.nutritionPer100g?.carbsGrams?.editableValue() ?: nutritionLabelPrefill?.carbsPer100g?.editableValue().orEmpty())
    }
    var fat by rememberSaveable(initial?.id, nutritionLabelPrefill) {
        mutableStateOf(initial?.nutritionPer100g?.fatGrams?.editableValue() ?: nutritionLabelPrefill?.fatPer100g?.editableValue().orEmpty())
    }
    var fiber by rememberSaveable(initial?.id, nutritionLabelPrefill) {
        mutableStateOf(initial?.nutritionPer100g?.fiberGrams?.editableValue() ?: nutritionLabelPrefill?.fiberPer100g?.editableValue().orEmpty())
    }
    var allergens by remember(initial?.id, nutritionLabelPrefill) {
        mutableStateOf(initial?.allergens ?: nutritionLabelPrefill?.allergens.orEmpty())
    }
    var measures by remember(initial?.id) {
        mutableStateOf(
            initial?.servings.orEmpty().filterNot { it.id == initialPackageMeasure?.id }.map {
                PersonalMeasure(it.id.takeUnless { isTemplateCopy }, it.label, it.grams, it.suggestedAmounts)
            },
        )
    }
    var isPackaged by rememberSaveable(initial?.id, nutritionLabelPrefill) {
        mutableStateOf(initial?.isPackaged == true || nutritionLabelPrefill?.packageGrams != null)
    }
    var packageLabel by rememberSaveable(initial?.id) {
        mutableStateOf(initialPackageMeasure?.label?.en?.removePrefix("1 ") ?: "package")
    }
    var packageWeight by rememberSaveable(initial?.id, nutritionLabelPrefill) {
        mutableStateOf(initialPackageMeasure?.grams?.toString() ?: nutritionLabelPrefill?.packageGrams?.toString().orEmpty())
    }
    var packageFractions by remember(initial?.id) {
        mutableStateOf(initialPackageMeasure?.suggestedAmounts?.toSet() ?: setOf(0.5, 0.25))
    }
    var measureLabel by rememberSaveable(initial?.id) { mutableStateOf("") }
    var measureGrams by rememberSaveable(initial?.id) { mutableStateOf("") }
    var selectedCommonMeasureName by rememberSaveable(initial?.id) { mutableStateOf<String?>(null) }
    var confirmArchive by remember { mutableStateOf(false) }
    var pendingImageUri by rememberSaveable(initial?.id) { mutableStateOf<String?>(null) }
    var pendingFoodCameraUri by rememberSaveable(initial?.id) { mutableStateOf<String?>(null) }
    var keepExistingImage by rememberSaveable(initial?.id) { mutableStateOf(true) }
    var shareForReview by rememberSaveable(initial?.id) {
        mutableStateOf(initial?.reviewStatus == ReviewStatus.READY_FOR_REVIEW)
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            pendingImageUri = uri.toString()
            keepExistingImage = true
        }
    }
    val foodCamera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { captured ->
        pendingFoodCameraUri?.let(Uri::parse)?.let { uri ->
            if (captured) {
                pendingImageUri = uri.toString()
                keepExistingImage = true
            } else {
                runCatching { context.contentResolver.delete(uri, null, null) }
            }
        }
        pendingFoodCameraUri = null
    }
    val shownImage = pendingImageUri ?: initial?.image?.localPath?.takeIf { keepExistingImage }
    val caloriesValue = calories.toIntOrNull()
    val proteinValue = protein.localizedDoubleOrNull()
    val carbsValue = carbs.localizedDoubleOrNull()
    val fatValue = fat.localizedDoubleOrNull()
    val fiberValue = fiber.localizedDoubleOrNull()
    val packageWeightValue = packageWeight.toIntOrNull()
    val provenance = if (isTemplateCopy) {
        FoodProvenance(FoodSourceType.PERSONAL, sourceId = initial?.let { "variant-of:${it.id}" })
    } else {
        initial?.provenance ?: FoodProvenance(
            if (nutritionLabelPrefill != null) FoodSourceType.OCR else FoodSourceType.PERSONAL,
        )
    }
    val barcodeConflict = barcode.trim().takeIf(String::isNotEmpty)?.let { candidate ->
        existingFoods.firstOrNull { it.id != initial?.id && it.barcode == candidate }
    }
    val identityConflict = name.trim().takeIf(String::isNotEmpty)?.let { candidate ->
        existingFoods.firstOrNull {
            it.id != initial?.id && it.id != barcodeConflict?.id && it.hasSameIdentity(candidate, brand)
        }
    }
    val valid = name.isNotBlank() && caloriesValue in 0..5_000 &&
        proteinValue != null && proteinValue in 0.0..100.0 &&
        carbsValue != null && carbsValue in 0.0..100.0 &&
        fatValue != null && fatValue in 0.0..100.0 &&
        (fiber.isBlank() || fiberValue != null && fiberValue in 0.0..100.0) &&
        (!isPackaged || packageLabel.isNotBlank() && packageWeightValue in 1..50_000) &&
        barcodeConflict == null

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                }
                Column {
                    Text(
                        stringResource(
                            when {
                                isTemplateCopy -> R.string.create_personal_variant
                                nutritionLabelPrefill != null -> R.string.review_scanned_label
                                initial == null -> R.string.add_personal_food
                                initial.isPersonal -> R.string.edit_personal_food
                                else -> R.string.review_imported_food
                            },
                        ),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(
                            if (nutritionLabelPrefill != null) {
                                R.string.ocr_review_notice
                            } else if (initial != null && !initial.isPersonal) {
                                R.string.open_food_facts_review_notice
                            } else {
                                R.string.personal_food_private
                            },
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (nutritionLabelPrefill != null) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                    Column(
                        Modifier.fillMaxWidth().padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(stringResource(R.string.ocr_detected_values), fontWeight = FontWeight.SemiBold)
                        nutritionLabelPrefill.warnings.forEach { warning ->
                            Text(
                                "• ${stringResource(warning.labelResource())}",
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontSize = 13.sp,
                            )
                        }
                        if (nutritionLabelScanState.stagedImagePath != null) {
                            OutlinedButton(onClick = onFlagScan) {
                                Text(stringResource(R.string.flag_failed_scan))
                            }
                        } else if (nutritionLabelScanState.flaggedForSupport) {
                            Text(
                                stringResource(R.string.scan_saved_for_support),
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
            }
        }
        if (provenance.type != FoodSourceType.PERSONAL) {
            item {
                Text(
                    buildString {
                        append(stringResource(R.string.food_source, stringResource(provenance.type.labelResource())))
                        if (provenance.locallyModified) append(" · ${stringResource(R.string.edited_locally)}")
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                )
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(18.dp),
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AddAPhoto, null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.padding(start = 10.dp)) {
                            Text(stringResource(R.string.food_photo), fontWeight = FontWeight.SemiBold)
                            Text(
                                stringResource(R.string.food_photo_add_hint),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontSize = 13.sp,
                            )
                        }
                    }
                    shownImage?.let { source ->
                        FoodImagePreview(
                            source = source,
                            modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(14.dp)),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            val directory = File(context.cacheDir, "label-captures").apply { mkdirs() }
                            val file = File.createTempFile("food-photo-", ".jpg", directory)
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            pendingFoodCameraUri = uri.toString()
                            foodCamera.launch(uri)
                        }) {
                            Icon(Icons.Default.AddAPhoto, null)
                            Text(stringResource(R.string.take_photo), Modifier.padding(start = 6.dp))
                        }
                        OutlinedButton(onClick = {
                            imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }) { Text(stringResource(R.string.choose_photo)) }
                    }
                    if (shownImage != null) {
                        TextButton(onClick = { pendingImageUri = null; keepExistingImage = false }) {
                            Text(stringResource(R.string.remove_photo))
                        }
                    }
                    Text(
                        stringResource(R.string.food_photo_private_hint),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontSize = 12.sp,
                    )
                }
            }
        }
        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.food_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = { Text(stringResource(R.string.brand_optional)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it.filter(Char::isDigit) },
                    label = { Text(stringResource(R.string.barcode_optional)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
            }
        }
        barcodeConflict?.let { conflict ->
            item {
                DuplicateFoodNotice(
                    message = stringResource(R.string.barcode_duplicate, conflict.name(locale)),
                    actionLabel = stringResource(R.string.open_existing_food),
                    isError = true,
                ) { onOpenExistingFood(conflict) }
            }
        }
        if (barcodeConflict == null) {
            identityConflict?.let { conflict ->
                item {
                    DuplicateFoodNotice(
                        message = stringResource(R.string.possible_food_duplicate, conflict.name(locale)),
                        actionLabel = stringResource(R.string.review_existing_food),
                        isError = false,
                    ) { onOpenExistingFood(conflict) }
                }
            }
        }
        item { SectionTitle(R.string.nutrition_per_100g, horizontalPadding = 0.dp) }
        item {
            NumericField(calories, { calories = it }, R.string.calories, Modifier.fillMaxWidth())
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NumericField(protein, { protein = it }, R.string.protein_g, Modifier.weight(1f), decimal = true)
                NumericField(carbs, { carbs = it }, R.string.carbs_g, Modifier.weight(1f), decimal = true)
                NumericField(fat, { fat = it }, R.string.fat_g, Modifier.weight(1f), decimal = true)
            }
        }
        item {
            NumericField(fiber, { fiber = it }, R.string.fiber_optional_g, Modifier.fillMaxWidth(), decimal = true)
        }
        item { SectionTitle(R.string.packaging, horizontalPadding = 0.dp) }
        item {
            FilterChip(
                selected = isPackaged,
                onClick = { isPackaged = !isPackaged },
                label = { Text(stringResource(R.string.prepackaged_item)) },
            )
        }
        if (isPackaged) {
            item {
                Text(stringResource(R.string.prepackaged_explanation), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = packageLabel,
                        onValueChange = { packageLabel = it },
                        label = { Text(stringResource(R.string.package_unit_name)) },
                        placeholder = { Text(stringResource(R.string.package_name_example)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    NumericField(
                        packageWeight,
                        { packageWeight = it },
                        R.string.total_package_weight,
                        Modifier.weight(1f),
                    )
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.quick_splits), fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0.5 to "½", (1.0 / 3.0) to "⅓", 0.25 to "¼").forEach { (amount, label) ->
                            FilterChip(
                                selected = packageFractions.any { kotlin.math.abs(it - amount) < 0.001 },
                                onClick = {
                                    packageFractions = if (packageFractions.any { kotlin.math.abs(it - amount) < 0.001 }) {
                                        packageFractions.filterNot { kotlin.math.abs(it - amount) < 0.001 }.toSet()
                                    } else {
                                        packageFractions + amount
                                    }
                                },
                                label = { Text(label) },
                            )
                        }
                    }
                    Text(stringResource(R.string.whole_package_always_available), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item { SectionTitle(R.string.custom_measures, horizontalPadding = 0.dp) }
        item {
            Text(stringResource(R.string.custom_measures_explanation), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                CommonMeasure.entries.forEach { commonMeasure ->
                    FilterChip(
                        selected = selectedCommonMeasureName == commonMeasure.name,
                        onClick = {
                            selectedCommonMeasureName = commonMeasure.name
                            measureLabel = commonMeasure.label.forLocale(locale)
                        },
                        label = { Text(commonMeasure.label.forLocale(locale)) },
                    )
                }
            }
        }
        items(measures, key = { it.id ?: it.label }) { measure ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("${measure.label.forLocale(locale)} · ${measure.grams} g", modifier = Modifier.weight(1f))
                    IconButton(onClick = { measures = measures - measure }) {
                        Icon(Icons.Default.Delete, stringResource(R.string.remove_measure))
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = measureLabel,
                    onValueChange = {
                        measureLabel = it
                        selectedCommonMeasureName = null
                    },
                    label = { Text(stringResource(R.string.measure_name)) },
                    placeholder = { Text(stringResource(R.string.measure_name_example)) },
                    modifier = Modifier.weight(1.35f),
                    singleLine = true,
                )
                NumericField(
                    measureGrams,
                    { measureGrams = it },
                    R.string.grams_per_unit,
                    Modifier.weight(1f),
                )
                IconButton(
                    enabled = measureLabel.isNotBlank() && measureGrams.toIntOrNull() in 1..5_000 &&
                        measures.none { measure ->
                            measure.label.all().any { it.equals(measureLabel.trim(), ignoreCase = true) }
                        },
                    onClick = {
                        val commonMeasure = selectedCommonMeasureName?.let(CommonMeasure::valueOf)
                        measures = measures + PersonalMeasure(
                            label = commonMeasure?.label ?: LocalizedText(measureLabel.trim(), measureLabel.trim()),
                            grams = requireNotNull(measureGrams.toIntOrNull()),
                        )
                        measureLabel = ""
                        measureGrams = ""
                        selectedCommonMeasureName = null
                    },
                ) {
                    Icon(Icons.Default.Add, stringResource(R.string.add_measure))
                }
            }
        }
        item { SectionTitle(R.string.allergens, horizontalPadding = 0.dp) }
        item {
            Text(stringResource(R.string.allergen_cycle_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Allergen.entries.forEach { allergen ->
                    val declaration = allergens[allergen]
                    val allergenName = stringResource(allergen.labelResource())
                    val declarationLabel = when (declaration) {
                        AllergenDeclaration.CONTAINS -> stringResource(R.string.contains)
                        AllergenDeclaration.MAY_CONTAIN -> stringResource(R.string.may_contain)
                        null -> null
                    }
                    FilterChip(
                        selected = declaration != null,
                        onClick = {
                            val next = when (declaration) {
                                null -> AllergenDeclaration.CONTAINS
                                AllergenDeclaration.CONTAINS -> AllergenDeclaration.MAY_CONTAIN
                                AllergenDeclaration.MAY_CONTAIN -> null
                            }
                            allergens = allergens.toMutableMap().apply {
                                if (next == null) remove(allergen) else put(allergen, next)
                            }
                        },
                        label = {
                            Text(allergenName + declarationLabel?.let { " · $it" }.orEmpty())
                        },
                    )
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = shareForReview,
                    onClick = { shareForReview = !shareForReview },
                    label = { Text(stringResource(R.string.include_in_review_export)) },
                )
                Text(stringResource(R.string.review_export_opt_in_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Button(
                enabled = valid,
                onClick = {
                    onSave(
                        PersonalFoodDraft(
                            id = initial?.id.takeUnless { isTemplateCopy },
                            name = name.trim(),
                            brand = brand,
                            barcode = barcode,
                            nutritionPer100g = Nutrition(
                                calories = requireNotNull(caloriesValue),
                                proteinGrams = requireNotNull(proteinValue),
                                carbsGrams = requireNotNull(carbsValue),
                                fatGrams = requireNotNull(fatValue),
                                fiberGrams = if (fiber.isBlank()) null else fiberValue,
                            ),
                            allergens = allergens,
                            measures = measures + if (isPackaged) {
                                listOf(
                                    PersonalMeasure(
                                        id = initialPackageMeasure?.id.takeUnless { isTemplateCopy },
                                        label = LocalizedText(packageLabel.trim(), packageLabel.trim()),
                                        grams = requireNotNull(packageWeightValue),
                                        suggestedAmounts = packageFractions.toList(),
                                        isPackage = true,
                                    ),
                                )
                            } else {
                                emptyList()
                            },
                            isPackaged = isPackaged,
                            provenance = provenance,
                            image = initial?.image?.takeIf { keepExistingImage },
                            pendingImageUri = pendingImageUri,
                            reviewStatus = if (shareForReview) ReviewStatus.READY_FOR_REVIEW else ReviewStatus.PRIVATE,
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.save_food)) }
        }
        if (initial?.isPersonal == true && !isTemplateCopy) {
            item {
                OutlinedButton(onClick = { confirmArchive = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.archive_food), color = MaterialTheme.colorScheme.error)
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    if (confirmArchive && initial != null) {
        AlertDialog(
            onDismissRequest = { confirmArchive = false },
            title = { Text(stringResource(R.string.archive_food)) },
            text = { Text(stringResource(R.string.archive_food_confirmation)) },
            confirmButton = {
                TextButton(onClick = { onArchive(initial.id) }) { Text(stringResource(R.string.archive)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmArchive = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun RecipeEditorScreen(
    initial: RecipeTemplate?,
    seedFood: Food?,
    foods: List<Food>,
    ingredientUsage: Map<String, Int>,
    onBack: () -> Unit,
    onSave: (RecipeDraft) -> Unit,
    onArchive: (String) -> Unit,
) {
    val locale = LocalLocale.current.platformLocale
    val seedGrams = seedFood?.servings?.firstOrNull()?.grams ?: 100
    val seedRecipeName = seedFood?.let { stringResource(R.string.food_recipe_name, it.name(locale)) }.orEmpty()
    var name by rememberSaveable(initial?.activeBatchId, seedFood?.id) {
        mutableStateOf(initial?.name ?: seedRecipeName)
    }
    var ingredientQuery by rememberSaveable(initial?.activeBatchId, seedFood?.id) { mutableStateOf("") }
    var ingredients by remember(initial?.activeBatchId, seedFood?.id) {
        mutableStateOf(
            initial?.ingredients.orEmpty().map { snapshot ->
                foods.firstOrNull { it.id == snapshot.foodId }?.let { current ->
                    RecipeIngredientDraft(
                        current.id,
                        current.names,
                        current.nutritionPer100g,
                        current.allergens,
                        snapshot.grams,
                    )
                } ?: snapshot
            }.ifEmpty {
                listOfNotNull(seedFood?.let { food ->
                    RecipeIngredientDraft(food.id, food.names, food.nutritionPer100g, food.allergens, seedGrams)
                })
            },
        )
    }
    var cookedYield by rememberSaveable(initial?.activeBatchId, seedFood?.id) {
        mutableStateOf(initial?.cookedYieldGrams?.toString() ?: seedFood?.let { seedGrams.toString() }.orEmpty())
    }
    var portionCount by rememberSaveable(initial?.activeBatchId, seedFood?.id) {
        mutableStateOf(initial?.portionCount?.toString() ?: if (seedFood != null) "1" else "4")
    }
    var yieldManuallyEdited by rememberSaveable(initial?.activeBatchId) { mutableStateOf(initial != null) }
    var confirmArchive by remember { mutableStateOf(false) }
    var shareForReview by rememberSaveable(initial?.activeBatchId) {
        mutableStateOf(initial?.reviewStatus == ReviewStatus.READY_FOR_REVIEW)
    }
    val cookedYieldValue = cookedYield.toIntOrNull()
    val portionCountValue = portionCount.toIntOrNull()
    val rawIngredientWeight = ingredients.sumOf(RecipeIngredientDraft::grams)
    val calculation = if (
        ingredients.isNotEmpty() && ingredients.all { it.grams in 1..100_000 } &&
        cookedYieldValue in 1..100_000 && portionCountValue in 1..1_000
    ) {
        RecipeCalculator.calculate(ingredients, requireNotNull(cookedYieldValue), requireNotNull(portionCountValue))
    } else {
        null
    }
    val ingredientCandidates = foods.asSequence()
        .filter { it.provenance.type != FoodSourceType.RECIPE }
        .filter { food -> ingredients.none { it.foodId == food.id } }
    val searchResults = if (ingredientQuery.isBlank()) {
        ingredientCandidates.filter { ingredientUsage[it.id].orZero() > 0 }
            .sortedByDescending { ingredientUsage[it.id].orZero() }
            .take(6)
            .toList()
    } else {
        ingredientCandidates.filter { it.matches(ingredientQuery) }
            .sortedWith(compareByDescending<Food> { ingredientUsage[it.id].orZero() }.thenBy { it.names.en })
            .take(6)
            .toList()
    }
    val valid = name.isNotBlank() && calculation != null

    fun updateIngredients(updated: List<RecipeIngredientDraft>) {
        ingredients = updated
        if (!yieldManuallyEdited) {
            cookedYield = updated.sumOf(RecipeIngredientDraft::grams).takeIf { it > 0 }?.toString().orEmpty()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                }
                Column {
                    Text(
                        stringResource(if (initial == null) R.string.create_recipe else R.string.cook_recipe_again),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(R.string.recipe_version_explanation),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.recipe_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        item { SectionTitle(R.string.ingredients, horizontalPadding = 0.dp) }
        item {
            Text(stringResource(R.string.recipe_ingredient_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            OutlinedTextField(
                value = ingredientQuery,
                onValueChange = { ingredientQuery = it },
                label = { Text(stringResource(R.string.search_ingredients)) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = if (ingredientQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { ingredientQuery = "" }) {
                            Icon(Icons.Default.Close, stringResource(R.string.close))
                        }
                    }
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        if (ingredientQuery.isBlank() && searchResults.isNotEmpty()) {
            item {
                Text(
                    stringResource(R.string.frequent_ingredients),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(searchResults, key = { "recipe-search-${it.id}" }) { food ->
            Card(
                onClick = {
                    updateIngredients(
                        ingredients + RecipeIngredientDraft(
                            food.id,
                            food.names,
                            food.nutritionPer100g,
                            food.allergens,
                            food.servings.firstOrNull()?.grams ?: 100,
                        ),
                    )
                    ingredientQuery = ""
                },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    FoodVisual(
                        food,
                        Modifier.width(62.dp).height(62.dp).clip(RoundedCornerShape(12.dp)),
                    )
                    Column(Modifier.weight(1f)) {
                        Text(food.name(locale), fontWeight = FontWeight.SemiBold, maxLines = 3)
                        Text(
                            "${food.nutritionPer100g.calories} ${stringResource(R.string.kcal)} · ${stringResource(R.string.per_100g)}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                        )
                        ingredientUsage[food.id]?.takeIf { it > 0 }?.let { uses ->
                            Text(
                                pluralStringResource(R.plurals.used_in_recipe_batches, uses, uses),
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                            )
                        }
                    }
                    Icon(Icons.Default.Add, stringResource(R.string.add_ingredient), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        if (ingredientQuery.isNotBlank() && searchResults.isEmpty()) {
            item { Text(stringResource(R.string.no_saved_ingredient), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(ingredients, key = { "recipe-ingredient-${it.foodId}" }) { ingredient ->
            RecipeIngredientRow(
                ingredient = ingredient,
                food = foods.firstOrNull { it.id == ingredient.foodId },
                locale = locale,
                onGramsChange = { grams ->
                    updateIngredients(
                        ingredients.map { current ->
                            if (current.foodId == ingredient.foodId) current.copy(grams = grams) else current
                        },
                    )
                },
                onRemove = { updateIngredients(ingredients - ingredient) },
            )
        }
        if (ingredients.isEmpty()) {
            item { Text(stringResource(R.string.no_recipe_ingredients), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        item { SectionTitle(R.string.batch_yield, horizontalPadding = 0.dp) }
        item {
            Text(stringResource(R.string.batch_yield_explanation), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NumericField(
                    cookedYield,
                    {
                        cookedYield = it
                        yieldManuallyEdited = true
                    },
                    R.string.finished_weight_g,
                    Modifier.weight(1f),
                )
                NumericField(portionCount, { portionCount = it }, R.string.number_of_portions, Modifier.weight(1f))
            }
        }
        if (rawIngredientWeight > 0) {
            item {
                Text(
                    stringResource(R.string.raw_ingredient_weight, rawIngredientWeight),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                )
            }
        }
        calculation?.let { result ->
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.recipe_nutrition_preview), fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(
                                R.string.recipe_batch_summary,
                                result.totalNutrition.calories,
                                result.nutritionPer100g.calories,
                                result.portionGrams,
                                result.nutritionPer100g.forGrams(result.portionGrams).calories,
                            ),
                        )
                        Text(
                            stringResource(
                                R.string.recipe_macro_summary,
                                result.nutritionPer100g.proteinGrams,
                                result.nutritionPer100g.carbsGrams,
                                result.nutritionPer100g.fatGrams,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                        )
                        if (result.nutritionPer100g.fiberGrams == null) {
                            Text(
                                stringResource(R.string.recipe_fiber_incomplete),
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(
                    selected = shareForReview,
                    onClick = { shareForReview = !shareForReview },
                    label = { Text(stringResource(R.string.include_in_review_export)) },
                )
                Text(
                    stringResource(R.string.review_export_opt_in_hint),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
        }
        item {
            Button(
                enabled = valid,
                onClick = {
                    onSave(
                        RecipeDraft(
                            id = initial?.foodId,
                            name = name.trim(),
                            ingredients = ingredients,
                            cookedYieldGrams = requireNotNull(cookedYieldValue),
                            portionCount = requireNotNull(portionCountValue),
                            reviewStatus = if (shareForReview) ReviewStatus.READY_FOR_REVIEW else ReviewStatus.PRIVATE,
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(if (initial == null) R.string.save_recipe else R.string.save_new_batch)) }
        }
        if (initial != null && initial.batches.isNotEmpty()) {
            item { SectionTitle(R.string.batch_history, horizontalPadding = 0.dp) }
            items(initial.batches, key = { it.id }) { batch ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale)
                                    .format(Date(batch.cookedAtEpochMillis)),
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                stringResource(R.string.batch_yield_kcal, batch.cookedYieldGrams, batch.nutritionPer100g.calories),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            stringResource(R.string.grams_remaining, batch.remainingGrams),
                            color = if (batch.remainingGrams > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
        if (initial != null) {
            item {
                OutlinedButton(onClick = { confirmArchive = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.archive_recipe), color = MaterialTheme.colorScheme.error)
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    if (confirmArchive && initial != null) {
        AlertDialog(
            onDismissRequest = { confirmArchive = false },
            title = { Text(stringResource(R.string.archive_recipe)) },
            text = { Text(stringResource(R.string.archive_recipe_confirmation)) },
            confirmButton = {
                TextButton(onClick = { onArchive(initial.foodId) }) { Text(stringResource(R.string.archive)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmArchive = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecipeIngredientRow(
    ingredient: RecipeIngredientDraft,
    food: Food?,
    locale: Locale,
    onGramsChange: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    val gramsLabel = stringResource(R.string.grams_short)
    val choices = food?.let { buildUnitChoices(it, emptyList(), locale, gramsLabel) }
        ?: listOf(UnitChoice(GRAMS_UNIT_KEY, gramsLabel, null))
    val initialChoice = choices.firstOrNull { choice ->
        choice.serving?.let { serving ->
            ingredient.grams % serving.grams == 0 && ingredient.grams / serving.grams in 1..100
        } == true
    } ?: choices.first { it.key == GRAMS_UNIT_KEY }
    var selectedKey by remember(ingredient.foodId) { mutableStateOf(initialChoice.key) }
    var amountText by remember(ingredient.foodId) {
        mutableStateOf(
            formatEditableAmount(ingredient.grams / (initialChoice.serving?.grams ?: 1).toDouble()),
        )
    }
    var unitMenuOpen by remember(ingredient.foodId) { mutableStateOf(false) }
    val selected = choices.firstOrNull { it.key == selectedKey } ?: choices.last()

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                food?.let {
                    FoodVisual(
                        it,
                        Modifier.width(60.dp).height(60.dp).clip(RoundedCornerShape(12.dp)),
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(ingredient.foodName.forLocale(locale), fontWeight = FontWeight.SemiBold, maxLines = 3)
                    Text(
                        "${ingredient.nutritionPer100g.calories} ${stringResource(R.string.kcal)} · ${stringResource(R.string.per_100g)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                    )
                }
            }
            Text(
                stringResource(
                    R.string.recipe_ingredient_amount_summary,
                    ingredient.nutritionPer100g.forGrams(ingredient.grams).calories,
                    ingredient.grams,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { candidate ->
                        val filtered = candidate.filter { it.isDigit() || it == '.' || it == ',' }
                        if (filtered.count { it == '.' || it == ',' } <= 1) {
                            amountText = filtered
                            val amount = filtered.localizedDoubleOrNull()
                            onGramsChange(amount?.let { (it * (selected.serving?.grams ?: 1)).roundToInt() } ?: 0)
                        }
                    },
                    label = { Text(stringResource(R.string.amount)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.width(110.dp),
                    singleLine = true,
                )
                Box {
                    OutlinedButton(
                        onClick = { unitMenuOpen = true },
                        modifier = Modifier.heightIn(min = 56.dp).widthIn(min = 120.dp),
                    ) { Text(selected.label, maxLines = 2) }
                    DropdownMenu(expanded = unitMenuOpen, onDismissRequest = { unitMenuOpen = false }) {
                        choices.forEach { choice ->
                            DropdownMenuItem(
                                text = { Text(choice.label) },
                                onClick = {
                                    selectedKey = choice.key
                                    amountText = formatEditableAmount(
                                        ingredient.grams / (choice.serving?.grams ?: 1).toDouble(),
                                    )
                                    unitMenuOpen = false
                                },
                            )
                        }
                    }
                }
                IconButton(onClick = onRemove, modifier = Modifier.heightIn(min = 56.dp)) {
                    Icon(Icons.Default.Delete, stringResource(R.string.remove_ingredient))
                }
            }
        }
    }
}

private fun Double.editableValue(): String =
    if (this % 1.0 == 0.0) toInt().toString() else toString().trimEnd('0').trimEnd('.')

private fun String.localizedDoubleOrNull(): Double? = replace(',', '.').toDoubleOrNull()

private fun Allergen.labelResource() = when (this) {
    Allergen.GLUTEN -> R.string.allergen_gluten
    Allergen.CRUSTACEANS -> R.string.allergen_crustaceans
    Allergen.EGGS -> R.string.allergen_eggs
    Allergen.FISH -> R.string.allergen_fish
    Allergen.PEANUTS -> R.string.allergen_peanuts
    Allergen.SOY -> R.string.allergen_soy
    Allergen.MILK -> R.string.allergen_milk
    Allergen.NUTS -> R.string.allergen_nuts
    Allergen.CELERY -> R.string.allergen_celery
    Allergen.MUSTARD -> R.string.allergen_mustard
    Allergen.SESAME -> R.string.allergen_sesame
    Allergen.SULPHITES -> R.string.allergen_sulphites
    Allergen.LUPIN -> R.string.allergen_lupin
    Allergen.MOLLUSCS -> R.string.allergen_molluscs
}

@Composable
private fun ProfileForm(
    title: String,
    subtitle: String,
    initial: UserProfile?,
    actionLabel: String,
    showLanguageIntro: Boolean,
    autoSave: Boolean = false,
    onSave: (UserProfile) -> Unit,
    onBack: (() -> Unit)? = null,
    exportState: CatalogueExportState? = null,
    onExport: (() -> Unit)? = null,
    supportExportState: SupportExportState? = null,
    onClearSavedDiagnostics: (() -> Unit)? = null,
) {
    var displayName by rememberSaveable { mutableStateOf(initial?.displayName.orEmpty()) }
    var age by rememberSaveable { mutableStateOf((initial?.age ?: 30).toString()) }
    var height by rememberSaveable { mutableStateOf((initial?.heightCm ?: 175).toString()) }
    var weight by rememberSaveable { mutableStateOf((initial?.weightKg ?: 75.0).toString().trimEnd('0').trimEnd('.')) }
    var sexName by rememberSaveable { mutableStateOf((initial?.formulaSex ?: FormulaSex.FEMALE).name) }
    var activityName by rememberSaveable { mutableStateOf((initial?.activityLevel ?: ActivityLevel.LIGHT).name) }
    var goalName by rememberSaveable { mutableStateOf((initial?.goalType ?: GoalType.MAINTAIN).name) }
    var targetModeName by rememberSaveable { mutableStateOf((initial?.targetMode ?: TargetMode.ESTIMATED).name) }
    var bodyLengthUnitName by rememberSaveable {
        mutableStateOf((initial?.bodyLengthUnit ?: BodyLengthUnit.CENTIMETERS).name)
    }
    val defaultTargets = initial ?: UserProfile(
        onboardingComplete = false,
        age = 30,
        heightCm = 175,
        weightKg = 75.0,
        formulaSex = FormulaSex.FEMALE,
        activityLevel = ActivityLevel.LIGHT,
        goalType = GoalType.MAINTAIN,
        targetMode = TargetMode.ESTIMATED,
        calorieGoal = 2_108,
        proteinGoalGrams = 120,
        carbsGoalGrams = 272,
        fatGoalGrams = 60,
    )
    var calorieTarget by rememberSaveable { mutableStateOf(defaultTargets.calorieGoal.toString()) }
    var proteinTarget by rememberSaveable { mutableStateOf(defaultTargets.proteinGoalGrams.toString()) }
    var carbsTarget by rememberSaveable { mutableStateOf(defaultTargets.carbsGoalGrams.toString()) }
    var fatTarget by rememberSaveable { mutableStateOf(defaultTargets.fatGoalGrams.toString()) }
    var fiberTarget by rememberSaveable { mutableStateOf(defaultTargets.fiberGoalGrams.toString()) }
    var confirmClearDiagnostics by rememberSaveable { mutableStateOf(false) }

    val ageValue = age.toIntOrNull()
    val heightValue = height.toIntOrNull()
    val weightValue = weight.replace(',', '.').toDoubleOrNull()
    val measurementsValid = ageValue in 18..100 && heightValue in 120..230 &&
        weightValue != null && weightValue in 35.0..300.0
    val validatedAge = ageValue ?: 0
    val validatedHeight = heightValue ?: 0
    val validatedWeight = weightValue ?: 0.0
    val sex = FormulaSex.valueOf(sexName)
    val activity = ActivityLevel.valueOf(activityName)
    val goal = GoalType.valueOf(goalName)
    val targetMode = TargetMode.valueOf(targetModeName)
    val bodyLengthUnit = BodyLengthUnit.valueOf(bodyLengthUnitName)
    val estimatedTargets = if (measurementsValid) {
        EnergyEstimator.dailyTargets(validatedAge, validatedHeight, validatedWeight, sex, activity, goal)
    } else null
    val customCalories = calorieTarget.toIntOrNull()
    val customProtein = proteinTarget.toIntOrNull()
    val customCarbs = carbsTarget.toIntOrNull()
    val customFat = fatTarget.toIntOrNull()
    val customFiber = fiberTarget.toIntOrNull()
    val customTargetsValid = customCalories in 500..10_000 && customProtein in 1..1_000 &&
        customCarbs in 1..1_000 && customFat in 1..1_000 && customFiber in 1..200
    val valid = measurementsValid && (targetMode == TargetMode.ESTIMATED || customTargetsValid)
    val updateMacrosAndCalories: (String, String, String) -> Unit = { newProtein, newCarbs, newFat ->
        proteinTarget = newProtein
        carbsTarget = newCarbs
        fatTarget = newFat
        val parsedProtein = newProtein.toIntOrNull()
        val parsedCarbs = newCarbs.toIntOrNull()
        val parsedFat = newFat.toIntOrNull()
        if (parsedProtein != null && parsedCarbs != null && parsedFat != null) {
            calorieTarget = EnergyEstimator.caloriesForMacros(parsedProtein, parsedCarbs, parsedFat).toString()
        }
    }
    val profileDraft = if (valid) {
        val targets = if (targetMode == TargetMode.ESTIMATED) {
            requireNotNull(estimatedTargets)
        } else {
            com.shadow.calorietracker.model.DailyTargets(
                requireNotNull(customCalories),
                requireNotNull(customProtein),
                requireNotNull(customCarbs),
                requireNotNull(customFat),
                requireNotNull(customFiber),
            )
        }
        UserProfile(
            onboardingComplete = true,
            age = validatedAge,
            heightCm = validatedHeight,
            weightKg = validatedWeight,
            formulaSex = sex,
            activityLevel = activity,
            goalType = goal,
            calorieGoal = targets.calories,
            proteinGoalGrams = targets.proteinGrams,
            carbsGoalGrams = targets.carbsGrams,
            fatGoalGrams = targets.fatGrams,
            fiberGoalGrams = targets.fiberGrams,
            targetMode = targetMode,
            displayName = displayName.trim(),
            lastGoalReviewAtEpochMillis = initial?.lastGoalReviewAtEpochMillis,
            bodyLengthUnit = bodyLengthUnit,
        )
    } else null

    LaunchedEffect(autoSave, profileDraft) {
        if (autoSave && profileDraft != null && profileDraft != initial) {
            delay(450)
            onSave(profileDraft)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                onBack?.let {
                    IconButton(onClick = it) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
                Column {
                    Text(title, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item { LanguageSelector(showLanguageIntro) }
        item {
            OutlinedTextField(
                value = displayName,
                onValueChange = { if (it.length <= 50) displayName = it },
                label = { Text(stringResource(R.string.display_name_optional)) },
                supportingText = { Text(stringResource(R.string.display_name_private)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        item { SectionTitle(R.string.your_measurements, horizontalPadding = 0.dp) }
        item {
            Text(stringResource(R.string.formula_explanation), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NumericField(age, { age = it }, R.string.age, Modifier.weight(1f))
                NumericField(height, { height = it }, R.string.height_cm, Modifier.weight(1f))
                NumericField(weight, { weight = it }, R.string.weight_kg, Modifier.weight(1f), decimal = true)
            }
        }
        item {
            ChoiceRow(
                values = FormulaSex.entries,
                selected = sex,
                label = { stringResource(if (it == FormulaSex.FEMALE) R.string.female else R.string.male) },
                onSelect = { sexName = it.name },
            )
        }
        item { SectionTitle(R.string.tape_measurement_unit, horizontalPadding = 0.dp) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.tape_measurement_unit_hint),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ChoiceRow(
                    values = BodyLengthUnit.entries,
                    selected = bodyLengthUnit,
                    label = { it.symbol },
                    onSelect = { bodyLengthUnitName = it.name },
                )
            }
        }
        item { SectionTitle(R.string.activity, horizontalPadding = 0.dp) }
        item {
            ChoiceRow(
                values = ActivityLevel.entries,
                selected = activity,
                label = { stringResource(it.labelResource()) },
                onSelect = { activityName = it.name },
            )
        }
        item { SectionTitle(R.string.goal, horizontalPadding = 0.dp) }
        item {
            ChoiceRow(
                values = GoalType.entries,
                selected = goal,
                label = { stringResource(it.labelResource()) },
                onSelect = { goalName = it.name },
            )
        }
        item { SectionTitle(R.string.daily_targets, horizontalPadding = 0.dp) }
        item {
            ChoiceRow(
                values = TargetMode.entries,
                selected = targetMode,
                label = { stringResource(if (it == TargetMode.ESTIMATED) R.string.estimated else R.string.custom) },
                onSelect = { targetModeName = it.name },
            )
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (targetMode == TargetMode.ESTIMATED) {
                        Text(stringResource(R.string.estimated_daily_goal), fontWeight = FontWeight.SemiBold)
                        Text(
                            "${estimatedTargets?.calories ?: 0} ${stringResource(R.string.kcal)}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        estimatedTargets?.let {
                            Text(
                                stringResource(
                                    R.string.macro_target_summary,
                                    it.proteinGrams,
                                    it.carbsGrams,
                                    it.fatGrams,
                                    it.fiberGrams,
                                ),
                            )
                        }
                        Text(
                            stringResource(R.string.estimate_disclaimer),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(stringResource(R.string.custom_target_explanation), fontWeight = FontWeight.SemiBold)
                        NumericField(
                            calorieTarget,
                            { candidate ->
                                calorieTarget = candidate
                                val newCalories = candidate.toIntOrNull()
                                val currentProtein = proteinTarget.toIntOrNull()
                                val currentCarbs = carbsTarget.toIntOrNull()
                                val currentFat = fatTarget.toIntOrNull()
                                if (newCalories in 500..10_000 && currentProtein != null &&
                                    currentCarbs != null && currentFat != null
                                ) {
                                    EnergyEstimator.redistributeMacros(
                                        requireNotNull(newCalories),
                                        currentProtein,
                                        currentCarbs,
                                        currentFat,
                                    )?.let { balanced ->
                                        proteinTarget = balanced.proteinGrams.toString()
                                        carbsTarget = balanced.carbsGrams.toString()
                                        fatTarget = balanced.fatGrams.toString()
                                    }
                                }
                            },
                            R.string.calorie_target,
                            Modifier.fillMaxWidth(),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            NumericField(
                                proteinTarget,
                                { updateMacrosAndCalories(it, carbsTarget, fatTarget) },
                                R.string.protein_g,
                                Modifier.weight(1f),
                            )
                            NumericField(
                                carbsTarget,
                                { updateMacrosAndCalories(proteinTarget, it, fatTarget) },
                                R.string.carbs_g,
                                Modifier.weight(1f),
                            )
                            NumericField(
                                fatTarget,
                                { updateMacrosAndCalories(proteinTarget, carbsTarget, it) },
                                R.string.fat_g,
                                Modifier.weight(1f),
                            )
                        }
                        NumericField(fiberTarget, { fiberTarget = it }, R.string.fiber_g, Modifier.fillMaxWidth())
                        if (!customTargetsValid) {
                            Text(stringResource(R.string.invalid_custom_targets), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
        if (onExport != null && exportState != null) {
            item { SectionTitle(R.string.personal_catalogue_export, horizontalPadding = 0.dp) }
            item {
                Text(
                    stringResource(R.string.personal_catalogue_export_explanation),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                Text(
                    stringResource(R.string.feedback_thread_export_hint),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(
                        R.string.saved_scan_diagnostics,
                        supportExportState?.savedDiagnosticCount ?: 0,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                )
            }
            if ((supportExportState?.savedDiagnosticCount ?: 0) > 0 && onClearSavedDiagnostics != null) {
                item {
                    TextButton(onClick = { confirmClearDiagnostics = true }) {
                        Text(stringResource(R.string.delete_saved_diagnostics))
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = onExport,
                    enabled = exportState.status != CatalogueExportStatus.EXPORTING,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(
                            if (exportState.status == CatalogueExportStatus.EXPORTING) {
                                R.string.exporting_review_items
                            } else {
                                R.string.export_review_items
                            },
                        ),
                    )
                }
            }
            when (exportState.status) {
                CatalogueExportStatus.SUCCESS -> item {
                    val result = exportState.result
                    Text(
                        if (result == null ||
                            result.itemCount == 0 && result.diagnosticCount == 0 && !result.hasFeedback
                        ) {
                            stringResource(R.string.catalogue_export_empty)
                        } else {
                            stringResource(
                                R.string.review_bundle_export_success,
                                result.itemCount,
                                result.imageCount,
                                result.diagnosticCount,
                            )
                        },
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                CatalogueExportStatus.ERROR -> item {
                    Text(stringResource(R.string.catalogue_export_error), color = MaterialTheme.colorScheme.error)
                }
                else -> Unit
            }
        }
        if (!autoSave) item {
            Button(
                enabled = valid,
                onClick = {
                    profileDraft?.let(onSave)
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(actionLabel) }
        }
        if (autoSave) item {
            Text(
                stringResource(if (valid) R.string.settings_auto_saved else R.string.settings_waiting_for_valid_values),
                modifier = Modifier.fillMaxWidth(),
                color = if (valid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
    if (confirmClearDiagnostics && onClearSavedDiagnostics != null) {
        AlertDialog(
            onDismissRequest = { confirmClearDiagnostics = false },
            title = { Text(stringResource(R.string.delete_saved_diagnostics)) },
            text = { Text(stringResource(R.string.delete_saved_diagnostics_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    onClearSavedDiagnostics()
                    confirmClearDiagnostics = false
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearDiagnostics = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun LanguageSelector(showIntro: Boolean) {
    val currentLanguage = LocalLocale.current.language
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.language), fontWeight = FontWeight.Bold, fontSize = 18.sp)
        if (showIntro) Text(stringResource(R.string.language_intro), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilterChip(selected = currentLanguage != "ro", onClick = { setLanguage("en") }, label = { Text("English") })
            FilterChip(selected = currentLanguage == "ro", onClick = { setLanguage("ro") }, label = { Text("Română") })
        }
    }
}

private fun setLanguage(tag: String) {
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
}

@Composable
private fun NumericField(
    value: String,
    onChange: (String) -> Unit,
    label: Int,
    modifier: Modifier,
    decimal: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { text ->
            val filtered = text.filter { it.isDigit() || decimal && (it == '.' || it == ',') }
            onChange(if (decimal) filtered.withSingleDecimalSeparator() else filtered)
        },
        label = { Text(stringResource(label)) },
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        singleLine = true,
        modifier = modifier,
    )
}

@Composable
private fun NumericField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier,
    decimal: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { text ->
            val filtered = text.filter { it.isDigit() || decimal && (it == '.' || it == ',') }
            onChange(if (decimal) filtered.withSingleDecimalSeparator() else filtered)
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        singleLine = true,
        modifier = modifier,
    )
}

private fun String.withSingleDecimalSeparator(): String {
    var separatorSeen = false
    return filter { character ->
        if (character == '.' || character == ',') {
            if (separatorSeen) false else true.also { separatorSeen = true }
        } else {
            true
        }
    }
}

@Composable
private fun <T> ChoiceRow(values: List<T>, selected: T, label: @Composable (T) -> String, onSelect: (T) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        items(values) { value ->
            FilterChip(selected = value == selected, onClick = { onSelect(value) }, label = { Text(label(value)) })
        }
    }
}

private fun ActivityLevel.labelResource() = when (this) {
    ActivityLevel.SEDENTARY -> R.string.sedentary
    ActivityLevel.LIGHT -> R.string.light_activity
    ActivityLevel.MODERATE -> R.string.moderate_activity
    ActivityLevel.ACTIVE -> R.string.active
}

private fun GoalType.labelResource() = when (this) {
    GoalType.LOSE -> R.string.lose
    GoalType.MAINTAIN -> R.string.maintain
    GoalType.GAIN -> R.string.gain
}

@Composable
private fun BodyProgressScreen(
    state: AppUiState,
    scanState: BodyScaleScanState,
    onBack: () -> Unit,
    onSave: (BodyMeasurement) -> Unit,
    onDelete: (BodyMeasurement) -> Unit,
    onScan: (Uri) -> Unit,
    onClearScan: () -> Unit,
    onFlagFailedScan: () -> Unit,
    onSaveProfile: (UserProfile) -> Unit,
) {
    val locale = LocalLocale.current.platformLocale
    val profile = requireNotNull(state.profile)
    var editorInitial by remember { mutableStateOf<BodyMeasurement?>(null) }
    var editorWarnings by remember { mutableStateOf<Set<BodyScaleWarning>>(emptySet()) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(onScan)
    }
    LaunchedEffect(scanState.status, scanState.prefill) {
        if (scanState.status == BodyScaleScanStatus.SUCCESS) {
            scanState.prefill?.measurement?.let {
                editorInitial = it
                editorWarnings = scanState.prefill.warnings
                onClearScan()
            }
        }
    }
    val latest = state.bodyMeasurements.firstOrNull()
    val previous = state.bodyMeasurements.getOrNull(1)
    val change = if (latest != null && previous != null) latest.weightKg - previous.weightKg else null
    val adaptiveReview = AdaptiveGoalReviewer.review(
        profile = profile,
        measurements = state.bodyMeasurements,
        loggedDates = state.historyDays.map { it.date }.toSet(),
        zoneId = ZoneId.systemDefault(),
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(start = 8.dp, end = 12.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                }
                Column {
                    Text(stringResource(R.string.body_progress), fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Text(
                        profile.displayName.takeIf(String::isNotBlank)?.let { stringResource(R.string.progress_for_name, it) }
                            ?: stringResource(R.string.body_progress_subtitle),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item {
            Card(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(24.dp),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.latest_check_in), fontWeight = FontWeight.SemiBold)
                    if (latest == null) {
                        Text(stringResource(R.string.no_body_check_ins), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(formatAmount(latest.weightKg, locale), fontSize = 38.sp, fontWeight = FontWeight.Bold)
                            Text(" ${stringResource(R.string.kg)}", Modifier.padding(bottom = 6.dp))
                            Spacer(Modifier.weight(1f))
                            change?.let {
                                val favorable = when (profile.goalType) {
                                    GoalType.LOSE -> it < 0
                                    GoalType.GAIN -> it > 0
                                    GoalType.MAINTAIN -> abs(it) <= 0.2
                                }
                                Text(
                                    "${if (it > 0) "+" else ""}${formatAmount(it, locale)} ${stringResource(R.string.kg)}",
                                    color = if (favorable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                        Text(
                            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale)
                                .format(Date(latest.measuredAtEpochMillis)),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                            latest.bmi?.let { BodyMetricLabel(R.string.bmi, it, "") }
                            latest.bodyFatPercent?.let { BodyMetricLabel(R.string.body_fat, it, "%") }
                            latest.muscleMassKg?.let { BodyMetricLabel(R.string.muscle_mass, it, stringResource(R.string.kg)) }
                        }
                    }
                }
            }
        }
        if (state.bodyMeasurements.size >= 2) {
            item { WeightTrendCard(state.bodyMeasurements, locale) }
        }
        item {
            AdaptiveGoalReviewCard(
                review = adaptiveReview,
                profile = profile,
                latestMeasurement = latest,
                locale = locale,
                onApply = { proposedCalories ->
                    val macros = EnergyEstimator.redistributeMacros(
                        calorieTarget = proposedCalories,
                        proteinGrams = profile.proteinGoalGrams,
                        carbsGrams = profile.carbsGoalGrams,
                        fatGrams = profile.fatGoalGrams,
                    )
                    onSaveProfile(
                        profile.copy(
                            weightKg = latest?.weightKg ?: profile.weightKg,
                            calorieGoal = proposedCalories,
                            proteinGoalGrams = macros?.proteinGrams ?: profile.proteinGoalGrams,
                            carbsGoalGrams = macros?.carbsGrams ?: profile.carbsGoalGrams,
                            fatGoalGrams = macros?.fatGrams ?: profile.fatGoalGrams,
                            targetMode = TargetMode.CUSTOM,
                            lastGoalReviewAtEpochMillis = System.currentTimeMillis(),
                        ),
                    )
                },
            )
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = {
                        val bmi = (
                            profile.weightKg / ((profile.heightCm / 100.0) * (profile.heightCm / 100.0)) * 100
                            ).roundToInt() / 100.0
                        editorWarnings = emptySet()
                        editorInitial = BodyMeasurement(
                            measuredAtEpochMillis = System.currentTimeMillis(),
                            weightKg = profile.weightKg,
                            bmi = bmi,
                        )
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.Add, null)
                    Text(stringResource(R.string.add_check_in), Modifier.padding(start = 6.dp))
                }
                OutlinedButton(
                    enabled = scanState.status != BodyScaleScanStatus.PROCESSING,
                    onClick = {
                        onClearScan()
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    if (scanState.status == BodyScaleScanStatus.PROCESSING) {
                        CircularProgressIndicator(Modifier.height(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.DocumentScanner, null)
                    }
                    Text(stringResource(R.string.import_scale_report), Modifier.padding(start = 6.dp))
                }
            }
        }
        if (scanState.status == BodyScaleScanStatus.ERROR ||
            scanState.status == BodyScaleScanStatus.SUCCESS && scanState.prefill?.measurement == null
        ) {
            item {
                Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.scale_scan_error), color = MaterialTheme.colorScheme.error)
                    if (scanState.stagedImagePath != null) {
                        OutlinedButton(onClick = onFlagFailedScan) {
                            Text(stringResource(R.string.flag_failed_scan))
                        }
                    } else if (scanState.flaggedForSupport) {
                        Text(stringResource(R.string.scan_saved_for_support), color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        item { SectionTitle(R.string.check_in_history) }
        if (state.bodyMeasurements.isEmpty()) {
            item { EmptyText(R.string.no_body_check_ins) }
        } else {
            items(state.bodyMeasurements, key = { requireNotNull(it.id) }) { measurement ->
                BodyMeasurementRow(
                    measurement = measurement,
                    locale = locale,
                    bodyLengthUnit = profile.bodyLengthUnit,
                    onEdit = {
                        editorWarnings = emptySet()
                        editorInitial = measurement
                    },
                    onDelete = { onDelete(measurement) },
                )
            }
        }
        item {
            Text(
                stringResource(R.string.composition_disclaimer),
                Modifier.padding(horizontal = 20.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    editorInitial?.let { initial ->
        BodyMeasurementEditorSheet(
            initial = initial,
            warnings = editorWarnings,
            locale = locale,
            bodyLengthUnit = profile.bodyLengthUnit,
            onDismiss = { editorInitial = null },
            onSave = {
                onSave(it)
                editorInitial = null
            },
        )
    }
}

@Composable
private fun BodyMetricLabel(label: Int, value: Double, suffix: String) {
    Column {
        Text(stringResource(label), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("${formatAmount(value, LocalLocale.current.platformLocale)}$suffix", fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AdaptiveGoalReviewCard(
    review: AdaptiveGoalReview,
    profile: UserProfile,
    latestMeasurement: BodyMeasurement?,
    locale: Locale,
    onApply: (Int) -> Unit,
) {
    val lastApplied = profile.lastGoalReviewAtEpochMillis
    val waitingForNewEvidence = lastApplied != null &&
        (latestMeasurement == null || latestMeasurement.measuredAtEpochMillis < lastApplied + 7L * 86_400_000L)
    Card(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.adaptive_goal_review), fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.adaptive_goal_review_explanation),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
            )
            when {
                waitingForNewEvidence -> Text(stringResource(R.string.adaptive_review_waiting))
                review is AdaptiveGoalReview.NeedMoreCheckIns -> Text(
                    stringResource(R.string.adaptive_need_check_ins, review.current, review.required),
                )
                review is AdaptiveGoalReview.NeedMoreTime -> Text(
                    stringResource(R.string.adaptive_need_time, review.spanDays, review.requiredDays),
                )
                review is AdaptiveGoalReview.NeedMoreDiaryDays -> Text(
                    stringResource(R.string.adaptive_need_diary, review.current, review.required),
                )
                review is AdaptiveGoalReview.OnTrack -> Text(
                    stringResource(
                        R.string.adaptive_on_track,
                        signedAmount(review.weeklyChangeKg, locale),
                    ),
                    color = MaterialTheme.colorScheme.primary,
                )
                review is AdaptiveGoalReview.Suggestion -> {
                    Text(
                        stringResource(
                            R.string.adaptive_trend_evidence,
                            signedAmount(review.weeklyChangeKg, locale),
                            review.checkInCount,
                            review.spanDays,
                        ),
                    )
                    Text(
                        stringResource(
                            R.string.adaptive_calorie_proposal,
                            review.proposedCalories,
                            if (review.calorieAdjustment > 0) "+${review.calorieAdjustment}" else review.calorieAdjustment,
                        ),
                        fontWeight = FontWeight.SemiBold,
                    )
                    Button(onClick = { onApply(review.proposedCalories) }, Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.apply_adaptive_targets))
                    }
                }
            }
            Text(
                stringResource(R.string.adaptive_review_safety),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
        }
    }
}

private fun signedAmount(value: Double, locale: Locale): String =
    (if (value > 0) "+" else "") + formatAmount(value, locale)

@Composable
private fun WeightTrendCard(measurements: List<BodyMeasurement>, locale: Locale) {
    val points = measurements.take(30).reversed()
    val minimum = points.minOf(BodyMeasurement::weightKg)
    val maximum = points.maxOf(BodyMeasurement::weightKg)
    val range = (maximum - minimum).coerceAtLeast(0.5)
    val lineColor = MaterialTheme.colorScheme.primary
    val guideColor = MaterialTheme.colorScheme.outlineVariant
    Card(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.weight_trend), fontWeight = FontWeight.Bold)
            Canvas(Modifier.fillMaxWidth().height(130.dp)) {
                drawLine(guideColor, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 2f)
                val coordinates = points.mapIndexed { index, measurement ->
                    val x = if (points.size == 1) size.width / 2 else size.width * index / (points.size - 1f)
                    val y = size.height - ((measurement.weightKg - minimum) / range * size.height).toFloat()
                    Offset(x, y)
                }
                coordinates.zipWithNext().forEach { (first, second) ->
                    drawLine(lineColor, first, second, strokeWidth = 7f, cap = StrokeCap.Round)
                }
                coordinates.forEach { drawCircle(lineColor, radius = 7f, center = it) }
            }
            Row(Modifier.fillMaxWidth()) {
                Text("${formatAmount(minimum, locale)} ${stringResource(R.string.kg)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                Text("${formatAmount(maximum, locale)} ${stringResource(R.string.kg)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun BodyMeasurementRow(
    measurement: BodyMeasurement,
    locale: Locale,
    bodyLengthUnit: BodyLengthUnit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("${formatAmount(measurement.weightKg, locale)} ${stringResource(R.string.kg)}", fontWeight = FontWeight.Bold)
            Text(
                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale)
                    .format(Date(measurement.measuredAtEpochMillis)),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
            )
            val details = buildList {
                measurement.bodyFatPercent?.let { add("${stringResource(R.string.body_fat)} ${formatAmount(it, locale)}%") }
                measurement.muscleMassKg?.let { add("${stringResource(R.string.muscle_mass)} ${formatAmount(it, locale)} kg") }
                measurement.waistCm?.let {
                    add(
                        "${stringResource(R.string.waist)} " +
                            "${formatAmount(bodyLengthUnit.fromCentimeters(it), locale)} ${bodyLengthUnit.symbol}",
                    )
                }
            }
            if (details.isNotEmpty()) Text(details.joinToString(" · "), fontSize = 12.sp)
        }
        if (measurement.source == BodyMeasurementSource.OCR) {
            Text(stringResource(R.string.scanned), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
        }
        IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, stringResource(R.string.edit_check_in)) }
        IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, stringResource(R.string.delete_check_in)) }
    }
    HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outlineVariant)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BodyMeasurementEditorSheet(
    initial: BodyMeasurement,
    warnings: Set<BodyScaleWarning>,
    locale: Locale,
    bodyLengthUnit: BodyLengthUnit,
    onDismiss: () -> Unit,
    onSave: (BodyMeasurement) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val zoneId = ZoneId.systemDefault()
    val initialDateTime = Instant.ofEpochMilli(initial.measuredAtEpochMillis).atZone(zoneId)
    var dateEpochDay by remember(initial.id, initial.measuredAtEpochMillis) {
        mutableStateOf(initialDateTime.toLocalDate().toEpochDay())
    }
    var hour by remember(initial.id, initial.measuredAtEpochMillis) { mutableStateOf(initialDateTime.hour) }
    var minute by remember(initial.id, initial.measuredAtEpochMillis) { mutableStateOf(initialDateTime.minute) }
    var weight by remember(initial.id, initial.measuredAtEpochMillis) { mutableStateOf(initial.weightKg.editableValue()) }
    var bmi by remember(initial.id, initial.measuredAtEpochMillis) { mutableStateOf(initial.bmi.editableValue()) }
    var bodyFat by remember(initial.id, initial.measuredAtEpochMillis) { mutableStateOf(initial.bodyFatPercent.editableValue()) }
    var fatMass by remember(initial.id, initial.measuredAtEpochMillis) { mutableStateOf(initial.fatMassKg.editableValue()) }
    var fatFreeMass by remember(initial.id, initial.measuredAtEpochMillis) { mutableStateOf(initial.fatFreeMassKg.editableValue()) }
    var muscleMass by remember(initial.id, initial.measuredAtEpochMillis) { mutableStateOf(initial.muscleMassKg.editableValue()) }
    var musclePercent by remember(initial.id, initial.measuredAtEpochMillis) { mutableStateOf(initial.musclePercent.editableValue()) }
    var skeletalMuscle by remember(initial.id, initial.measuredAtEpochMillis) {
        mutableStateOf(initial.skeletalMusclePercent.editableValue())
    }
    var boneMass by remember(initial.id, initial.measuredAtEpochMillis) { mutableStateOf(initial.boneMassKg.editableValue()) }
    var proteinMass by remember(initial.id, initial.measuredAtEpochMillis) { mutableStateOf(initial.proteinMassKg.editableValue()) }
    var proteinPercent by remember(initial.id, initial.measuredAtEpochMillis) { mutableStateOf(initial.proteinPercent.editableValue()) }
    var waterMass by remember(initial.id, initial.measuredAtEpochMillis) { mutableStateOf(initial.waterMassKg.editableValue()) }
    var bodyWater by remember(initial.id, initial.measuredAtEpochMillis) { mutableStateOf(initial.bodyWaterPercent.editableValue()) }
    var subcutaneousFat by remember(initial.id, initial.measuredAtEpochMillis) {
        mutableStateOf(initial.subcutaneousFatPercent.editableValue())
    }
    var visceralFat by remember(initial.id, initial.measuredAtEpochMillis) { mutableStateOf(initial.visceralFat.editableValue()) }
    var bmr by remember(initial.id, initial.measuredAtEpochMillis) { mutableStateOf(initial.bmrCalories?.toString().orEmpty()) }
    var bodyAge by remember(initial.id, initial.measuredAtEpochMillis) { mutableStateOf(initial.bodyAge?.toString().orEmpty()) }
    var neck by remember(initial.id, initial.measuredAtEpochMillis, bodyLengthUnit) {
        mutableStateOf(initial.neckCm?.let(bodyLengthUnit::fromCentimeters).editableValue())
    }
    var chest by remember(initial.id, initial.measuredAtEpochMillis, bodyLengthUnit) {
        mutableStateOf(initial.chestCm?.let(bodyLengthUnit::fromCentimeters).editableValue())
    }
    var waist by remember(initial.id, initial.measuredAtEpochMillis, bodyLengthUnit) {
        mutableStateOf(initial.waistCm?.let(bodyLengthUnit::fromCentimeters).editableValue())
    }
    var hips by remember(initial.id, initial.measuredAtEpochMillis, bodyLengthUnit) {
        mutableStateOf(initial.hipsCm?.let(bodyLengthUnit::fromCentimeters).editableValue())
    }
    var upperArm by remember(initial.id, initial.measuredAtEpochMillis, bodyLengthUnit) {
        mutableStateOf(initial.upperArmCm?.let(bodyLengthUnit::fromCentimeters).editableValue())
    }
    var thigh by remember(initial.id, initial.measuredAtEpochMillis, bodyLengthUnit) {
        mutableStateOf(initial.thighCm?.let(bodyLengthUnit::fromCentimeters).editableValue())
    }
    var calf by remember(initial.id, initial.measuredAtEpochMillis, bodyLengthUnit) {
        mutableStateOf(initial.calfCm?.let(bodyLengthUnit::fromCentimeters).editableValue())
    }
    var showTape by rememberSaveable(initial.id) {
        mutableStateOf(listOf(initial.neckCm, initial.chestCm, initial.waistCm, initial.hipsCm, initial.upperArmCm, initial.thighCm, initial.calfCm).any { it != null })
    }
    var showAdvanced by rememberSaveable(initial.id) {
        mutableStateOf(
            initial.source == BodyMeasurementSource.OCR || initial.id != null &&
                (listOf(initial.bodyFatPercent, initial.fatMassKg, initial.fatFreeMassKg, initial.muscleMassKg,
                    initial.musclePercent, initial.skeletalMusclePercent, initial.boneMassKg, initial.proteinMassKg,
                    initial.proteinPercent, initial.waterMassKg, initial.bodyWaterPercent, initial.subcutaneousFatPercent,
                    initial.visceralFat, initial.bmi).any { it != null } || initial.bmrCalories != null || initial.bodyAge != null),
        )
    }
    val date = LocalDate.ofEpochDay(dateEpochDay)
    val timestamp = date.atTime(hour, minute).atZone(zoneId).toInstant().toEpochMilli()
    val weightValue = weight.localizedDoubleOrNull()
    fun optionalValid(value: String, range: ClosedFloatingPointRange<Double>) =
        value.isBlank() || value.localizedDoubleOrNull()?.let { it in range } == true
    val valid = weightValue != null && weightValue in 20.0..400.0 && timestamp <= System.currentTimeMillis() &&
        optionalValid(bmi, 5.0..100.0) && optionalValid(bodyFat, 0.0..100.0) &&
        listOf(fatMass, fatFreeMass, muscleMass, boneMass, proteinMass, waterMass).all { optionalValid(it, 0.0..400.0) } &&
        listOf(musclePercent, skeletalMuscle, proteinPercent, bodyWater, subcutaneousFat).all {
            optionalValid(it, 0.0..100.0)
        } && optionalValid(visceralFat, 0.0..100.0) &&
        listOf(neck, chest, waist, hips, upperArm, thigh, calf).all { value ->
            value.isBlank() || value.localizedDoubleOrNull()?.let(bodyLengthUnit::toCentimeters)?.let { it in 5.0..300.0 } == true
        } &&
        (bmr.isBlank() || bmr.toIntOrNull() in 500..5_000) && (bodyAge.isBlank() || bodyAge.toIntOrNull() in 1..120)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                stringResource(if (initial.id == null) R.string.review_check_in else R.string.edit_check_in),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            if (initial.source == BodyMeasurementSource.OCR) {
                Text(stringResource(R.string.review_scale_values), color = MaterialTheme.colorScheme.primary)
            }
            warnings.forEach { warning ->
                Text(
                    stringResource(
                        when (warning) {
                            BodyScaleWarning.WEIGHT_MISSING -> R.string.scale_weight_missing
                            BodyScaleWarning.DATE_MISSING -> R.string.scale_date_missing
                            BodyScaleWarning.LIMITED_METRICS -> R.string.scale_limited_metrics
                        },
                    ),
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        android.app.DatePickerDialog(
                            context,
                            { _, year, month, day -> dateEpochDay = LocalDate.of(year, month + 1, day).toEpochDay() },
                            date.year,
                            date.monthValue - 1,
                            date.dayOfMonth,
                        ).apply { datePicker.maxDate = System.currentTimeMillis() }.show()
                    },
                    modifier = Modifier.weight(1f),
                ) { Text(date.format(DateTimeFormatter.ofPattern("d MMM yyyy", locale))) }
                OutlinedButton(
                    onClick = {
                        android.app.TimePickerDialog(context, { _, h, m -> hour = h; minute = m }, hour, minute, true).show()
                    },
                    modifier = Modifier.weight(1f),
                ) { Text(String.format(locale, "%02d:%02d", hour, minute)) }
            }
            NumericField(weight, { weight = it }, R.string.weight_kg, Modifier.fillMaxWidth(), decimal = true)
            OptionalMeasurementSection(
                title = stringResource(R.string.tape_measurements),
                subtitle = stringResource(R.string.tape_measurements_subtitle),
                expanded = showTape,
                onToggle = { showTape = !showTape },
            ) {
                Text(
                    stringResource(R.string.tape_measurement_general_instruction),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                )
                val unitLabel = bodyLengthUnit.symbol
                ExplainedNumericField(neck, { neck = it }, stringResource(R.string.body_measurement_label, stringResource(R.string.neck), unitLabel), R.string.neck_measure_instruction)
                ExplainedNumericField(chest, { chest = it }, stringResource(R.string.body_measurement_label, stringResource(R.string.chest), unitLabel), R.string.chest_measure_instruction)
                ExplainedNumericField(waist, { waist = it }, stringResource(R.string.body_measurement_label, stringResource(R.string.waist), unitLabel), R.string.waist_measure_instruction)
                ExplainedNumericField(hips, { hips = it }, stringResource(R.string.body_measurement_label, stringResource(R.string.hips), unitLabel), R.string.hips_measure_instruction)
                ExplainedNumericField(upperArm, { upperArm = it }, stringResource(R.string.body_measurement_label, stringResource(R.string.upper_arm), unitLabel), R.string.upper_arm_measure_instruction)
                ExplainedNumericField(thigh, { thigh = it }, stringResource(R.string.body_measurement_label, stringResource(R.string.thigh), unitLabel), R.string.thigh_measure_instruction)
                ExplainedNumericField(calf, { calf = it }, stringResource(R.string.body_measurement_label, stringResource(R.string.calf), unitLabel), R.string.calf_measure_instruction)
            }
            OptionalMeasurementSection(
                title = stringResource(R.string.advanced_composition),
                subtitle = stringResource(R.string.composition_optional_hint),
                expanded = showAdvanced,
                onToggle = { showAdvanced = !showAdvanced },
            ) {
                ExplainedNumericField(bmi, { bmi = it }, R.string.bmi, R.string.bmi_explanation)
                ExplainedNumericField(bodyFat, { bodyFat = it }, R.string.body_fat_percent, R.string.body_fat_explanation)
                ExplainedNumericField(fatMass, { fatMass = it }, R.string.fat_mass_kg, R.string.fat_mass_explanation)
                ExplainedNumericField(fatFreeMass, { fatFreeMass = it }, R.string.fat_free_mass_kg, R.string.fat_free_mass_explanation)
                ExplainedNumericField(muscleMass, { muscleMass = it }, R.string.muscle_mass_kg, R.string.muscle_mass_explanation)
                ExplainedNumericField(musclePercent, { musclePercent = it }, R.string.muscle_percent, R.string.muscle_percent_explanation)
                ExplainedNumericField(skeletalMuscle, { skeletalMuscle = it }, R.string.skeletal_muscle_percent, R.string.skeletal_muscle_explanation)
                ExplainedNumericField(boneMass, { boneMass = it }, R.string.bone_mass_kg, R.string.bone_mass_explanation)
                ExplainedNumericField(proteinMass, { proteinMass = it }, R.string.protein_mass_kg, R.string.protein_mass_explanation)
                ExplainedNumericField(proteinPercent, { proteinPercent = it }, R.string.protein_percent, R.string.protein_percent_explanation)
                ExplainedNumericField(waterMass, { waterMass = it }, R.string.water_mass_kg, R.string.water_mass_explanation)
                ExplainedNumericField(bodyWater, { bodyWater = it }, R.string.body_water_percent, R.string.body_water_explanation)
                ExplainedNumericField(subcutaneousFat, { subcutaneousFat = it }, R.string.subcutaneous_fat_percent, R.string.subcutaneous_fat_explanation)
                ExplainedNumericField(visceralFat, { visceralFat = it }, R.string.visceral_fat, R.string.visceral_fat_explanation)
                ExplainedNumericField(bmr, { bmr = it }, R.string.bmr_kcal, R.string.bmr_explanation, decimal = false)
                ExplainedNumericField(bodyAge, { bodyAge = it }, R.string.body_age, R.string.body_age_explanation, decimal = false)
            }
            if (!valid) Text(stringResource(R.string.invalid_body_measurement), color = MaterialTheme.colorScheme.error)
            Button(
                enabled = valid,
                onClick = {
                    onSave(
                        BodyMeasurement(
                            id = initial.id,
                            measuredAtEpochMillis = timestamp,
                            weightKg = requireNotNull(weightValue),
                            bmi = bmi.localizedDoubleOrNull(),
                            bodyFatPercent = bodyFat.localizedDoubleOrNull(),
                            fatMassKg = fatMass.localizedDoubleOrNull(),
                            fatFreeMassKg = fatFreeMass.localizedDoubleOrNull(),
                            muscleMassKg = muscleMass.localizedDoubleOrNull(),
                            musclePercent = musclePercent.localizedDoubleOrNull(),
                            skeletalMusclePercent = skeletalMuscle.localizedDoubleOrNull(),
                            boneMassKg = boneMass.localizedDoubleOrNull(),
                            proteinMassKg = proteinMass.localizedDoubleOrNull(),
                            proteinPercent = proteinPercent.localizedDoubleOrNull(),
                            waterMassKg = waterMass.localizedDoubleOrNull(),
                            bodyWaterPercent = bodyWater.localizedDoubleOrNull(),
                            subcutaneousFatPercent = subcutaneousFat.localizedDoubleOrNull(),
                            visceralFat = visceralFat.localizedDoubleOrNull(),
                            bmrCalories = bmr.toIntOrNull(),
                            bodyAge = bodyAge.toIntOrNull(),
                            neckCm = neck.localizedDoubleOrNull()?.let(bodyLengthUnit::toCentimeters),
                            chestCm = chest.localizedDoubleOrNull()?.let(bodyLengthUnit::toCentimeters),
                            waistCm = waist.localizedDoubleOrNull()?.let(bodyLengthUnit::toCentimeters),
                            hipsCm = hips.localizedDoubleOrNull()?.let(bodyLengthUnit::toCentimeters),
                            upperArmCm = upperArm.localizedDoubleOrNull()?.let(bodyLengthUnit::toCentimeters),
                            thighCm = thigh.localizedDoubleOrNull()?.let(bodyLengthUnit::toCentimeters),
                            calfCm = calf.localizedDoubleOrNull()?.let(bodyLengthUnit::toCentimeters),
                            source = initial.source,
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.save_check_in)) }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun OptionalMeasurementSection(
    title: String,
    subtitle: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            TextButton(onClick = onToggle, Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                    Text(title, fontWeight = FontWeight.SemiBold)
                    Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    stringResource(if (expanded) R.string.collapse else R.string.expand),
                )
            }
            if (expanded) {
                Column(
                    Modifier.padding(start = 4.dp, end = 4.dp, bottom = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) { content() }
            }
        }
    }
}

@Composable
private fun ExplainedNumericField(
    value: String,
    onValueChange: (String) -> Unit,
    label: Int,
    explanation: Int,
    decimal: Boolean = true,
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        NumericField(value, onValueChange, label, Modifier.fillMaxWidth(), decimal = decimal)
        Text(
            stringResource(explanation),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun ExplainedNumericField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    explanation: Int,
    decimal: Boolean = true,
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        NumericField(value, onValueChange, label, Modifier.fillMaxWidth(), decimal = decimal)
        Text(
            stringResource(explanation),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
        )
    }
}

private fun Double?.editableValue(): String = this?.editableValue().orEmpty()

private enum class HistoryRange(val days: Long) { DAYS(7), WEEKS(28), MONTHS(183) }

private data class ReportBucket(val label: String, val calories: Int, val loggedDays: Int)

@Composable
private fun HistoryScreen(
    state: AppUiState,
    onBack: () -> Unit,
    onDelete: (FoodEntry) -> Unit,
    onAdd: (Food, Double, Serving?, String?, Long) -> Unit,
    onUpdate: (FoodEntry, Double, Long) -> Unit,
) {
    val context = LocalContext.current
    val locale = LocalLocale.current.platformLocale
    val zoneId = ZoneId.systemDefault()
    val today = LocalDate.now(zoneId)
    var selectedEpochDay by rememberSaveable { mutableStateOf(today.toEpochDay()) }
    var rangeName by rememberSaveable { mutableStateOf(HistoryRange.DAYS.name) }
    var showFoodPicker by rememberSaveable { mutableStateOf(false) }
    var foodPickerQuery by rememberSaveable { mutableStateOf("") }
    var selectedAddFood by remember { mutableStateOf<Food?>(null) }
    var editingEntry by remember { mutableStateOf<FoodEntry?>(null) }
    val selectedDate = LocalDate.ofEpochDay(selectedEpochDay.coerceAtMost(today.toEpochDay()))
    val range = HistoryRange.valueOf(rangeName)
    val profile = requireNotNull(state.profile)
    val selectedEntries = state.allEntries.filter {
        Instant.ofEpochMilli(it.consumedAtEpochMillis).atZone(zoneId).toLocalDate() == selectedDate
    }
    val selectedDay = state.historyDays.firstOrNull { it.date == selectedDate }
    val startDate = today.minusDays(range.days - 1)
    val report = HistoryReportCalculator.period(state.historyDays, startDate, today, profile.calorieGoal)
    val buckets = when (range) {
        HistoryRange.DAYS -> (0L until 7L).map { offset ->
            val date = startDate.plusDays(offset)
            val day = state.historyDays.firstOrNull { it.date == date }
            ReportBucket(date.format(DateTimeFormatter.ofPattern("EEE", locale)), day?.nutrition?.calories ?: 0, if (day == null) 0 else 1)
        }
        HistoryRange.WEEKS -> (0L until 4L).map { index ->
            val bucketStart = startDate.plusDays(index * 7)
            val bucketEnd = bucketStart.plusDays(6).coerceAtMost(today)
            val summary = HistoryReportCalculator.period(state.historyDays, bucketStart, bucketEnd, profile.calorieGoal)
            ReportBucket(
                "${bucketStart.format(DateTimeFormatter.ofPattern("d MMM", locale))}–${bucketEnd.format(DateTimeFormatter.ofPattern("d MMM", locale))}",
                summary.averageNutrition.calories,
                summary.loggedDays,
            )
        }
        HistoryRange.MONTHS -> {
            val currentMonth = YearMonth.from(today)
            (5 downTo 0).map { monthsAgo ->
                val month = currentMonth.minusMonths(monthsAgo.toLong())
                val monthStart = month.atDay(1)
                val monthEnd = month.atEndOfMonth().coerceAtMost(today)
                val summary = HistoryReportCalculator.period(state.historyDays, monthStart, monthEnd, profile.calorieGoal)
                ReportBucket(month.format(DateTimeFormatter.ofPattern("MMM", locale)), summary.averageNutrition.calories, summary.loggedDays)
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(start = 8.dp, end = 12.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                }
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.history), fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.history_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(HistoryRange.entries) { option ->
                    FilterChip(
                        selected = option == range,
                        onClick = { rangeName = option.name },
                        label = {
                            Text(
                                stringResource(
                                    when (option) {
                                        HistoryRange.DAYS -> R.string.last_7_days
                                        HistoryRange.WEEKS -> R.string.last_4_weeks
                                        HistoryRange.MONTHS -> R.string.last_6_months
                                    },
                                ),
                            )
                        },
                    )
                }
            }
        }
        item {
            Card(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.report_overview), fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(R.string.average_calories, report.averageNutrition.calories),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(R.string.logged_days, report.loggedDays, report.totalDays),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        stringResource(
                            R.string.average_macros,
                            report.averageNutrition.proteinGrams,
                            report.averageNutrition.carbsGrams,
                            report.averageNutrition.fatGrams,
                        ),
                        fontSize = 13.sp,
                    )
                    report.averageNutrition.fiberGrams?.let {
                        Text(stringResource(R.string.average_fiber, it), fontSize = 13.sp)
                    }
                }
            }
        }
        items(buckets, key = { it.label }) { bucket ->
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    Text(bucket.label, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Text(
                        if (bucket.loggedDays == 0) stringResource(R.string.no_data) else "${bucket.calories} ${stringResource(R.string.kcal)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                LinearProgressIndicator(
                    progress = { (bucket.calories.toFloat() / profile.calorieGoal).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(7.dp),
                )
            }
        }
        item { SectionTitle(R.string.daily_history) }
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { selectedEpochDay-- }) {
                    Icon(Icons.Default.ChevronLeft, stringResource(R.string.previous_day))
                }
                OutlinedButton(
                    onClick = {
                        android.app.DatePickerDialog(
                            context,
                            { _, year, month, day -> selectedEpochDay = LocalDate.of(year, month + 1, day).toEpochDay() },
                            selectedDate.year,
                            selectedDate.monthValue - 1,
                            selectedDate.dayOfMonth,
                        ).apply { datePicker.maxDate = System.currentTimeMillis() }.show()
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.CalendarMonth, null)
                    Text(
                        selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", locale)),
                        Modifier.padding(start = 8.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(enabled = selectedDate < today, onClick = { selectedEpochDay++ }) {
                    Icon(Icons.Default.ChevronRight, stringResource(R.string.next_day))
                }
            }
        }
        item {
            OutlinedButton(
                onClick = {
                    foodPickerQuery = ""
                    showFoodPicker = true
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            ) {
                Icon(Icons.Default.Add, null)
                Text(stringResource(R.string.add_food_to_day), Modifier.padding(start = 8.dp))
            }
        }
        item {
            Card(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "${selectedDay?.nutrition?.calories ?: 0} / ${profile.calorieGoal} ${stringResource(R.string.kcal)}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    selectedDay?.let { day ->
                        Text(
                            stringResource(
                                R.string.day_macro_summary,
                                day.nutrition.proteinGrams,
                                day.nutrition.carbsGrams,
                                day.nutrition.fatGrams,
                                day.nutrition.fiberGrams ?: 0.0,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        if (selectedEntries.isEmpty()) {
            item { EmptyText(R.string.no_entries_for_day) }
        } else {
            items(selectedEntries, key = { it.id }) { entry ->
                EntryRow(entry, locale, onRemove = { onDelete(entry) }, onEdit = { editingEntry = entry })
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    if (showFoodPicker) {
        HistoryFoodPickerSheet(
            foods = state.foods,
            usage = state.unitUsage,
            query = foodPickerQuery,
            locale = locale,
            onQueryChange = { foodPickerQuery = it },
            onDismiss = { showFoodPicker = false },
            onSelect = {
                selectedAddFood = it
                showFoodPicker = false
            },
        )
    }
    selectedAddFood?.let { food ->
        QuickAddSheet(
            food = food,
            recipe = state.recipes[food.id],
            usage = state.unitUsage[food.id].orEmpty(),
            quantityUsage = state.quantityUsage[food.id].orEmpty(),
            preparationUsage = state.preparationUsage[food.id].orEmpty(),
            locale = locale,
            onDismiss = { selectedAddFood = null },
        ) { loggedFood, amount, serving, batchId ->
            val time = if (selectedDate == today) LocalTime.now() else LocalTime.NOON
            val consumedAt = selectedDate.atTime(time).atZone(zoneId).toInstant().toEpochMilli()
            onAdd(loggedFood, amount, serving, batchId, consumedAt)
            selectedAddFood = null
        }
    }
    editingEntry?.let { entry ->
        val batchRemaining = entry.recipeBatchId?.let { batchId ->
            state.recipes.values.asSequence().flatMap { it.batches.asSequence() }
                .firstOrNull { it.id == batchId }?.remainingGrams
        }
        EntryEditorSheet(
            entry = entry,
            locale = locale,
            zoneId = zoneId,
            maximumRecipeGrams = batchRemaining?.plus(entry.recipeBatchGrams),
            onDismiss = { editingEntry = null },
            onSave = { amount, consumedAt ->
                onUpdate(entry, amount, consumedAt)
                editingEntry = null
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedbackScreen(
    messages: List<FeedbackMessage>,
    onBack: () -> Unit,
    onSend: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    var draft by rememberSaveable { mutableStateOf("") }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { if (it.length <= 2_000) draft = it },
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.feedback_message)) },
                    minLines = 1,
                    maxLines = 5,
                )
                IconButton(
                    enabled = draft.isNotBlank(),
                    onClick = {
                        onSend(draft)
                        draft = ""
                    },
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, stringResource(R.string.send_feedback))
                }
            }
        },
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                    Column {
                        Text(stringResource(R.string.feedback), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(R.string.feedback_private_explanation),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (messages.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.no_feedback_messages),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(messages, key = FeedbackMessage::id) { message ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(start = 16.dp, top = 12.dp, end = 6.dp, bottom = 6.dp)) {
                        Text(message.text, modifier = Modifier.padding(end = 10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                                    .format(Date(message.createdAtEpochMillis)),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                            )
                            IconButton(onClick = { onDelete(message.id) }) {
                                Icon(Icons.Default.Delete, stringResource(R.string.delete_feedback_message))
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodayScreen(
    state: AppUiState,
    lookupState: FoodLookupState,
    nutritionLabelScanState: NutritionLabelScanState,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenProgress: () -> Unit,
    onOpenFeedback: () -> Unit,
    onAdd: (Food, Double, Serving?, String?, Long) -> Unit,
    onDelete: (FoodEntry) -> Unit,
    onUpdate: (FoodEntry, Double, Long) -> Unit,
    onCreateFood: (Pair<String, String?>) -> Unit,
    onCreateRecipe: () -> Unit,
    onEditFood: (Food) -> Unit,
    onCreateVariant: (Food) -> Unit,
    onCreateRecipeFromFood: (Food) -> Unit,
    onSearchGlobal: (String, String) -> Unit,
    onLookupBarcode: (String, String) -> Unit,
    onClearLookup: () -> Unit,
    onScanNutritionLabel: (Uri, String) -> Unit,
    onClearNutritionLabelScan: () -> Unit,
    onFlagFailedNutritionScan: () -> Unit,
    onImportFood: (Food) -> Unit,
) {
    val context = LocalContext.current
    val locale = LocalLocale.current.platformLocale
    var pendingCameraUri by rememberSaveable { mutableStateOf<String?>(null) }
    var showLabelSourceDialog by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedCatalogueCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var showFoodFilters by rememberSaveable { mutableStateOf(false) }
    val zoneId = ZoneId.systemDefault()
    val today = LocalDate.now(zoneId)
    var selectedEpochDay by rememberSaveable { mutableStateOf(today.toEpochDay()) }
    val selectedDate = LocalDate.ofEpochDay(selectedEpochDay.coerceAtMost(today.toEpochDay()))
    val selectedEntries = state.allEntries.filter { entry ->
        Instant.ofEpochMilli(entry.consumedAtEpochMillis).atZone(zoneId).toLocalDate() == selectedDate
    }
    val selectedTotals = selectedEntries.fold(Nutrition.Zero) { total, entry -> total + entry.nutrition }
    val selectedFiberIncomplete = selectedEntries.any { it.nutrition.fiberGrams == null }
    val labelPhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { onScanNutritionLabel(it, query.trim()) }
    }
    val labelCamera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { captured ->
        pendingCameraUri?.let(Uri::parse)?.let { uri ->
            if (captured) onScanNutritionLabel(uri, query.trim())
            else runCatching { context.contentResolver.delete(uri, null, null) }
        }
        pendingCameraUri = null
    }
    val barcodeScanner = remember {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
            )
            .enableAutoZoom()
            .build()
        GmsBarcodeScanning.getClient(context, options)
    }
    var selectedFood by remember { mutableStateOf<Food?>(null) }
    var customizingFood by remember { mutableStateOf<Food?>(null) }
    var editingEntry by remember { mutableStateOf<FoodEntry?>(null) }
    var showMacroDetails by rememberSaveable { mutableStateOf(false) }
    val profile = requireNotNull(state.profile)
    val frequencyByFoodId = state.unitUsage.mapValues { (_, usages) -> usages.sumOf(UnitUsage::useCount) }
    val recipesWithLeftovers = state.recipes.values
        .filter { recipe -> recipe.batches.any { it.remainingGrams > 0 } }
        .sortedBy { it.name }
    val catalogueCategories = state.foods.mapNotNull(Food::categoryKey).distinct().sorted()
    val isBrowsingCatalogue = selectedCatalogueCategory != null
    val allLocalQueryMatches = if (query.isBlank()) emptyList() else state.foods.filter { it.matches(query) }
    val categoryQueryMatches = allLocalQueryMatches.filter { it.matchesCatalogueCategory(selectedCatalogueCategory) }
    val expandedSearchBeyondCategory = query.isNotBlank() && selectedCatalogueCategory != null &&
        categoryQueryMatches.isEmpty() && allLocalQueryMatches.isNotEmpty()
    val displayedFoods = when {
        query.isNotBlank() -> if (expandedSearchBeyondCategory) allLocalQueryMatches else categoryQueryMatches
        isBrowsingCatalogue -> state.foods
            .filter { it.matchesCatalogueCategory(selectedCatalogueCategory) }
            .sortedBy { it.name(locale) }
        else -> FoodRecommender.rankPreviouslyUsed(state.foods, selectedTotals, profile, frequencyByFoodId)
    }
    val exactFoodExists = query.isNotBlank() && state.foods.any { it.hasExactName(query) }
    val currentLookup = lookupState.takeIf { it.query.equals(query.trim(), ignoreCase = true) }
    val remoteResults = currentLookup?.results.orEmpty().filterNot { remote ->
        state.foods.any { local -> local.hasSameCatalogueIdentity(remote) }
    }
    val globalResultAlreadySaved = currentLookup?.results.orEmpty().any { remote ->
        state.foods.any { local -> local.hasSameCatalogueIdentity(remote) }
    }
    val exactRemoteFoodExists = remoteResults.any { it.hasExactName(query) }
    val globalMatchExists = exactRemoteFoodExists || currentLookup?.isBarcodeLookup == true && remoteResults.isNotEmpty()

    fun runGlobalSearch() {
        val normalized = query.trim()
        if (normalized.all(Char::isDigit) && normalized.length in 8..14) {
            onLookupBarcode(normalized, locale.language)
        } else if (normalized.length >= 2) {
            onSearchGlobal(normalized, locale.language)
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .statusBarsPadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.padding(start = 20.dp, end = 10.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                android.app.DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        selectedEpochDay = LocalDate.of(year, month + 1, day).toEpochDay()
                                    },
                                    selectedDate.year,
                                    selectedDate.monthValue - 1,
                                    selectedDate.dayOfMonth,
                                ).apply { datePicker.maxDate = System.currentTimeMillis() }.show()
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (selectedDate == today) {
                                    stringResource(R.string.today)
                                } else {
                                    selectedDate.format(DateTimeFormatter.ofPattern("EEE, d MMM", locale))
                                },
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Icon(
                                Icons.Default.CalendarMonth,
                                stringResource(R.string.open_day_browser),
                                Modifier.padding(start = 8.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Text(
                            if (selectedDate == today) {
                                stringResource(R.string.daily_summary)
                            } else {
                                selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", locale))
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onOpenHistory) {
                        Icon(Icons.Default.History, stringResource(R.string.history))
                    }
                    IconButton(onClick = onOpenProgress) {
                        Icon(Icons.Default.MonitorWeight, stringResource(R.string.body_progress))
                    }
                    IconButton(onClick = onOpenFeedback) {
                        Icon(Icons.Default.ChatBubbleOutline, stringResource(R.string.feedback))
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, stringResource(R.string.settings))
                    }
                }
            }
            item { SummaryCard(selectedTotals, profile, selectedFiberIncomplete) { showMacroDetails = true } }
            if (recipesWithLeftovers.isNotEmpty()) {
                item { SectionTitle(R.string.leftovers) }
                item {
                    LazyRow(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(recipesWithLeftovers, key = { it.foodId }) { recipe ->
                            LeftoverCard(recipe, locale) {
                                state.foods.firstOrNull { it.id == recipe.foodId }?.let { selectedFood = it }
                            }
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        onClearLookup()
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        Row {
                            IconButton(
                                enabled = nutritionLabelScanState.status != NutritionLabelScanStatus.PROCESSING,
                                onClick = { showLabelSourceDialog = true },
                            ) {
                                Icon(Icons.Default.DocumentScanner, stringResource(R.string.scan_nutrition_label))
                            }
                            if (query.isNotEmpty()) {
                                IconButton(onClick = {
                                    query = ""
                                    onClearLookup()
                                }) {
                                    Icon(Icons.Default.Close, stringResource(R.string.close))
                                }
                            } else {
                                IconButton(
                                    onClick = {
                                        barcodeScanner.startScan().addOnSuccessListener { barcode ->
                                            barcode.rawValue?.filter(Char::isDigit)?.takeIf(String::isNotEmpty)?.let { code ->
                                                val localFood = state.foods.firstOrNull { it.barcode == code }
                                                if (localFood != null) {
                                                    selectedFood = localFood
                                                } else {
                                                    query = code
                                                    onLookupBarcode(code, locale.language)
                                                }
                                            }
                                        }
                                    },
                                ) {
                                    Icon(Icons.Default.QrCodeScanner, stringResource(R.string.scan_barcode))
                                }
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { runGlobalSearch() }),
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                )
            }
            item {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val selectedFilterLabel = when (selectedCatalogueCategory) {
                        null -> stringResource(R.string.recommended_foods)
                        CATALOGUE_CATEGORY_ALL -> stringResource(R.string.all_foods)
                        CATALOGUE_CATEGORY_PERSONAL -> stringResource(R.string.personal_foods)
                        CATALOGUE_CATEGORY_RECIPES -> stringResource(R.string.recipes)
                        else -> stringResource(requireNotNull(selectedCatalogueCategory).labelResource())
                    }
                    OutlinedButton(
                        onClick = { showFoodFilters = !showFoodFilters },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        selectedCatalogueCategory?.takeUnless(::isSpecialCatalogueFilter)?.let { category ->
                            CategoryFoodImage(category, Modifier.width(28.dp).height(28.dp).clip(RoundedCornerShape(8.dp)))
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(stringResource(R.string.food_filter_label, selectedFilterLabel), Modifier.weight(1f))
                        Icon(
                            if (showFoodFilters) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            stringResource(R.string.choose_food_filter),
                        )
                    }
                    if (showFoodFilters) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(18.dp),
                        ) {
                            FlowRow(
                                Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                        FilterChip(
                            selected = selectedCatalogueCategory == null,
                            onClick = {
                                selectedCatalogueCategory = null
                                showFoodFilters = false
                            },
                            label = { Text(stringResource(R.string.recommended_foods)) },
                        )
                        FilterChip(
                            selected = selectedCatalogueCategory == CATALOGUE_CATEGORY_ALL,
                            onClick = {
                                selectedCatalogueCategory = CATALOGUE_CATEGORY_ALL
                                showFoodFilters = false
                            },
                            label = { Text(stringResource(R.string.all_foods)) },
                        )
                                if (state.foods.any(Food::isPersonal)) {
                            FilterChip(
                                selected = selectedCatalogueCategory == CATALOGUE_CATEGORY_PERSONAL,
                                        onClick = {
                                            selectedCatalogueCategory = CATALOGUE_CATEGORY_PERSONAL
                                            showFoodFilters = false
                                        },
                                label = { Text(stringResource(R.string.personal_foods)) },
                            )
                        }
                                if (state.recipes.isNotEmpty()) {
                            FilterChip(
                                selected = selectedCatalogueCategory == CATALOGUE_CATEGORY_RECIPES,
                                        onClick = {
                                            selectedCatalogueCategory = CATALOGUE_CATEGORY_RECIPES
                                            showFoodFilters = false
                                        },
                                label = { Text(stringResource(R.string.recipes)) },
                            )
                        }
                                catalogueCategories.forEach { category ->
                        FilterChip(
                            selected = selectedCatalogueCategory == category,
                                        onClick = {
                                            selectedCatalogueCategory = category
                                            showFoodFilters = false
                                        },
                            label = { Text(stringResource(category.labelResource())) },
                                        leadingIcon = {
                                            CategoryFoodImage(
                                                category,
                                                Modifier.width(26.dp).height(26.dp).clip(RoundedCornerShape(8.dp)),
                                            )
                                        },
                        )
                                }
                            }
                        }
                    }
                }
            }
            if (expandedSearchBeyondCategory) {
                item {
                    Text(
                        stringResource(R.string.search_expanded_all_categories),
                        Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                    )
                }
            }
            if (nutritionLabelScanState.status == NutritionLabelScanStatus.PROCESSING) {
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(Modifier.height(20.dp), strokeWidth = 2.dp)
                        Text(stringResource(R.string.reading_nutrition_label))
                    }
                }
            } else if (nutritionLabelScanState.status == NutritionLabelScanStatus.ERROR) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(stringResource(R.string.ocr_scan_error), color = MaterialTheme.colorScheme.error)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (nutritionLabelScanState.stagedImagePath != null) {
                                OutlinedButton(onClick = onFlagFailedNutritionScan) {
                                    Text(stringResource(R.string.flag_failed_scan))
                                }
                            } else if (nutritionLabelScanState.flaggedForSupport) {
                                Text(
                                    stringResource(R.string.scan_saved_for_support),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            TextButton(onClick = {
                                onClearNutritionLabelScan()
                                showLabelSourceDialog = true
                            }) { Text(stringResource(R.string.retry)) }
                        }
                    }
                }
            }
            if (query.isBlank()) {
                item {
                    OutlinedButton(
                        onClick = onCreateRecipe,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    ) {
                        Icon(Icons.Default.RestaurantMenu, null)
                        Text(stringResource(R.string.create_recipe), Modifier.padding(start = 8.dp))
                    }
                }
            }
            if (query.isNotBlank()) {
                item {
                    OutlinedButton(
                        enabled = query.trim().length >= 2 && currentLookup?.status != FoodLookupStatus.SEARCHING,
                        onClick = { runGlobalSearch() },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    ) {
                        if (currentLookup?.status == FoodLookupStatus.SEARCHING) {
                            CircularProgressIndicator(Modifier.height(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Search, null)
                        }
                        Text(
                            stringResource(
                                if (query.trim().all(Char::isDigit) && query.trim().length in 8..14) {
                                    R.string.lookup_barcode
                                } else {
                                    R.string.search_open_food_facts
                                },
                            ),
                            Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
            item {
                SectionTitle(
                    when {
                        query.isNotBlank() -> R.string.quick_add
                        isBrowsingCatalogue -> R.string.food_catalogue
                        else -> R.string.recommended_foods
                    },
                )
            }
            if (displayedFoods.isEmpty()) {
                item {
                    EmptyText(
                        if (query.isBlank() && !isBrowsingCatalogue) R.string.no_recommended_foods else R.string.no_results,
                    )
                }
            }
            else items(displayedFoods, key = { it.id }) {
                FoodRow(
                    food = it,
                    recipe = state.recipes[it.id],
                    locale = locale,
                    currentTotals = selectedTotals,
                    profile = profile,
                    standaloneUses = frequencyByFoodId[it.id] ?: 0,
                    ingredientUses = state.ingredientUsage[it.id] ?: 0,
                    onAdd = { selectedFood = it },
                    onEdit = { onEditFood(it) },
                )
            }
            if (currentLookup?.status == FoodLookupStatus.SUCCESS && remoteResults.isNotEmpty()) {
                item { SectionTitle(R.string.open_food_facts_results) }
                items(remoteResults, key = { it.id }) { food ->
                    OpenFoodFactsRow(food, locale) { onImportFood(food) }
                }
            }
            if (currentLookup?.status == FoodLookupStatus.ERROR || currentLookup?.status == FoodLookupStatus.RATE_LIMITED) {
                item {
                    Text(
                        stringResource(
                            if (currentLookup.status == FoodLookupStatus.RATE_LIMITED) {
                                R.string.open_food_facts_rate_limited
                            } else {
                                R.string.open_food_facts_error
                            },
                        ),
                        Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            if (query.isNotBlank() && !exactFoodExists && !globalMatchExists && !globalResultAlreadySaved &&
                currentLookup?.status == FoodLookupStatus.SUCCESS
            ) {
                item {
                    OutlinedButton(
                        onClick = {
                            if (currentLookup.isBarcodeLookup) {
                                onCreateFood("" to query.trim())
                            } else {
                                onCreateFood(query.trim() to null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    ) {
                        Text(
                            stringResource(
                                if (currentLookup.isBarcodeLookup) {
                                    R.string.add_missing_barcode_food
                                } else {
                                    R.string.add_missing_personal_food
                                },
                                query.trim(),
                            ),
                        )
                    }
                }
            }
            item { SectionTitle(if (selectedDate == today) R.string.today_entries else R.string.selected_day_entries) }
            if (selectedEntries.isEmpty()) item { EmptyText(R.string.no_entries) }
            else items(selectedEntries, key = { it.id }) {
                EntryRow(it, locale, onRemove = { onDelete(it) }, onEdit = { editingEntry = it })
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    selectedFood?.let { food ->
        QuickAddSheet(
            food,
            state.recipes[food.id],
            state.unitUsage[food.id].orEmpty(),
            state.quantityUsage[food.id].orEmpty(),
            state.preparationUsage[food.id].orEmpty(),
            locale,
            { selectedFood = null },
            {
                selectedFood = null
                customizingFood = food
            },
            onPrepareBatch = state.recipes[food.id]?.let {
                {
                    selectedFood = null
                    onEditFood(food)
                }
            },
        ) { loggedFood, amount, serving, batchId ->
            val time = if (selectedDate == today) LocalTime.now() else LocalTime.NOON
            val consumedAt = selectedDate.atTime(time).atZone(zoneId).toInstant().toEpochMilli()
            onAdd(loggedFood, amount, serving, batchId, consumedAt)
            selectedFood = null
            query = ""
        }
    }
    editingEntry?.let { entry ->
        val batchRemaining = entry.recipeBatchId?.let { batchId ->
            state.recipes.values.asSequence().flatMap { it.batches.asSequence() }
                .firstOrNull { it.id == batchId }?.remainingGrams
        }
        EntryEditorSheet(
            entry = entry,
            locale = locale,
            zoneId = zoneId,
            maximumRecipeGrams = batchRemaining?.plus(entry.recipeBatchGrams),
            onDismiss = { editingEntry = null },
            onSave = { amount, consumedAt ->
                onUpdate(entry, amount, consumedAt)
                editingEntry = null
            },
        )
    }
    if (showMacroDetails) {
        MacroDetailsSheet(selectedTotals, profile, selectedFiberIncomplete) { showMacroDetails = false }
    }
    if (showLabelSourceDialog) {
        AlertDialog(
            onDismissRequest = { showLabelSourceDialog = false },
            title = { Text(stringResource(R.string.scan_nutrition_label)) },
            text = { Text(stringResource(R.string.scan_label_explanation)) },
            confirmButton = {
                TextButton(onClick = {
                    showLabelSourceDialog = false
                    val directory = File(context.cacheDir, "label-captures").apply { mkdirs() }
                    directory.listFiles()?.filter { it.name.startsWith("nutrition-label-") }?.forEach(File::delete)
                    val file = File.createTempFile("nutrition-label-", ".jpg", directory)
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    pendingCameraUri = uri.toString()
                    labelCamera.launch(uri)
                }) { Text(stringResource(R.string.take_photo)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showLabelSourceDialog = false
                    labelPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) { Text(stringResource(R.string.choose_photo)) }
            },
        )
    }
    customizingFood?.let { food ->
        AlertDialog(
            onDismissRequest = { customizingFood = null },
            title = { Text(stringResource(R.string.customize_food, food.name(locale))) },
            text = { Text(stringResource(R.string.customize_food_explanation)) },
            confirmButton = {
                TextButton(onClick = {
                    customizingFood = null
                    if (food.isPersonal) onEditFood(food) else onCreateVariant(food)
                }) {
                    Text(
                        stringResource(
                            if (food.isPersonal) R.string.edit_personal_food else R.string.create_personal_variant,
                        ),
                    )
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        customizingFood = null
                        onCreateRecipeFromFood(food)
                    }) { Text(stringResource(R.string.create_recipe_from_food)) }
                    TextButton(onClick = { customizingFood = null }) { Text(stringResource(R.string.cancel)) }
                }
            },
        )
    }
}

@Composable
private fun SummaryCard(totals: Nutrition, profile: UserProfile, fiberIncomplete: Boolean, onOpenDetails: () -> Unit) {
    val remaining = max(0, profile.calorieGoal - totals.calories)
    Card(
        onClick = onOpenDetails,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${totals.calories}", fontSize = 36.sp, fontWeight = FontWeight.Bold)
                Text(" / ${profile.calorieGoal} ${stringResource(R.string.kcal)}", Modifier.padding(bottom = 6.dp))
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text("$remaining", fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.remaining), fontSize = 12.sp)
                }
            }
            SeamlessProgressBar(
                progress = (totals.calories.toFloat() / profile.calorieGoal).coerceIn(0f, 1f),
                modifier = Modifier.fillMaxWidth().height(9.dp),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Macro(R.string.protein, totals.proteinGrams, profile.proteinGoalGrams)
                Macro(R.string.carbs, totals.carbsGrams, profile.carbsGoalGrams)
                Macro(R.string.fat, totals.fatGrams, profile.fatGoalGrams)
                OptionalMacro(R.string.fiber, totals.fiberGrams, profile.fiberGoalGrams, fiberIncomplete)
            }
            if (remaining in 1..250) {
                Text(pluralStringResource(R.plurals.goal_warning, remaining, remaining), color = MaterialTheme.colorScheme.error)
            }
            Text(
                stringResource(R.string.summary_hint),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun SeamlessProgressBar(progress: Float, modifier: Modifier = Modifier) {
    val consumedColor = MaterialTheme.colorScheme.primary
    val remainingColor = MaterialTheme.colorScheme.secondaryContainer
    Canvas(modifier.progressSemantics(progress)) {
        val radius = size.height / 2f
        val cornerRadius = CornerRadius(radius, radius)
        drawRoundRect(color = remainingColor, cornerRadius = cornerRadius)
        val consumedWidth = size.width * progress.coerceIn(0f, 1f)
        if (consumedWidth > 0f) {
            clipRect(right = consumedWidth) {
                drawRoundRect(color = consumedColor, cornerRadius = cornerRadius)
            }
        }
    }
}

@Composable
private fun LeftoverCard(recipe: RecipeTemplate, locale: Locale, onAdd: () -> Unit) {
    val availableBatches = recipe.batches.filter { it.remainingGrams > 0 }
    val remainingGrams = availableBatches.sumOf { it.remainingGrams }
    val remainingPortions = availableBatches.sumOf { it.remainingPortions }
    val representativeBatch = availableBatches.firstOrNull { it.id == recipe.activeBatchId } ?: availableBatches.first()
    Card(
        onClick = onAdd,
        modifier = Modifier.width(290.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            CategoryFoodImage(
                "prepared-meals",
                Modifier.width(62.dp).height(62.dp).clip(RoundedCornerShape(14.dp)),
            )
            Column(Modifier.padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(recipe.name, fontWeight = FontWeight.Bold)
            Text(
                stringResource(
                    R.string.batch_remaining,
                    remainingGrams,
                    formatAmount(remainingPortions, locale),
                ),
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                stringResource(
                    R.string.prepared_on,
                    DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(Date(representativeBatch.cookedAtEpochMillis)),
                ),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MacroDetailsSheet(
    totals: Nutrition,
    profile: UserProfile,
    fiberIncomplete: Boolean,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(stringResource(R.string.macro_breakdown), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            MacroDetail(R.string.protein, totals.proteinGrams, profile.proteinGoalGrams)
            MacroDetail(R.string.carbs, totals.carbsGrams, profile.carbsGoalGrams)
            MacroDetail(R.string.fat, totals.fatGrams, profile.fatGoalGrams)
            OptionalMacroDetail(R.string.fiber, totals.fiberGrams, profile.fiberGoalGrams, fiberIncomplete)
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun OptionalMacroDetail(label: Int, consumed: Double?, goal: Int, incomplete: Boolean) {
    if (consumed != null) {
        MacroDetail(label, consumed, goal)
        if (incomplete) {
            Text(stringResource(R.string.nutrient_partial_entries), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(label), fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.nutrient_unknown_entries), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MacroDetail(label: Int, consumed: Double, goal: Int) {
    val remaining = max(0, goal - consumed.roundToInt())
    val overTarget = consumed > goal
    val emphasisColor = if (overTarget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(label), fontWeight = FontWeight.SemiBold, color = emphasisColor)
            Text("${consumed.roundToInt()} / ${goal}g", color = emphasisColor)
        }
        LinearProgressIndicator(
            progress = { (consumed / goal.coerceAtLeast(1)).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = if (overTarget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        )
        Text(
            "$remaining g ${stringResource(R.string.remaining)}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
        )
    }
}

@Composable private fun Macro(label: Int, value: Double, goal: Int) {
    val overTarget = value > goal
    val color = if (overTarget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    Column {
        Text(stringResource(label), fontSize = 12.sp, color = color)
        Text(
            "${value.roundToInt()} / ${goal}g",
            fontWeight = FontWeight.SemiBold,
            color = if (overTarget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable private fun OptionalMacro(label: Int, value: Double?, goal: Int, incomplete: Boolean) {
    if (value != null && !incomplete) Macro(label, value, goal) else Column {
        Text(stringResource(label), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            if (value == null) "— / ${goal}g" else "≈ ${value.roundToInt()} / ${goal}g",
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun SectionTitle(resource: Int, horizontalPadding: androidx.compose.ui.unit.Dp = 20.dp) {
    Text(
        stringResource(resource),
        Modifier.padding(start = horizontalPadding, end = horizontalPadding, top = 8.dp),
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
    )
}

@Composable private fun EmptyText(resource: Int) {
    Text(stringResource(resource), Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun DuplicateFoodNotice(
    message: String,
    actionLabel: String,
    isError: Boolean,
    onOpen: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 14.dp, top = 8.dp, bottom = 8.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(message, Modifier.weight(1f), fontSize = 13.sp)
            TextButton(onClick = onOpen) { Text(actionLabel) }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun FoodRow(
    food: Food,
    recipe: RecipeTemplate?,
    locale: Locale,
    currentTotals: Nutrition,
    profile: UserProfile,
    standaloneUses: Int,
    ingredientUses: Int,
    onAdd: () -> Unit,
    onEdit: () -> Unit,
) {
    val warnings = food.projectedMacroOverages(currentTotals, profile)
    val kcalLabel = stringResource(R.string.kcal)
    val per100gLabel = stringResource(R.string.per_100g)
    val fiberLabel = stringResource(R.string.fiber).lowercase(locale)
    val detailText = buildString {
        append("${food.nutritionPer100g.calories} $kcalLabel $per100gLabel")
        food.nutritionPer100g.fiberGrams?.let { append(" · ${formatAmount(it, locale)} g $fiberLabel") }
    }
    Card(
        onClick = onAdd,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FoodVisual(
                    food,
                    Modifier.width(72.dp).height(72.dp).clip(RoundedCornerShape(14.dp)),
                )
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(food.name(locale), fontWeight = FontWeight.SemiBold)
                    Text(
                        food.detail(locale),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (food.isPersonal) {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, stringResource(R.string.edit_personal_food))
                        }
                    }
                    IconButton(onClick = onAdd) {
                        Icon(Icons.Default.Add, stringResource(R.string.quick_add), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (food.provenance.type != FoodSourceType.BUILT_IN) {
                    Text(
                        buildString {
                            append(stringResource(food.provenance.type.labelResource()))
                            if (food.provenance.locallyModified) append(" · ${stringResource(R.string.edited_locally)}")
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    detailText,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val usageLabels = buildList {
                    if (standaloneUses > 0) {
                        add(pluralStringResource(R.plurals.logged_as_food, standaloneUses, standaloneUses))
                    }
                    if (ingredientUses > 0) {
                        add(pluralStringResource(R.plurals.used_in_recipe_batches, ingredientUses, ingredientUses))
                    }
                }
                if (usageLabels.isNotEmpty()) {
                    Text(
                        usageLabels.joinToString(" · "),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                recipe?.takeIf { it.batches.any { batch -> batch.remainingGrams > 0 } }?.let {
                    val remainingGrams = it.batches.sumOf { batch -> batch.remainingGrams.coerceAtLeast(0) }
                    val remainingPortions = it.batches.filter { batch -> batch.remainingGrams > 0 }
                        .sumOf { batch -> batch.remainingPortions }
                    Text(
                        stringResource(
                            R.string.batch_remaining,
                            remainingGrams,
                            formatAmount(remainingPortions, locale),
                        ),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (warnings.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        warnings.forEach { warning ->
                            Text(
                                "${stringResource(warning.kind.labelResource())} +${warning.addedGrams.roundToInt()} g · ${warning.comparisonGrams} g",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
    }
}

private const val CATALOGUE_CATEGORY_ALL = "__all__"
private const val CATALOGUE_CATEGORY_PERSONAL = "__personal__"
private const val CATALOGUE_CATEGORY_RECIPES = "__recipes__"

private fun isSpecialCatalogueFilter(value: String) = value.startsWith("__")

private fun Food.matchesCatalogueCategory(category: String?): Boolean = when (category) {
    null, CATALOGUE_CATEGORY_ALL -> true
    CATALOGUE_CATEGORY_PERSONAL -> isPersonal && provenance.type != FoodSourceType.RECIPE
    CATALOGUE_CATEGORY_RECIPES -> provenance.type == FoodSourceType.RECIPE
    else -> categoryKey == category
}

private fun String.labelResource() = when (this) {
    "fruit" -> R.string.category_fruit
    "vegetables" -> R.string.category_vegetables
    "grains" -> R.string.category_grains
    "bakery" -> R.string.category_bakery
    "meat" -> R.string.category_meat
    "fish" -> R.string.category_fish
    "eggs" -> R.string.category_eggs
    "legumes" -> R.string.category_legumes
    "plant-protein" -> R.string.category_plant_protein
    "dairy" -> R.string.category_dairy
    "fats" -> R.string.category_fats
    "pantry" -> R.string.category_pantry
    "nuts-seeds" -> R.string.category_nuts_seeds
    "prepared-meals" -> R.string.category_prepared_meals
    "beverages" -> R.string.category_beverages
    else -> R.string.category_other
}

private fun String?.categoryImageResource() = when (this) {
    "fruit" -> R.drawable.food_category_fruit
    "vegetables" -> R.drawable.food_category_vegetables
    "grains" -> R.drawable.food_category_grains
    "bakery" -> R.drawable.food_category_bakery
    "meat" -> R.drawable.food_category_meat
    "fish" -> R.drawable.food_category_fish
    "eggs" -> R.drawable.food_category_eggs
    "legumes" -> R.drawable.food_category_legumes
    "plant-protein" -> R.drawable.food_category_plant_protein
    "dairy" -> R.drawable.food_category_dairy
    "fats" -> R.drawable.food_category_fats
    "pantry" -> R.drawable.food_category_pantry
    "nuts-seeds" -> R.drawable.food_category_nuts_seeds
    "prepared-meals" -> R.drawable.food_category_prepared_meals
    "beverages" -> R.drawable.food_category_beverages
    else -> R.drawable.food_category_other
}

@Composable
private fun CategoryFoodImage(categoryKey: String?, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(categoryKey.categoryImageResource()),
        contentDescription = stringResource(categoryKey?.labelResource() ?: R.string.category_other),
        contentScale = ContentScale.Crop,
        modifier = modifier,
    )
}

@Composable
private fun FoodVisual(food: Food, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val locale = LocalLocale.current.platformLocale
    val bundledResource = remember(food.id) {
        context.resources.getIdentifier(
            "food_item_${food.id.replace('-', '_')}",
            "drawable",
            context.packageName,
        )
    }
    when {
        food.image?.localPath != null -> FoodImagePreview(requireNotNull(food.image.localPath), modifier)
        bundledResource != 0 -> Image(
            painter = painterResource(bundledResource),
            contentDescription = food.name(locale),
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
        else -> CategoryFoodImage(food.categoryKey, modifier)
    }
}

@Composable
private fun FoodImagePreview(source: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap = remember(source) {
        runCatching {
            if (source.startsWith("content:")) {
                context.contentResolver.openInputStream(Uri.parse(source)).use { BitmapFactory.decodeStream(it) }
            } else {
                BitmapFactory.decodeFile(source)
            }
        }.getOrNull()
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = stringResource(R.string.food_photo),
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    }
}

@Composable
private fun OpenFoodFactsRow(food: Food, locale: Locale, onReview: () -> Unit) {
    Card(
        onClick = onReview,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(food.name(locale), fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    buildString {
                        food.brand?.let { append("$it · ") }
                        append("${food.nutritionPer100g.calories} ${stringResource(R.string.kcal)} · ")
                        append(stringResource(R.string.per_100g))
                    },
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(R.string.review_before_saving),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Icon(Icons.Default.Edit, stringResource(R.string.review_imported_food), tint = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun MacroKind.labelResource() = when (this) {
    MacroKind.PROTEIN -> R.string.protein
    MacroKind.CARBS -> R.string.carbs
    MacroKind.FAT -> R.string.fat
}

private fun NutritionLabelWarning.labelResource() = when (this) {
    NutritionLabelWarning.BASIS_UNKNOWN -> R.string.ocr_basis_unknown
    NutritionLabelWarning.MULTIPLE_COLUMNS_UNCLEAR -> R.string.ocr_columns_unclear
    NutritionLabelWarning.NORMALIZED_FROM_SERVING -> R.string.ocr_normalized_serving
    NutritionLabelWarning.CORE_VALUES_MISSING -> R.string.ocr_core_missing
}

private fun FoodSourceType.labelResource() = when (this) {
    FoodSourceType.BUILT_IN -> R.string.source_built_in
    FoodSourceType.PERSONAL -> R.string.source_personal
    FoodSourceType.OPEN_FOOD_FACTS -> R.string.source_open_food_facts
    FoodSourceType.OCR -> R.string.source_scanned_label
    FoodSourceType.RECIPE -> R.string.source_recipe
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryFoodPickerSheet(
    foods: List<Food>,
    usage: Map<String, List<UnitUsage>>,
    query: String,
    locale: Locale,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSelect: (Food) -> Unit,
) {
    val results = foods.asSequence()
        .filter { query.isBlank() || it.matches(query) }
        .sortedWith(
            compareByDescending<Food> { food -> usage[food.id].orEmpty().sumOf(UnitUsage::useCount) }
                .thenBy { it.name(locale) },
        )
        .take(30)
        .toList()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.add_food_to_day), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
            )
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (results.isEmpty()) item { EmptyText(R.string.no_results) }
                items(results, key = { it.id }) { food ->
                    Card(onClick = { onSelect(food) }) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(food.name(locale), fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${food.nutritionPer100g.calories} ${stringResource(R.string.kcal)} · ${stringResource(R.string.per_100g)}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp,
                                )
                            }
                            Icon(Icons.Default.Add, stringResource(R.string.quick_add))
                        }
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryEditorSheet(
    entry: FoodEntry,
    locale: Locale,
    zoneId: ZoneId,
    maximumRecipeGrams: Int?,
    onDismiss: () -> Unit,
    onSave: (Double, Long) -> Unit,
) {
    val context = LocalContext.current
    val initialDateTime = Instant.ofEpochMilli(entry.consumedAtEpochMillis).atZone(zoneId)
    var amountText by remember(entry.id) { mutableStateOf(formatEditableAmount(entry.enteredAmount)) }
    var dateEpochDay by remember(entry.id) { mutableStateOf(initialDateTime.toLocalDate().toEpochDay()) }
    var hour by remember(entry.id) { mutableStateOf(initialDateTime.hour) }
    var minute by remember(entry.id) { mutableStateOf(initialDateTime.minute) }
    val date = LocalDate.ofEpochDay(dateEpochDay)
    val amount = amountText.replace(',', '.').toDoubleOrNull()
    val gramsPerUnit = entry.grams / entry.enteredAmount.coerceAtLeast(0.001)
    val grams = amount?.let { (it * gramsPerUnit).roundToInt() } ?: 0
    val candidateTimestamp = date.atTime(hour, minute).atZone(zoneId).toInstant().toEpochMilli()
    val valid = amount != null && amount > 0.0 && grams in 1..5_000 &&
        (maximumRecipeGrams == null || grams <= maximumRecipeGrams) && candidateTimestamp <= System.currentTimeMillis()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.edit_diary_entry), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(entry.foodName.forLocale(locale), color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = amountText,
                onValueChange = { candidate ->
                    val filtered = candidate.filter { it.isDigit() || it == '.' || it == ',' }
                    if (filtered.count { it == '.' || it == ',' } <= 1) amountText = filtered
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.amount)) },
                suffix = { Text(entry.unitLabel.forLocale(locale)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
            )
            Text("≈ $grams ${stringResource(R.string.grams_short)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (maximumRecipeGrams != null && grams > maximumRecipeGrams) {
                Text(stringResource(R.string.not_enough_batch_remaining), color = MaterialTheme.colorScheme.error)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        android.app.DatePickerDialog(
                            context,
                            { _, year, month, day -> dateEpochDay = LocalDate.of(year, month + 1, day).toEpochDay() },
                            date.year,
                            date.monthValue - 1,
                            date.dayOfMonth,
                        ).apply { datePicker.maxDate = System.currentTimeMillis() }.show()
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.CalendarMonth, null)
                    Text(date.format(DateTimeFormatter.ofPattern("d MMM yyyy", locale)), Modifier.padding(start = 6.dp))
                }
                OutlinedButton(
                    onClick = {
                        android.app.TimePickerDialog(context, { _, h, m -> hour = h; minute = m }, hour, minute, true).show()
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(String.format(locale, "%02d:%02d", hour, minute))
                }
            }
            Text(stringResource(R.string.entry_edit_snapshot_hint), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            Button(
                enabled = valid,
                onClick = {
                    onSave(requireNotNull(amount), candidateTimestamp)
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.save_changes)) }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun EntryRow(
    entry: FoodEntry,
    locale: Locale,
    onRemove: () -> Unit,
    onEdit: (() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(entry.foodName.forLocale(locale), fontWeight = FontWeight.SemiBold)
            val time = DateFormat.getTimeInstance(DateFormat.SHORT, locale).format(Date(entry.consumedAtEpochMillis))
            val amount = if (entry.unitKey == GRAMS_UNIT_KEY) {
                "${entry.grams}g"
            } else {
                "${formatAmount(entry.enteredAmount, locale)} × ${entry.unitLabel.forLocale(locale)} · ${entry.grams}g"
            }
            val preparation = entry.preparationName?.forLocale(locale)?.let { "$it · " }.orEmpty()
            Text("$preparation$amount · $time", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${entry.nutrition.calories} ${stringResource(R.string.kcal)}", fontWeight = FontWeight.Bold)
            entry.nutrition.fiberGrams?.let {
                Text(
                    "${formatAmount(it, locale)} g ${stringResource(R.string.fiber).lowercase(locale)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        onEdit?.let {
            IconButton(onClick = it) { Icon(Icons.Default.Edit, stringResource(R.string.edit_diary_entry)) }
        }
        IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, stringResource(R.string.remove_entry)) }
    }
    HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outlineVariant)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun QuickAddSheet(
    food: Food,
    recipe: RecipeTemplate?,
    usage: List<UnitUsage>,
    quantityUsage: List<QuantityUsage>,
    preparationUsage: List<PreparationUsage>,
    locale: Locale,
    onDismiss: () -> Unit,
    onCustomize: (() -> Unit)? = null,
    onPrepareBatch: (() -> Unit)? = null,
    onAdd: (Food, Double, Serving?, String?) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val gramsLabel = stringResource(R.string.grams_short)
    val preferredPreparationId = preparationUsage.maxWithOrNull(
        compareBy<PreparationUsage> { it.useCount }.thenBy { it.lastUsedAtEpochMillis },
    )?.preparationId ?: food.defaultPreparationId ?: food.preparations.firstOrNull()?.id
    var selectedPreparationId by remember(food.id) { mutableStateOf(preferredPreparationId) }
    var choosingPreparation by remember(food.id) { mutableStateOf(food.preparations.size > 1) }
    val selectedPreparation = food.preparations.firstOrNull { it.id == selectedPreparationId }
    val preparedFood = selectedPreparation?.let(food::withPreparation) ?: food
    val availableBatches = recipe?.batches.orEmpty().filter { it.remainingGrams > 0 }
    var selectedBatchId by remember(food.id) {
        mutableStateOf(availableBatches.firstOrNull { it.id == recipe?.activeBatchId }?.id ?: availableBatches.firstOrNull()?.id)
    }
    val selectedBatch = availableBatches.firstOrNull { it.id == selectedBatchId }
    val effectiveFood = if (selectedBatch == null) {
        preparedFood
    } else {
        preparedFood.copy(
            nutritionPer100g = selectedBatch.nutritionPer100g,
            servings = food.servings.map { serving ->
                if (serving.id == "${food.id}-portion") serving.copy(grams = selectedBatch.portionGrams) else serving
            },
        )
    }
    val choices = buildUnitChoices(effectiveFood, usage, locale, gramsLabel)
    val presets = buildQuantityPresets(choices, quantityUsage, locale, gramsLabel)
    val servingPresets = presets.filter { it.unitKey != GRAMS_UNIT_KEY }
    val gramPresets = presets.filter { it.unitKey == GRAMS_UNIT_KEY }
    var selectedKey by remember(food.id) { mutableStateOf(choices.first().key) }
    var amountText by remember(food.id) { mutableStateOf(formatEditableAmount(choices.first().baseAmount)) }
    var unitMenuOpen by remember { mutableStateOf(false) }
    val selected = choices.firstOrNull { it.key == selectedKey } ?: choices.first()
    val amount = amountText.replace(',', '.').toDoubleOrNull()
    val grams = amount?.let { (it * (selected.serving?.grams ?: 1)).roundToInt() } ?: 0
    val remainingGrams = selectedBatch?.remainingGrams
    val valid = amount != null && amount > 0.0 && grams in 1..5_000 &&
        (remainingGrams == null || grams <= remainingGrams)
    val nutrition = effectiveFood.nutritionPer100g.forGrams(grams.coerceAtLeast(0))
    val containsAllergens = mutableListOf<String>()
    val mayContainAllergens = mutableListOf<String>()
    food.allergens.forEach { (allergen, declaration) ->
        val label = stringResource(allergen.labelResource())
        if (declaration == AllergenDeclaration.CONTAINS) containsAllergens += label else mayContainAllergens += label
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(food.name(locale), Modifier.weight(1f), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                onCustomize?.let {
                    TextButton(onClick = it) {
                        Icon(Icons.Default.ContentCopy, null)
                        Text(stringResource(R.string.customize), Modifier.padding(start = 6.dp))
                    }
                }
            }
            FoodVisual(
                food,
                Modifier.fillMaxWidth().height(128.dp).clip(RoundedCornerShape(18.dp)),
            )
            if (choosingPreparation) {
                Text(stringResource(R.string.choose_cooking_method), fontWeight = FontWeight.SemiBold)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    maxItemsInEachRow = 2,
                ) {
                    food.preparations.forEach { preparation ->
                        OutlinedButton(
                            onClick = {
                                selectedPreparationId = preparation.id
                                choosingPreparation = false
                            },
                            modifier = Modifier.width(150.dp).height(96.dp),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.RestaurantMenu, null)
                                Text(preparation.names.forLocale(locale), fontWeight = FontWeight.SemiBold)
                                if (preparation.id == preferredPreparationId) {
                                    Text(
                                        stringResource(
                                            if (preparationUsage.isEmpty()) R.string.default_method else R.string.most_used,
                                        ),
                                        fontSize = 11.sp,
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
            Text(effectiveFood.detail(locale), color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (food.preparations.size > 1) {
                TextButton(onClick = { choosingPreparation = true }) {
                    Text(stringResource(R.string.change_cooking_method))
                }
            }
            if (availableBatches.size > 1) {
                Text(stringResource(R.string.choose_batch), fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(availableBatches, key = { it.id }) { batch ->
                        FilterChip(
                            selected = batch.id == selectedBatchId,
                            onClick = {
                                selectedBatchId = batch.id
                                selectedKey = "${food.id}-portion"
                                amountText = "1"
                            },
                            label = {
                                Text(
                                    DateFormat.getDateInstance(DateFormat.SHORT, locale)
                                        .format(Date(batch.cookedAtEpochMillis)),
                                )
                            },
                        )
                    }
                }
            }
            selectedBatch?.let { shownBatch ->
                Text(
                    stringResource(
                        R.string.batch_remaining,
                        shownBatch.remainingGrams,
                        formatAmount(shownBatch.remainingPortions, locale),
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (recipe != null && selectedBatch == null) {
                Text(
                    stringResource(R.string.no_leftovers_recipe_log_hint),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                onPrepareBatch?.let { prepare ->
                    OutlinedButton(onClick = prepare, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.RestaurantMenu, null)
                        Text(stringResource(R.string.prepare_batch), Modifier.padding(start = 8.dp))
                    }
                }
            }
            if (containsAllergens.isNotEmpty()) {
                Text(
                    stringResource(R.string.contains_allergens, containsAllergens.joinToString()),
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (mayContainAllergens.isNotEmpty()) {
                Text(
                    stringResource(R.string.may_contain_allergens, mayContainAllergens.joinToString()),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(stringResource(R.string.choose_serving), fontWeight = FontWeight.SemiBold)
            if (servingPresets.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    maxLines = 2,
                ) {
                    servingPresets.forEach { preset ->
                        QuantityPresetChip(preset, selected.key, amount) {
                            selectedKey = preset.unitKey
                            amountText = formatEditableAmount(preset.amount)
                        }
                    }
                }
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                maxLines = 1,
            ) {
                gramPresets.forEach { preset ->
                    QuantityPresetChip(preset, selected.key, amount) {
                        selectedKey = preset.unitKey
                        amountText = formatEditableAmount(preset.amount)
                    }
                }
            }
            Box(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { candidate ->
                        val filtered = candidate.filter { it.isDigit() || it == '.' || it == ',' }
                        if (filtered.count { it == '.' || it == ',' } <= 1) amountText = filtered
                    },
                    label = { Text(stringResource(R.string.amount)) },
                    suffix = {
                        TextButton(onClick = { unitMenuOpen = true }) { Text(selected.label) }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                DropdownMenu(expanded = unitMenuOpen, onDismissRequest = { unitMenuOpen = false }) {
                    choices.forEach { choice ->
                        DropdownMenuItem(
                            text = { Text(choice.label) },
                            onClick = {
                                selectedKey = choice.key
                                amountText = formatEditableAmount(choice.baseAmount)
                                unitMenuOpen = false
                            },
                        )
                    }
                }
            }
            if (selected.serving != null && valid) {
                Text("≈ $grams ${stringResource(R.string.grams_short)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (remainingGrams != null && grams > remainingGrams) {
                Text(stringResource(R.string.not_enough_batch_remaining), color = MaterialTheme.colorScheme.error)
            }
            nutrition.fiberGrams?.let {
                Text(
                    "${formatAmount(it, locale)} g ${stringResource(R.string.fiber).lowercase(locale)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                enabled = valid,
                onClick = { onAdd(effectiveFood, requireNotNull(amount), selected.serving, selectedBatch?.id) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(pluralStringResource(R.plurals.add_food, nutrition.calories, nutrition.calories))
            }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun QuantityPresetChip(
    preset: QuantityPreset,
    selectedKey: String,
    amount: Double?,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selectedKey == preset.unitKey && amount == preset.amount,
        onClick = onClick,
        label = { Text(preset.label) },
    )
}

private fun formatEditableAmount(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString().trimEnd('0').trimEnd('.')

private fun formatAmount(value: Double, locale: Locale): String =
    NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 2 }.format(value)

private fun Int?.orZero(): Int = this ?: 0
