package com.shadow.calorietracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shadow.calorietracker.R
import com.shadow.calorietracker.model.Food
import com.shadow.calorietracker.model.FoodEntry
import com.shadow.calorietracker.model.Nutrition
import com.shadow.calorietracker.model.sampleFoods
import kotlin.math.max
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalorieQuickApp() {
    val locale = LocalLocale.current.platformLocale
    val entries = remember { mutableStateListOf<FoodEntry>() }
    var query by remember { mutableStateOf("") }
    var selectedFood by remember { mutableStateOf<Food?>(null) }
    val dailyGoal = 2_100
    val totals = entries.fold(Nutrition.Zero) { total, entry -> total + entry.nutrition }
    val filteredFoods = sampleFoods.filter { it.matches(query) }

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
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Text(stringResource(R.string.today), fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(R.string.daily_summary),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                SummaryCard(totals = totals, dailyGoal = dailyGoal)
            }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                )
            }
            item { SectionTitle(if (query.isBlank()) R.string.frequent_foods else R.string.quick_add) }
            if (filteredFoods.isEmpty()) {
                item { EmptyText(R.string.no_results) }
            } else {
                items(filteredFoods, key = { it.id }) { food ->
                    FoodRow(food = food, locale = locale, onAdd = { selectedFood = food })
                }
            }
            item { SectionTitle(R.string.today_entries) }
            if (entries.isEmpty()) {
                item { EmptyText(R.string.no_entries) }
            } else {
                items(entries, key = { it.id }) { entry ->
                    EntryRow(entry = entry, locale = locale, onRemove = { entries.remove(entry) })
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    selectedFood?.let { food ->
        QuickAddSheet(
            food = food,
            locale = locale,
            onDismiss = { selectedFood = null },
            onAdd = { grams ->
                entries.add(0, FoodEntry(food = food, grams = grams))
                selectedFood = null
                query = ""
            },
        )
    }
}

@Composable
private fun SummaryCard(totals: Nutrition, dailyGoal: Int) {
    val remaining = max(0, dailyGoal - totals.calories)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${totals.calories}", fontSize = 36.sp, fontWeight = FontWeight.Bold)
                Text(" / $dailyGoal ${stringResource(R.string.kcal)}", modifier = Modifier.padding(bottom = 6.dp))
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text("$remaining", fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.remaining), fontSize = 12.sp)
                }
            }
            LinearProgressIndicator(
                progress = { (totals.calories.toFloat() / dailyGoal).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(9.dp),
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Macro(R.string.protein, totals.proteinGrams, 140)
                Macro(R.string.carbs, totals.carbsGrams, 230)
                Macro(R.string.fat, totals.fatGrams, 70)
            }
            if (remaining in 1..250) {
                Text(
                    pluralStringResource(R.plurals.goal_warning, remaining, remaining),
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun Macro(label: Int, value: Double, goal: Int) {
    Column {
        Text(stringResource(label), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("${value.roundToInt()} / ${goal}g", fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SectionTitle(resource: Int) {
    Text(
        stringResource(resource),
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp),
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun EmptyText(resource: Int) {
    Text(
        stringResource(resource),
        modifier = Modifier.padding(horizontal = 20.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun FoodRow(food: Food, locale: java.util.Locale, onAdd: () -> Unit) {
    Card(
        onClick = onAdd,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(food.name(locale), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${food.detail(locale)} · ${food.nutritionPer100g.calories} ${stringResource(R.string.kcal)} ${stringResource(R.string.per_100g)}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.quick_add), tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun EntryRow(entry: FoodEntry, locale: java.util.Locale, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.food.name(locale), fontWeight = FontWeight.SemiBold)
            Text("${entry.grams}g", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
        Text("${entry.nutrition.calories} ${stringResource(R.string.kcal)}", fontWeight = FontWeight.Bold)
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.remove_entry))
        }
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outlineVariant)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickAddSheet(food: Food, locale: java.util.Locale, onDismiss: () -> Unit, onAdd: (Int) -> Unit) {
    var grams by remember(food.id) { mutableIntStateOf(100) }
    val nutrition = food.nutritionPer100g.forGrams(grams)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(food.name(locale), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(food.detail(locale), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.amount), fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(50, 100, 150, 200).forEach { preset ->
                    AssistChip(onClick = { grams = preset }, label = { Text("$preset g") })
                }
            }
            OutlinedTextField(
                value = grams.toString(),
                onValueChange = { value -> grams = value.filter(Char::isDigit).toIntOrNull()?.coerceIn(1, 5000) ?: 1 },
                label = { Text(stringResource(R.string.amount)) },
                suffix = { Text(stringResource(R.string.grams_short)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = { onAdd(grams) }, modifier = Modifier.fillMaxWidth()) {
                Text(pluralStringResource(R.plurals.add_food, nutrition.calories, nutrition.calories))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
