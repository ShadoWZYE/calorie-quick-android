package com.shadow.calorietracker.ui

import androidx.appcompat.app.AppCompatDelegate
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shadow.calorietracker.R
import com.shadow.calorietracker.model.ActivityLevel
import com.shadow.calorietracker.model.EnergyEstimator
import com.shadow.calorietracker.model.Food
import com.shadow.calorietracker.model.FoodEntry
import com.shadow.calorietracker.model.FormulaSex
import com.shadow.calorietracker.model.FoodRecommender
import com.shadow.calorietracker.model.GoalType
import com.shadow.calorietracker.model.MacroKind
import com.shadow.calorietracker.model.Nutrition
import com.shadow.calorietracker.model.QuantityUsage
import com.shadow.calorietracker.model.Serving
import com.shadow.calorietracker.model.TargetMode
import com.shadow.calorietracker.model.UnitUsage
import com.shadow.calorietracker.model.UserProfile
import com.shadow.calorietracker.model.projectedMacroOverages
import com.shadow.calorietracker.data.GRAMS_UNIT_KEY
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Locale
import java.util.Date
import kotlin.math.max
import kotlin.math.roundToInt

private enum class AppScreen { TODAY, SETTINGS }

@Composable
fun CalorieQuickApp(viewModel: CalorieViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val profile = state.profile
    var screenName by rememberSaveable { mutableStateOf(AppScreen.TODAY.name) }

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
        else -> TodayScreen(
            state = state,
            onOpenSettings = { screenName = AppScreen.SETTINGS.name },
            onAdd = viewModel::addEntry,
            onDelete = viewModel::deleteEntry,
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
    val customTargetsValid = customCalories in 500..10_000 && customProtein in 1..1_000 &&
        customCarbs in 1..1_000 && customFat in 1..1_000
    val valid = measurementsValid && (targetMode == TargetMode.ESTIMATED || customTargetsValid)

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
                            { calorieTarget = it },
                            R.string.calorie_target,
                            Modifier.fillMaxWidth(),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            NumericField(proteinTarget, { proteinTarget = it }, R.string.protein_g, Modifier.weight(1f))
                            NumericField(carbsTarget, { carbsTarget = it }, R.string.carbs_g, Modifier.weight(1f))
                            NumericField(fatTarget, { fatTarget = it }, R.string.fat_g, Modifier.weight(1f))
                        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodayScreen(
    state: AppUiState,
    onOpenSettings: () -> Unit,
    onAdd: (Food, Double, Serving?) -> Unit,
    onDelete: (FoodEntry) -> Unit,
) {
    val locale = LocalLocale.current.platformLocale
    var query by rememberSaveable { mutableStateOf("") }
    var selectedFood by remember { mutableStateOf<Food?>(null) }
    var showMacroDetails by rememberSaveable { mutableStateOf(false) }
    val profile = requireNotNull(state.profile)
    val frequencyByFoodId = state.unitUsage.mapValues { (_, usages) -> usages.sumOf(UnitUsage::useCount) }
    val displayedFoods = if (query.isBlank()) {
        FoodRecommender.rank(state.foods, state.totals, profile, frequencyByFoodId)
    } else {
        state.foods.filter { it.matches(query) }
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
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, stringResource(R.string.settings))
                    }
                }
            }
            item { SummaryCard(state.totals, profile) { showMacroDetails = true } }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, stringResource(R.string.close))
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                )
            }
            item { SectionTitle(if (query.isBlank()) R.string.recommended_foods else R.string.quick_add) }
            if (displayedFoods.isEmpty()) item { EmptyText(R.string.no_results) }
            else items(displayedFoods, key = { it.id }) {
                FoodRow(it, locale, state.totals, profile) { selectedFood = it }
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
            state.unitUsage[food.id].orEmpty(),
            state.quantityUsage[food.id].orEmpty(),
            locale,
            { selectedFood = null },
        ) { amount, serving ->
            onAdd(food, amount, serving)
            selectedFood = null
            query = ""
        }
    }
    if (showMacroDetails) {
        MacroDetailsSheet(state.totals, profile) { showMacroDetails = false }
    }
}

@Composable
private fun SummaryCard(totals: Nutrition, profile: UserProfile, onOpenDetails: () -> Unit) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MacroDetailsSheet(totals: Nutrition, profile: UserProfile, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(stringResource(R.string.macro_breakdown), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            MacroDetail(R.string.protein, totals.proteinGrams, profile.proteinGoalGrams)
            MacroDetail(R.string.carbs, totals.carbsGrams, profile.carbsGoalGrams)
            MacroDetail(R.string.fat, totals.fatGrams, profile.fatGoalGrams)
            Spacer(Modifier.height(12.dp))
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
private fun FoodRow(
    food: Food,
    locale: Locale,
    currentTotals: Nutrition,
    profile: UserProfile,
    onAdd: () -> Unit,
) {
    val warnings = food.projectedMacroOverages(currentTotals, profile)
    Card(
        onClick = onAdd,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(food.name(locale), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${food.detail(locale)} · ${food.nutritionPer100g.calories} ${stringResource(R.string.kcal)} ${stringResource(R.string.per_100g)}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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
            Icon(Icons.Default.Add, stringResource(R.string.quick_add), tint = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun MacroKind.labelResource() = when (this) {
    MacroKind.PROTEIN -> R.string.protein
    MacroKind.CARBS -> R.string.carbs
    MacroKind.FAT -> R.string.fat
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
        Text("${entry.nutrition.calories} ${stringResource(R.string.kcal)}", fontWeight = FontWeight.Bold)
        IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, stringResource(R.string.remove_entry)) }
    }
    HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outlineVariant)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun QuickAddSheet(
    food: Food,
    usage: List<UnitUsage>,
    quantityUsage: List<QuantityUsage>,
    locale: Locale,
    onDismiss: () -> Unit,
    onAdd: (Double, Serving?) -> Unit,
) {
    val gramsLabel = stringResource(R.string.grams_short)
    val choices = buildUnitChoices(food, usage, locale, gramsLabel)
    val presets = buildQuantityPresets(choices, quantityUsage, locale, gramsLabel)
    val servingPresets = presets.filter { it.unitKey != GRAMS_UNIT_KEY }
    val gramPresets = presets.filter { it.unitKey == GRAMS_UNIT_KEY }
    var selectedKey by remember(food.id) { mutableStateOf(choices.first().key) }
    var amountText by remember(food.id) { mutableStateOf(formatEditableAmount(choices.first().baseAmount)) }
    var unitMenuOpen by remember { mutableStateOf(false) }
    val selected = choices.firstOrNull { it.key == selectedKey } ?: choices.first()
    val amount = amountText.replace(',', '.').toDoubleOrNull()
    val grams = amount?.let { (it * (selected.serving?.grams ?: 1)).roundToInt() } ?: 0
    val valid = amount != null && amount > 0.0 && grams in 1..5_000
    val nutrition = food.nutritionPer100g.forGrams(grams.coerceAtLeast(0))
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
            Button(
                enabled = valid,
                onClick = { onAdd(requireNotNull(amount), selected.serving) },
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
