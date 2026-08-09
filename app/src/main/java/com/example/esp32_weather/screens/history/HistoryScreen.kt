package com.example.esp32_weather.screens.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    TEMPERATURE("Temp", "°C", Color(0xFFFF7043)),
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
                            .height(290.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Filter Chips Row
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
    val minTemp = history.minOfOrNull { it.minTemperature } ?: 0f
    val maxTemp = history.maxOfOrNull { it.maxTemperature } ?: 0f
    val tempDelta = maxTemp - minTemp

    val peakLuxHour = history.maxByOrNull { it.avgLux }
    val avgTemp = history.map { it.avgTemperature }.average().toFloat()
    val avgHum = history.map { it.avgHumidity }.average().toFloat()

    // Pressure Trend (Compare first recorded hour vs last recorded hour)
    val firstPressure = history.first().avgPressure
    val lastPressure = history.last().avgPressure
    val pressureDiff = lastPressure - firstPressure
    val pressureTrend = when {
        pressureDiff > 1.0f -> "Rising ⬆ (Fair Weather)"
        pressureDiff < -1.0f -> "Falling ⬇ (Clouds / Rain)"
        else -> "Steady ➡"
    }

    // Comfort Score Logic
    val comfortLabel = when {
        avgTemp in 18.0..25.0 && avgHum in 30.0..60.0 -> "Ideal & Comfortable 😊"
        avgTemp > 28.0 && avgHum > 65.0 -> "Hot & Humid 💦"
        avgTemp < 15.0 -> "Crisp & Cold ❄"
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
                SummaryTile(title = "Temp Range", value = "${minTemp.toInt()}° - ${maxTemp.toInt()}°C", subtitle = "Swing: ${String.format("%.1f", tempDelta)}°C")
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

// --- Dynamic Canvas Multi-Metric Graph ---
@Composable
fun MultiMetricGraph(history: List<HourlyWeather>, metric: GraphMetric) {
    val rawValues = history.map {
        when (metric) {
            GraphMetric.TEMPERATURE -> it.avgTemperature
            GraphMetric.HUMIDITY -> it.avgHumidity
            GraphMetric.PRESSURE -> it.avgPressure
            GraphMetric.LUX -> it.avgLux
        }
    }

    val maxVal = rawValues.maxOrNull() ?: 0f
    val minVal = rawValues.minOrNull() ?: 0f

    val range = (maxVal - minVal).coerceAtLeast(0.1f)
    val maxGraphY = maxVal + (range * 0.1f)
    val minGraphY = minVal - (range * 0.1f)
    val totalRange = maxGraphY - minGraphY

    val lineColor = metric.color
    val gradientColor = metric.color.copy(alpha = 0.25f)

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 36.dp, bottom = 24.dp, top = 8.dp, end = 8.dp)
        ) {
            val width = size.width
            val height = size.height
            val xStep = width / (rawValues.size - 1)
            val strokePath = Path()
            val fillPath = Path()

            rawValues.forEachIndexed { index, valItem ->
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

            fillPath.lineTo(width, height)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(listOf(gradientColor, Color.Transparent), startY = 0f, endY = height),
                style = Fill
            )
            drawPath(path = strokePath, color = lineColor, style = Stroke(width = 3.dp.toPx()))
        }

        // Y-Axis Overlay
        Text(
            text = "${maxVal.toInt()}${metric.unit}",
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.TopStart),
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
                Text(text = "${String.format("%.1f", data.avgTemperature)}°C", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "Hum: ${data.avgHumidity.toInt()}% | Pres: ${data.avgPressure.toInt()} hPa",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}