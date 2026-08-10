package com.example.esp32_weather.screens.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.esp32_weather.data.model.HourlyWeather
import com.example.esp32_weather.viewmodel.WeatherViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class GraphMetric(val label: String, val unit: String, val color: Color) {
    TEMPERATURE("Temp", "°C", Color(0xFF26A69A)), // Primary color for Outside Temp (Teal)
    HUMIDITY("Humidity", "%", Color(0xFF29B6F6)),
    PRESSURE("Pressure", "hPa", Color(0xFFAB47BC)),
    LUX("Light", "lx", Color(0xFFFFCA28))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: WeatherViewModel) {
    val historyList by viewModel.selectedDateHistory.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()

    var selectedMetric by remember { mutableStateOf(GraphMetric.TEMPERATURE) }

    val dateDisplay = selectedDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
    val isToday = selectedDate.isEqual(LocalDate.now())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // --- Header with Date Navigation ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.previousDay() }) {
                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous Day")
            }

            Text(
                text = "Trends: $dateDisplay",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = { viewModel.nextDay() },
                enabled = !isToday
            ) {
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = "Next Day",
                    tint = if (isToday) Color.Gray else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (historyList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No data recorded for this date.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // 1. Daily Highlights Summary Card
                item {
                    DailyHighlightsSummary(historyList)
                }

                // 2. Interactive Graph with Metric Chips
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(310.dp), // Slightly taller to fit legend
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                GraphMetric.values().forEach { metric ->
                                    FilterChip(
                                        selected = selectedMetric == metric,
                                        onClick = { selectedMetric = metric },
                                        label = { Text(metric.label, fontSize = 12.sp) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (historyList.size < 2) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Need at least 2 hours of data to render graph.")
                                }
                            } else {
                                MultiMetricGraph(history = historyList, metric = selectedMetric)
                            }
                        }
                    }
                }

                // 3. Section Title
                item {
                    Text(
                        text = "Hourly Readings Log",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // 4. Detailed History List
                items(historyList.reversed()) { hourData ->
                    HourlyRowItem(hourData)
                }
            }
        }
    }
}

// --- Daily Summary Highlights Block ---
@Composable
fun DailyHighlightsSummary(history: List<HourlyWeather>) {
    // Outside Temp
    val minTempOut = history.minOfOrNull { it.minTempOut } ?: 0f
    val maxTempOut = history.maxOfOrNull { it.maxTempOut } ?: 0f

    // Inside Temp
    val minTempIn = history.minOfOrNull { it.minTemperature } ?: 0f
    val maxTempIn = history.maxOfOrNull { it.maxTemperature } ?: 0f

    val peakLuxHour = history.maxByOrNull { it.avgLux }
    val avgTempOut = history.map { it.avgTempOut }.average().toFloat()
    val avgHum = history.map { it.avgHumidity }.average().toFloat()

    val firstPressure = history.first().avgPressure
    val lastPressure = history.last().avgPressure
    val pressureDiff = lastPressure - firstPressure
    val pressureTrend = when {
        pressureDiff > 1.0f -> "Rising ⬆ (Fair Weather)"
        pressureDiff < -1.0f -> "Falling ⬇ (Clouds / Rain)"
        else -> "Steady ➡"
    }

    val comfortLabel = when {
        avgTempOut in 18.0..25.0 && avgHum in 30.0..60.0 -> "Ideal & Comfortable 😊"
        avgTempOut > 28.0 && avgHum > 65.0 -> "Hot & Humid 💦"
        avgTempOut < 15.0 -> "Crisp & Cold ❄"
        avgHum < 30.0 -> "Dry Air 🌵"
        else -> "Moderate Weather 🌤"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Day Summary", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                // Updated to show Out as main, and In as subtitle
                SummaryTile(
                    title = "Temp Range",
                    value = "Out: ${minTempOut.toInt()}° - ${maxTempOut.toInt()}°C",
                    subtitle = "In: ${minTempIn.toInt()}° - ${maxTempIn.toInt()}°C"
                )
                SummaryTile(title = "Pressure Trend", value = pressureTrend.split(" ")[0], subtitle = pressureTrend.substringAfter(" "))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryTile(
                    title = "Peak Sun Hour",
                    value = peakLuxHour?.timeString?.take(5) ?: "--:--",
                    subtitle = "${peakLuxHour?.avgLux?.toInt() ?: 0} lx"
                )
                SummaryTile(title = "Comfort Index", value = comfortLabel.split(" ").take(2).joinToString(" "), subtitle = comfortLabel)
            }
        }
    }
}

@Composable
fun SummaryTile(title: String, value: String, subtitle: String) {
    Column {
        Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// --- Dynamic Canvas Multi-Metric Graph (Now supports Dual Lines) ---
@Composable
fun MultiMetricGraph(history: List<HourlyWeather>, metric: GraphMetric) {
    val isTemp = metric == GraphMetric.TEMPERATURE
    val colorPrimary = metric.color
    val colorSecondary = Color(0xFFFF7043) // Orange for Inside Temp

    // Primary values (TempOut, Humidity, etc.)
    val rawValuesPrimary = history.map {
        when (metric) {
            GraphMetric.TEMPERATURE -> it.avgTempOut
            GraphMetric.HUMIDITY -> it.avgHumidity
            GraphMetric.PRESSURE -> it.avgPressure
            GraphMetric.LUX -> it.avgLux
        }
    }

    // Secondary values (Only used if Temp is selected)
    val rawValuesSecondary = if (isTemp) history.map { it.avgTemperature } else emptyList()

    // Find absolute max and min to scale the graph properly
    val allValues = rawValuesPrimary + rawValuesSecondary
    val maxVal = allValues.maxOrNull() ?: 0f
    val minVal = allValues.minOrNull() ?: 0f

    val range = (maxVal - minVal).coerceAtLeast(0.1f)
    val maxGraphY = maxVal + (range * 0.1f)
    val minGraphY = minVal - (range * 0.1f)
    val totalRange = maxGraphY - minGraphY

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 36.dp, bottom = 24.dp, top = 24.dp, end = 8.dp)
        ) {
            val width = size.width
            val height = size.height
            val xStep = width / (history.size - 1)

            // --- HELPER FUNCTION TO DRAW A LINE ---
            fun drawMetricLine(values: List<Float>, lineColor: Color, drawGradient: Boolean) {
                val strokePath = Path()
                val fillPath = Path()

                values.forEachIndexed { index, valItem ->
                    val x = index * xStep
                    val y = height - ((valItem - minGraphY) / totalRange * height)

                    if (index == 0) {
                        strokePath.moveTo(x, y)
                        fillPath.moveTo(x, height)
                        fillPath.lineTo(x, y)
                    } else {
                        strokePath.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }

                    drawCircle(color = lineColor, radius = 4.dp.toPx(), center = androidx.compose.ui.geometry.Offset(x, y))
                }

                if (drawGradient) {
                    fillPath.lineTo(width, height)
                    fillPath.close()
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.2f), Color.Transparent), startY = 0f, endY = height),
                        style = Fill
                    )
                }
                drawPath(path = strokePath, color = lineColor, style = Stroke(width = 3.dp.toPx()))
            }

            // Draw Secondary (Inside) first so it sits in the background
            if (isTemp) {
                drawMetricLine(rawValuesSecondary, colorSecondary, drawGradient = false)
            }
            // Draw Primary (Outside or Hum/Pres/Lux) on top
            drawMetricLine(rawValuesPrimary, colorPrimary, drawGradient = true)
        }

        // --- LEGEND OVERLAY (Only shows for Temperature) ---
        if (isTemp) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(colorPrimary))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Out", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(colorSecondary))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("In", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Y-Axis Overlay
        Text(
            text = "${maxVal.toInt()}${metric.unit}",
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.TopStart).padding(top = 12.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "${minVal.toInt()}${metric.unit}",
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 24.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // X-Axis Overlay
        Text(
            text = history.first().timeString.take(5),
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 36.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = history.last().timeString.take(5),
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun HourlyRowItem(data: HourlyWeather) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = data.timeString, fontSize = 18.sp, fontWeight = FontWeight.Medium)

            Column(horizontalAlignment = Alignment.End) {
                // Out and In Temperatures side-by-side
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "Out: ${String.format("%.1f", data.avgTempOut)}°C",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF26A69A) // Teal matching the graph
                    )
                    Text(
                        text = " | ",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "In: ${String.format("%.1f", data.avgTemperature)}°C",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFF7043) // Orange matching the graph
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Hum: ${data.avgHumidity.toInt()}% | Pres: ${data.avgPressure.toInt()} hPa",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}