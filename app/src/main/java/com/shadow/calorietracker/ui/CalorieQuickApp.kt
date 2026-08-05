package com.shadow.calorietracker.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.shadow.calorietracker.model.GoalType
import com.shadow.calorietracker.model.Nutrition
import com.shadow.calorietracker.model.UserProfile
import java.text.DateFormat
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

    val ageValue = age.toIntOrNull()
    val heightValue = height.toIntOrNull()
    val weightValue = weight.toDoubleOrNull()
    val valid = ageValue in 18..100 && heightValue in 120..230 && weightValue != null && weightValue in 35.0..300.0
    val validatedAge = ageValue ?: 0
    val validatedHeight = heightValue ?: 0
    val validatedWeight = weightValue ?: 0.0
    val sex = FormulaSex.valueOf(sexName)
    val activity = ActivityLevel.valueOf(activityName)
    val goal = GoalType.valueOf(goalName)
    val estimate = if (valid) {
        EnergyEstimator.dailyGoal(validatedAge, validatedHeight, validatedWeight, sex, activity, goal)
    } else 0

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
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(18.dp)) {
                    Text(stringResource(R.string.estimated_daily_goal), fontWeight = FontWeight.SemiBold)
                    Text("$estimate ${stringResource(R.string.kcal)}", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.estimate_disclaimer), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Button(
                enabled = valid,
                onClick = {
                    val protein = (validatedWeight * 1.6).roundToInt()
                    val fat = (validatedWeight * 0.8).roundToInt()
                    val carbs = max(0, ((estimate - protein * 4 - fat * 9) / 4.0).roundToInt())
                    onSave(
                        UserProfile(
                            true,
                            validatedAge,
                            validatedHeight,
                            validatedWeight,
                            sex,
                            activity,
                            goal,
                            estimate,
                            protein,
                            carbs,
                            fat,
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
        onValueChange = { text -> onChange(text.filter { it.isDigit() || decimal && it == '.' }) },
        label = { Text(stringResource(label)) },
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        singleLine = true,
        modifier = modifier,
    )
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
    onAdd: (Food, Int) -> Unit,
    onDelete: (FoodEntry) -> Unit,
) {
    val locale = LocalLocale.current.platformLocale
    var query by rememberSaveable { mutableStateOf("") }
    var selectedFood by remember { mutableStateOf<Food?>(null) }
    val profile = requireNotNull(state.profile)
    val filteredFoods = state.foods.filter { it.matches(query) }

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
            item { SummaryCard(state.totals, profile) }
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
            item { SectionTitle(if (query.isBlank()) R.string.frequent_foods else R.string.quick_add) }
            if (filteredFoods.isEmpty()) item { EmptyText(R.string.no_results) }
            else items(filteredFoods, key = { it.id }) { FoodRow(it, locale) { selectedFood = it } }
            item { SectionTitle(R.string.today_entries) }
            if (state.entries.isEmpty()) item { EmptyText(R.string.no_entries) }
            else items(state.entries, key = { it.id }) { EntryRow(it, locale) { onDelete(it) } }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    selectedFood?.let { food ->
        QuickAddSheet(food, locale, { selectedFood = null }) { grams ->
            onAdd(food, grams)
            selectedFood = null
            query = ""
        }
    }
}

@Composable
private fun SummaryCard(totals: Nutrition, profile: UserProfile) {
    val remaining = max(0, profile.calorieGoal - totals.calories)
    Card(
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
        }
    }
}

@Composable private fun Macro(label: Int, value: Double, goal: Int) {
    Column {
        Text(stringResource(label), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("${value.roundToInt()} / ${goal}g", fontWeight = FontWeight.SemiBold)
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
private fun FoodRow(food: Food, locale: Locale, onAdd: () -> Unit) {
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
            }
            Icon(Icons.Default.Add, stringResource(R.string.quick_add), tint = MaterialTheme.colorScheme.primary)
        }
    }
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
            Text("${entry.grams}g · $time", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
        Text("${entry.nutrition.calories} ${stringResource(R.string.kcal)}", fontWeight = FontWeight.Bold)
        IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, stringResource(R.string.remove_entry)) }
    }
    HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outlineVariant)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickAddSheet(food: Food, locale: Locale, onDismiss: () -> Unit, onAdd: (Int) -> Unit) {
    var grams by remember(food.id) { mutableIntStateOf(food.servings.firstOrNull()?.grams ?: 100) }
    val nutrition = food.nutritionPer100g.forGrams(grams)
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
            food.servings.forEach { serving ->
                FilterChip(
                    selected = grams == serving.grams,
                    onClick = { grams = serving.grams },
                    label = { Text("${serving.label.forLocale(locale)} · ${serving.grams} g") },
                )
            }
            FilterChip(selected = grams == 100, onClick = { grams = 100 }, label = { Text("100 g") })
            OutlinedTextField(
                value = grams.toString(),
                onValueChange = { grams = it.filter(Char::isDigit).toIntOrNull()?.coerceIn(1, 5_000) ?: 1 },
                label = { Text(stringResource(R.string.exact_grams)) },
                suffix = { Text(stringResource(R.string.grams_short)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = { onAdd(grams) }, Modifier.fillMaxWidth()) {
                Text(pluralStringResource(R.plurals.add_food, nutrition.calories, nutrition.calories))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
