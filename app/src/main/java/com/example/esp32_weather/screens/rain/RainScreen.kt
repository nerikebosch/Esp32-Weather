package com.example.esp32_weather.screens.rain

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.esp32_weather.viewmodel.RainBarItem
import com.example.esp32_weather.viewmodel.RainPeriod
import com.example.esp32_weather.viewmodel.WeatherViewModel
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RainScreen(viewModel: WeatherViewModel) {
    val todayRain by viewModel.todayRainAmount.collectAsState()
    val graphData by viewModel.rainGraphData.collectAsState()
    val selectedPeriod by viewModel.selectedRainPeriod.collectAsState()
    val selectedDate by viewModel.selectedRainDate.collectAsState()

    var inputAmount by remember { mutableStateOf("") }

    // Synchronize text field with stored today value when first loaded
    LaunchedEffect(todayRain) {
        if (inputAmount.isEmpty() && todayRain > 0f) {
            inputAmount = todayRain.toString()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Rainfall Tracker", fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 16.dp))

        // --- 1. Log Today's Rain Input Card ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Log Today's Rainfall", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text("Current logged today: ${String.format("%.1f", todayRain)} mm", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = inputAmount,
                        onValueChange = { inputAmount = it },
                        label = { Text("Rain (mm)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = {
                            val amount = inputAmount.toFloatOrNull() ?: 0f
                            viewModel.saveTodayRain(amount)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text("Save")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- 2. Period Selector Chips (Week, Month, Year) ---
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RainPeriod.values().forEach { period ->
                FilterChip(
                    selected = selectedPeriod == period,
                    onClick = { viewModel.selectedRainPeriod.value = period },
                    label = { Text(period.name, fontSize = 14.sp) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- 3. Date Range Navigation ---
        val rangeLabel = when (selectedPeriod) {
            RainPeriod.WEEK -> {
                val start = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val end = start.plusDays(6)
                "${start.format(DateTimeFormatter.ofPattern("MMM dd"))} - ${end.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}"
            }
            RainPeriod.MONTH -> selectedDate.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
            RainPeriod.YEAR -> selectedDate.format(DateTimeFormatter.ofPattern("yyyy"))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.previousRainPeriod() }) {
                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous")
            }
            Text(rangeLabel, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = { viewModel.nextRainPeriod() }) {
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- 4. Rain Summary Stats ---
        val totalRain = graphData.sumOf { it.amountMm.toDouble() }.toFloat()
        val rainyDays = graphData.count { it.amountMm > 0f }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total Rain", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${String.format("%.1f", totalRain)} mm", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0288D1))
                }
            }
            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Wet Days/Months", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$rainyDays", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 5. Rainfall Bar Chart Graph ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Rainfall Overview (mm)", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0288D1))
                Spacer(modifier = Modifier.height(16.dp))

                if (graphData.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No rain recorded.")
                    }
                } else {
                    val maxRain = (graphData.maxOfOrNull { it.amountMm } ?: 10f).coerceAtLeast(10f)

                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            graphData.forEach { bar ->
                                val heightFraction = (bar.amountMm / maxRain).coerceIn(0f, 1f)
                                val barColor = if (bar.amountMm > 0f) Color(0xFF0288D1) else Color.LightGray.copy(alpha = 0.3f)

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(if (bar.amountMm > 0f) heightFraction.coerceAtLeast(0.05f) else 0.05f)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(barColor)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // X-Axis Labels
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            graphData.forEach { bar ->
                                Text(
                                    text = bar.label,
                                    fontSize = if (selectedPeriod == RainPeriod.MONTH) 8.sp else 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}