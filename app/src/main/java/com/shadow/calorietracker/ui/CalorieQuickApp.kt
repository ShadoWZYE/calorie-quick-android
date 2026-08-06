package com.shadow.calorietracker.ui

import android.net.Uri
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shadow.calorietracker.R
import com.shadow.calorietracker.model.ActivityLevel
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
import com.shadow.calorietracker.model.RecipeCalculator
import com.shadow.calorietracker.model.RecipeDraft
import com.shadow.calorietracker.model.RecipeIngredientDraft
import com.shadow.calorietracker.model.RecipeTemplate
import com.shadow.calorietracker.model.Serving
import com.shadow.calorietracker.model.TargetMode
import com.shadow.calorietracker.model.UnitUsage
import com.shadow.calorietracker.model.UserProfile
import com.shadow.calorietracker.model.projectedMacroOverages
import com.shadow.calorietracker.data.GRAMS_UNIT_KEY
import com.shadow.calorietracker.data.NutritionLabelPrefill
import com.shadow.calorietracker.data.NutritionLabelWarning
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
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.roundToInt

private enum class AppScreen { TODAY, HISTORY, SETTINGS, FOOD_EDITOR, RECIPE_EDITOR }

@Composable
fun CalorieQuickApp(viewModel: CalorieViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lookupState by viewModel.foodLookupState.collectAsStateWithLifecycle()
    val nutritionLabelScanState by viewModel.nutritionLabelScanState.collectAsStateWithLifecycle()
    val profile = state.profile
    var screenName by rememberSaveable { mutableStateOf(AppScreen.TODAY.name) }
    var editingFood by remember { mutableStateOf<Food?>(null) }
    var newFoodName by rememberSaveable { mutableStateOf("") }
    var newFoodBarcode by rememberSaveable { mutableStateOf("") }
    var nutritionLabelPrefill by remember { mutableStateOf<NutritionLabelPrefill?>(null) }
    var editingRecipe by remember { mutableStateOf<RecipeTemplate?>(null) }

    LaunchedEffect(nutritionLabelScanState.status, nutritionLabelScanState.prefill) {
        if (nutritionLabelScanState.status == NutritionLabelScanStatus.SUCCESS) {
            nutritionLabelScanState.prefill?.let { prefill ->
                editingFood = null
                nutritionLabelPrefill = prefill
                newFoodName = prefill.suggestedName
                newFoodBarcode = prefill.barcode.orEmpty()
                screenName = AppScreen.FOOD_EDITOR.name
            }
            viewModel.clearNutritionLabelScan()
        }
    }

    when {
        !state.loaded -> LoadingScreen()
        profile == null || !profile.onboardingComplete -> OnboardingScreen(viewModel::saveProfile)
        screenName == AppScreen.SETTINGS.name -> SettingsScreen(
            profile = profile,
            onBack = { screenName = AppScreen.TODAY.name },
            onSave = {
                viewModel.saveProfile(it)
                screenName = AppScreen.TODAY.name
            },
        )
        screenName == AppScreen.HISTORY.name -> HistoryScreen(
            state = state,
            onBack = { screenName = AppScreen.TODAY.name },
            onDelete = viewModel::deleteEntry,
        )
        screenName == AppScreen.FOOD_EDITOR.name -> PersonalFoodEditorScreen(
            initial = editingFood,
            initialName = newFoodName,
            initialBarcode = newFoodBarcode,
            nutritionLabelPrefill = nutritionLabelPrefill,
            existingFoods = state.foods,
            onBack = { screenName = AppScreen.TODAY.name },
            onSave = {
                viewModel.savePersonalFood(it)
                screenName = AppScreen.TODAY.name
            },
            onArchive = { foodId ->
                viewModel.archivePersonalFood(foodId)
                screenName = AppScreen.TODAY.name
            },
            onOpenExistingFood = {
                editingFood = it
                nutritionLabelPrefill = null
                newFoodName = ""
                newFoodBarcode = ""
            },
        )
        screenName == AppScreen.RECIPE_EDITOR.name -> RecipeEditorScreen(
            initial = editingRecipe,
            foods = state.foods,
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
            onAdd = viewModel::addEntry,
            onDelete = viewModel::deleteEntry,
            onCreateFood = {
                editingFood = null
                nutritionLabelPrefill = null
                newFoodName = it.first
                newFoodBarcode = it.second.orEmpty()
                screenName = AppScreen.FOOD_EDITOR.name
            },
            onEditFood = {
                if (it.provenance.type == FoodSourceType.RECIPE) {
                    state.recipes[it.id]?.let { recipe ->
                        editingRecipe = recipe
                        screenName = AppScreen.RECIPE_EDITOR.name
                    }
                } else {
                    editingFood = it
                    nutritionLabelPrefill = null
                    newFoodBarcode = ""
                    screenName = AppScreen.FOOD_EDITOR.name
                }
            },
            onCreateRecipe = {
                editingRecipe = null
                screenName = AppScreen.RECIPE_EDITOR.name
            },
            lookupState = lookupState,
            nutritionLabelScanState = nutritionLabelScanState,
            onSearchGlobal = viewModel::searchOpenFoodFacts,
            onLookupBarcode = viewModel::lookupBarcode,
            onClearLookup = viewModel::clearFoodLookup,
            onScanNutritionLabel = viewModel::scanNutritionLabel,
            onClearNutritionLabelScan = viewModel::clearNutritionLabelScan,
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
private fun SettingsScreen(profile: UserProfile, onBack: () -> Unit, onSave: (UserProfile) -> Unit) {
    ProfileForm(
        title = stringResource(R.string.settings),
        subtitle = stringResource(R.string.settings_subtitle),
        initial = profile,
        actionLabel = stringResource(R.string.save_changes),
        showLanguageIntro = false,
        onBack = onBack,
        onSave = onSave,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PersonalFoodEditorScreen(
    initial: Food?,
    initialName: String,
    initialBarcode: String,
    nutritionLabelPrefill: NutritionLabelPrefill?,
    existingFoods: List<Food>,
    onBack: () -> Unit,
    onSave: (PersonalFoodDraft) -> Unit,
    onArchive: (String) -> Unit,
    onOpenExistingFood: (Food) -> Unit,
) {
    val locale = LocalLocale.current.platformLocale
    val initialPackageMeasure = initial?.takeIf(Food::isPackaged)?.servings?.firstOrNull(Serving::isPackage)
    var name by rememberSaveable(initial?.id, initialName, nutritionLabelPrefill) {
        mutableStateOf(initial?.name(locale) ?: nutritionLabelPrefill?.suggestedName ?: initialName)
    }
    var brand by rememberSaveable(initial?.id) { mutableStateOf(initial?.brand.orEmpty()) }
    var barcode by rememberSaveable(initial?.id, initialBarcode, nutritionLabelPrefill) {
        mutableStateOf(initial?.barcode ?: nutritionLabelPrefill?.barcode ?: initialBarcode)
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
                PersonalMeasure(it.id, it.label, it.grams, it.suggestedAmounts)
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
    val caloriesValue = calories.toIntOrNull()
    val proteinValue = protein.localizedDoubleOrNull()
    val carbsValue = carbs.localizedDoubleOrNull()
    val fatValue = fat.localizedDoubleOrNull()
    val fiberValue = fiber.localizedDoubleOrNull()
    val packageWeightValue = packageWeight.toIntOrNull()
    val provenance = initial?.provenance ?: FoodProvenance(
        if (nutritionLabelPrefill != null) FoodSourceType.OCR else FoodSourceType.PERSONAL,
    )
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
            Button(
                enabled = valid,
                onClick = {
                    onSave(
                        PersonalFoodDraft(
                            id = initial?.id,
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
                                        id = initialPackageMeasure?.id,
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
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.save_food)) }
        }
        if (initial?.isPersonal == true) {
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
    foods: List<Food>,
    onBack: () -> Unit,
    onSave: (RecipeDraft) -> Unit,
    onArchive: (String) -> Unit,
) {
    val locale = LocalLocale.current.platformLocale
    var name by rememberSaveable(initial?.activeBatchId) { mutableStateOf(initial?.name.orEmpty()) }
    var ingredientQuery by rememberSaveable(initial?.activeBatchId) { mutableStateOf("") }
    var ingredients by remember(initial?.activeBatchId) {
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
            },
        )
    }
    var cookedYield by rememberSaveable(initial?.activeBatchId) {
        mutableStateOf(initial?.cookedYieldGrams?.toString().orEmpty())
    }
    var portionCount by rememberSaveable(initial?.activeBatchId) {
        mutableStateOf(initial?.portionCount?.toString() ?: "4")
    }
    var yieldManuallyEdited by rememberSaveable(initial?.activeBatchId) { mutableStateOf(initial != null) }
    var confirmArchive by remember { mutableStateOf(false) }
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
    val searchResults = if (ingredientQuery.isBlank()) {
        emptyList()
    } else {
        foods.asSequence()
            .filter { it.provenance.type != FoodSourceType.RECIPE }
            .filter { food -> ingredients.none { it.foodId == food.id } }
            .filter { it.matches(ingredientQuery) }
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
        items(searchResults, key = { "recipe-search-${it.id}" }) { food ->
            Card(
                onClick = {
                    updateIngredients(
                        ingredients + RecipeIngredientDraft(
                            food.id,
                            food.names,
                            food.nutritionPer100g,
                            food.allergens,
                            100,
                        ),
                    )
                    ingredientQuery = ""
                },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            ) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(food.name(locale), fontWeight = FontWeight.SemiBold)
                        Text(
                            "${food.nutritionPer100g.calories} ${stringResource(R.string.kcal)} · ${stringResource(R.string.per_100g)}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                        )
                    }
                    Icon(Icons.Default.Add, stringResource(R.string.add_ingredient))
                }
            }
        }
        if (ingredientQuery.isNotBlank() && searchResults.isEmpty()) {
            item { Text(stringResource(R.string.no_saved_ingredient), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(ingredients, key = { "recipe-ingredient-${it.foodId}" }) { ingredient ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 14.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(ingredient.foodName.forLocale(locale), fontWeight = FontWeight.SemiBold)
                        Text(
                            "${ingredient.nutritionPer100g.forGrams(ingredient.grams).calories} ${stringResource(R.string.kcal)}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                        )
                    }
                    NumericField(
                        value = ingredient.grams.takeIf { it > 0 }?.toString().orEmpty(),
                        onChange = { value ->
                            val grams = value.toIntOrNull() ?: 0
                            updateIngredients(ingredients.map { if (it.foodId == ingredient.foodId) it.copy(grams = grams) else it })
                        },
                        label = R.string.grams_short,
                        modifier = Modifier.weight(0.55f),
                    )
                    IconButton(onClick = { updateIngredients(ingredients - ingredient) }) {
                        Icon(Icons.Default.Delete, stringResource(R.string.remove_ingredient))
                    }
                }
            }
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
    onSave: (UserProfile) -> Unit,
    onBack: (() -> Unit)? = null,
) {
    var age by rememberSaveable { mutableStateOf((initial?.age ?: 30).toString()) }
    var height by rememberSaveable { mutableStateOf((initial?.heightCm ?: 175).toString()) }
    var weight by rememberSaveable { mutableStateOf((initial?.weightKg ?: 75.0).toString().trimEnd('0').trimEnd('.')) }
    var sexName by rememberSaveable { mutableStateOf((initial?.formulaSex ?: FormulaSex.FEMALE).name) }
    var activityName by rememberSaveable { mutableStateOf((initial?.activityLevel ?: ActivityLevel.LIGHT).name) }
    var goalName by rememberSaveable { mutableStateOf((initial?.goalType ?: GoalType.MAINTAIN).name) }
    var targetModeName by rememberSaveable { mutableStateOf((initial?.targetMode ?: TargetMode.ESTIMATED).name) }
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
        item {
            Button(
                enabled = valid,
                onClick = {
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
                    onSave(
                        UserProfile(
                            true,
                            validatedAge,
                            validatedHeight,
                            validatedWeight,
                            sex,
                            activity,
                            goal,
                            targets.calories,
                            targets.proteinGrams,
                            targets.carbsGrams,
                            targets.fatGrams,
                            targets.fiberGrams,
                            targetMode,
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(actionLabel) }
        }
        item { Spacer(Modifier.height(24.dp)) }
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

private enum class HistoryRange(val days: Long) { DAYS(7), WEEKS(28), MONTHS(183) }

private data class ReportBucket(val label: String, val calories: Int, val loggedDays: Int)

@Composable
private fun HistoryScreen(
    state: AppUiState,
    onBack: () -> Unit,
    onDelete: (FoodEntry) -> Unit,
) {
    val context = LocalContext.current
    val locale = LocalLocale.current.platformLocale
    val zoneId = ZoneId.systemDefault()
    val today = LocalDate.now(zoneId)
    var selectedEpochDay by rememberSaveable { mutableStateOf(today.toEpochDay()) }
    var rangeName by rememberSaveable { mutableStateOf(HistoryRange.DAYS.name) }
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
            items(selectedEntries, key = { it.id }) { entry -> EntryRow(entry, locale) { onDelete(entry) } }
        }
        item { Spacer(Modifier.height(24.dp)) }
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
    onAdd: (Food, Double, Serving?, String?) -> Unit,
    onDelete: (FoodEntry) -> Unit,
    onCreateFood: (Pair<String, String?>) -> Unit,
    onCreateRecipe: () -> Unit,
    onEditFood: (Food) -> Unit,
    onSearchGlobal: (String, String) -> Unit,
    onLookupBarcode: (String, String) -> Unit,
    onClearLookup: () -> Unit,
    onScanNutritionLabel: (Uri, String) -> Unit,
    onClearNutritionLabelScan: () -> Unit,
    onImportFood: (Food) -> Unit,
) {
    val context = LocalContext.current
    val locale = LocalLocale.current.platformLocale
    var pendingCameraUri by rememberSaveable { mutableStateOf<String?>(null) }
    var showLabelSourceDialog by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
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
    var showMacroDetails by rememberSaveable { mutableStateOf(false) }
    val profile = requireNotNull(state.profile)
    val frequencyByFoodId = state.unitUsage.mapValues { (_, usages) -> usages.sumOf(UnitUsage::useCount) }
    val displayedFoods = if (query.isBlank()) {
        FoodRecommender.rank(state.foods, state.totals, profile, frequencyByFoodId)
    } else {
        state.foods.filter { it.matches(query) }
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
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.today), fontSize = 30.sp, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.daily_summary), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onOpenHistory) {
                        Icon(Icons.Default.History, stringResource(R.string.history))
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, stringResource(R.string.settings))
                    }
                }
            }
            item { SummaryCard(state.totals, profile, state.fiberIncomplete) { showMacroDetails = true } }
            if (state.recipes.isNotEmpty()) {
                item { SectionTitle(R.string.meal_prep) }
                item {
                    LazyRow(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(state.recipes.values.sortedBy { it.name }, key = { it.foodId }) { recipe ->
                            MealPrepCard(recipe, locale) {
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
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.ocr_scan_error),
                            Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.error,
                        )
                        TextButton(onClick = {
                            onClearNutritionLabelScan()
                            showLabelSourceDialog = true
                        }) { Text(stringResource(R.string.retry)) }
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
            item { SectionTitle(if (query.isBlank()) R.string.recommended_foods else R.string.quick_add) }
            if (displayedFoods.isEmpty()) item { EmptyText(R.string.no_results) }
            else items(displayedFoods, key = { it.id }) {
                FoodRow(it, state.recipes[it.id], locale, state.totals, profile, { selectedFood = it }) { onEditFood(it) }
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
            item { SectionTitle(R.string.today_entries) }
            if (state.entries.isEmpty()) item { EmptyText(R.string.no_entries) }
            else items(state.entries, key = { it.id }) { EntryRow(it, locale) { onDelete(it) } }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    selectedFood?.let { food ->
        QuickAddSheet(
            food,
            state.recipes[food.id],
            state.unitUsage[food.id].orEmpty(),
            state.quantityUsage[food.id].orEmpty(),
            locale,
            { selectedFood = null },
        ) { loggedFood, amount, serving, batchId ->
            onAdd(loggedFood, amount, serving, batchId)
            selectedFood = null
            query = ""
        }
    }
    if (showMacroDetails) {
        MacroDetailsSheet(state.totals, profile, state.fiberIncomplete) { showMacroDetails = false }
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
            LinearProgressIndicator(
                progress = { (totals.calories.toFloat() / profile.calorieGoal).coerceIn(0f, 1f) },
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
private fun MealPrepCard(recipe: RecipeTemplate, locale: Locale, onAdd: () -> Unit) {
    val portionGrams = (recipe.cookedYieldGrams / recipe.portionCount.toDouble()).coerceAtLeast(1.0)
    Card(
        onClick = onAdd,
        enabled = recipe.remainingGrams > 0,
        modifier = Modifier.width(270.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(recipe.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                stringResource(
                    R.string.batch_remaining,
                    recipe.remainingGrams,
                    formatAmount(recipe.remainingGrams / portionGrams, locale),
                ),
                color = if (recipe.remainingGrams > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
            Text(
                stringResource(
                    R.string.prepared_on,
                    DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(Date(recipe.cookedAtEpochMillis)),
                ),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
private fun FoodRow(
    food: Food,
    recipe: RecipeTemplate?,
    locale: Locale,
    currentTotals: Nutrition,
    profile: UserProfile,
    onAdd: () -> Unit,
    onEdit: () -> Unit,
) {
    val warnings = food.projectedMacroOverages(currentTotals, profile)
    val kcalLabel = stringResource(R.string.kcal)
    val per100gLabel = stringResource(R.string.per_100g)
    val fiberLabel = stringResource(R.string.fiber).lowercase(locale)
    val detailText = buildString {
        append("${food.detail(locale)} · ${food.nutritionPer100g.calories} $kcalLabel $per100gLabel")
        food.nutritionPer100g.fiberGrams?.let { append(" · ${formatAmount(it, locale)} g $fiberLabel") }
    }
    Card(
        onClick = onAdd,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(food.name(locale), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                recipe?.let {
                    val portionGrams = (it.cookedYieldGrams / it.portionCount.toDouble()).coerceAtLeast(1.0)
                    Text(
                        stringResource(
                            R.string.batch_remaining,
                            it.remainingGrams,
                            formatAmount(it.remainingGrams / portionGrams, locale),
                        ),
                        fontSize = 12.sp,
                        color = if (it.remainingGrams > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    )
                }
                if (warnings.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
            if (food.isPersonal) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, stringResource(R.string.edit_personal_food))
                }
            }
            Icon(Icons.Default.Add, stringResource(R.string.quick_add), tint = MaterialTheme.colorScheme.primary)
        }
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

@Composable
private fun EntryRow(entry: FoodEntry, locale: Locale, onRemove: () -> Unit) {
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
            Text("$amount · $time", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
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
    locale: Locale,
    onDismiss: () -> Unit,
    onAdd: (Food, Double, Serving?, String?) -> Unit,
) {
    val gramsLabel = stringResource(R.string.grams_short)
    val availableBatches = recipe?.batches.orEmpty().filter { it.remainingGrams > 0 }
    var selectedBatchId by remember(food.id) {
        mutableStateOf(availableBatches.firstOrNull { it.id == recipe?.activeBatchId }?.id ?: availableBatches.firstOrNull()?.id)
    }
    val selectedBatch = availableBatches.firstOrNull { it.id == selectedBatchId }
    val effectiveFood = if (selectedBatch == null) {
        food
    } else {
        food.copy(
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
    val remainingGrams = selectedBatch?.remainingGrams ?: recipe?.remainingGrams
    val valid = amount != null && amount > 0.0 && grams in 1..5_000 &&
        (remainingGrams == null || grams <= remainingGrams)
    val nutrition = effectiveFood.nutritionPer100g.forGrams(grams.coerceAtLeast(0))
    val containsAllergens = mutableListOf<String>()
    val mayContainAllergens = mutableListOf<String>()
    food.allergens.forEach { (allergen, declaration) ->
        val label = stringResource(allergen.labelResource())
        if (declaration == AllergenDeclaration.CONTAINS) containsAllergens += label else mayContainAllergens += label
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(food.name(locale), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(food.detail(locale), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            recipe?.let {
                val shownBatch = selectedBatch ?: it.batches.firstOrNull { batch -> batch.id == it.activeBatchId }
                val shownRemaining = shownBatch?.remainingGrams ?: it.remainingGrams
                val portionGrams = shownBatch?.portionGrams?.toDouble()
                    ?: (it.cookedYieldGrams / it.portionCount.toDouble()).coerceAtLeast(1.0)
                Text(
                    stringResource(
                        R.string.batch_remaining,
                        shownRemaining,
                        formatAmount(shownRemaining / portionGrams, locale),
                    ),
                    color = if (shownRemaining > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold,
                )
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
