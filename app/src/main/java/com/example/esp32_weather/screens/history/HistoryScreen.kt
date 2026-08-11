package com.example.esp32_weather.screens.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.esp32_weather.data.model.DailyWeatherSummary
import com.example.esp32_weather.data.model.HourlyWeather
import com.example.esp32_weather.viewmodel.WeatherViewModel

// --- ENUMS & DATA CLASSES ---
enum class GraphMetric(val label: String, val unit: String, val color: Color) {
    TEMPERATURE("Temp", "°C", Color(0xFF26A69A)),
    HUMIDITY("Humidity", "%", Color(0xFF29B6F6)),
    PRESSURE("Pressure", "hPa", Color(0xFFAB47BC)),
    LUX("Light", "lx", Color(0xFFFFCA28))
}

enum class HistoryPeriod(val label: String) {
    HOURS_24("24 Hours"),
    HOURS_48("48 Hours"),
    DAYS_7("7 Days")
}

// NOTE: You will move this to your data/model folder later when hooking up Firebase!
data class DailyWeatherSummary(
    val dayOfWeek: String,
    val dateString: String,
    val minTempOut: Float,
    val maxTempOut: Float,
    val avgHumidity: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: WeatherViewModel) {
    // Current Hourly state from your ViewModel
    val hourlyHistory by viewModel.selectedDateHistory.collectAsState()

    // UI States
    var selectedPeriod by remember { mutableStateOf(HistoryPeriod.HOURS_24) }
    var selectedMetric by remember { mutableStateOf(GraphMetric.TEMPERATURE) }

    val sevenDaySummaries by viewModel.sevenDaySummaries.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // --- 1. Header & Toggle Controls ---
        Text(
            text = "Historical Trends",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HistoryPeriod.values().forEach { period ->
                FilterChip(
                    selected = selectedPeriod == period,
                    onClick = {
                        selectedPeriod = period
                        // TODO: Tell ViewModel to fetch 24h, 48h, or 7-day data here!
                    },
                    label = { Text(period.label, fontSize = 13.sp) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 2. Dynamic Content Based on Selected Toggle ---
        if (selectedPeriod == HistoryPeriod.DAYS_7) {
            // SHOW 7-DAY OVERVIEW
            Text(
                text = "Last 7 Days Overview",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(sevenDaySummaries) { dailyData ->
                    DailySummaryRowItem(dailyData)
                }
            }

        } else {
            // SHOW 24/48 HOUR HOURLY VIEW
            if (hourlyHistory.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hourly data recorded for this period.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        DailyHighlightsSummary(hourlyHistory)
                    }

                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(310.dp),
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

                                if (hourlyHistory.size < 2) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("Need at least 2 hours of data to render graph.")
                                    }
                                } else {
                                    MultiMetricGraph(history = hourlyHistory, metric = selectedMetric)
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Hourly Readings Log",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(hourlyHistory.reversed()) { hourData ->
                        // Tell the row to show the Day if 48 Hours is selected
                        HourlyRowItem(
                            data = hourData,
                            is48Hours = selectedPeriod == HistoryPeriod.HOURS_48
                        )
                    }
                }
            }
        }
    }
}

// --- NEW: 7-Day UI Component ---
@Composable
fun DailySummaryRowItem(data: DailyWeatherSummary) {
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
            // Day & Date
            Column(modifier = Modifier.width(60.dp)) {
                Text(text = data.dayOfWeek, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(text = data.dateString, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Temperature Bar (Visual flair)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "${data.minTempOut.toInt()}°",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF42A5F5) // Cold Blue
                )

                // Gradient Range Bar
                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFF42A5F5), Color(0xFFEF5350))))
                )

                Text(
                    text = "${data.maxTempOut.toInt()}°",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEF5350) // Hot Red
                )
            }

            // Humidity / Extra stats
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(50.dp)) {
                Text(text = "Hum", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "${data.avgHumidity.toInt()}%", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF29B6F6))
            }
        }
    }
}

// --- EXISTING COMPONENTS (Unchanged) ---

@Composable
fun DailyHighlightsSummary(history: List<HourlyWeather>) {
    val minTempOut = history.minOfOrNull { it.minTempOut } ?: 0f
    val maxTempOut = history.maxOfOrNull { it.maxTempOut } ?: 0f
    val minTempIn = history.minOfOrNull { it.minTemperature } ?: 0f
    val maxTempIn = history.maxOfOrNull { it.maxTemperature } ?: 0f
    val peakLuxHour = history.maxByOrNull { it.avgLux }
    val avgTempOut = history.map { it.avgTempOut }.average().toFloat()
    val avgHum = history.map { it.avgHumidity }.average().toFloat()

    val firstPressure = history.firstOrNull()?.avgPressure ?: 0f
    val lastPressure = history.lastOrNull()?.avgPressure ?: 0f
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
            Text("Summary", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
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

@Composable
fun MultiMetricGraph(history: List<HourlyWeather>, metric: GraphMetric) {
    val isTemp = metric == GraphMetric.TEMPERATURE
    val colorPrimary = metric.color
    val colorSecondary = Color(0xFFFF7043)

    val rawValuesPrimary = history.map {
        when (metric) {
            GraphMetric.TEMPERATURE -> it.avgTempOut
            GraphMetric.HUMIDITY -> it.avgHumidity
            GraphMetric.PRESSURE -> it.avgPressure
            GraphMetric.LUX -> it.avgLux
        }
    }

    val rawValuesSecondary = if (isTemp) history.map { it.avgTemperature } else emptyList()

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
            val xStep = width / (history.size - 1).coerceAtLeast(1)

            fun drawMetricLine(values: List<Float>, lineColor: Color, drawGradient: Boolean) {
                if (values.isEmpty()) return
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

            if (isTemp) {
                drawMetricLine(rawValuesSecondary, colorSecondary, drawGradient = false)
            }
            drawMetricLine(rawValuesPrimary, colorPrimary, drawGradient = true)
        }

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
        val firstLabel = history.firstOrNull()?.let {
            if (it.timestamp > 0) java.time.Instant.ofEpochSecond(it.timestamp).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("EEE HH:mm")) else it.timeString.take(5)
        } ?: ""

        val lastLabel = history.lastOrNull()?.let {
            if (it.timestamp > 0) java.time.Instant.ofEpochSecond(it.timestamp).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("EEE HH:mm")) else it.timeString.take(5)
        } ?: ""

        Text(
            text = firstLabel,
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 36.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = lastLabel,
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun HourlyRowItem(data: HourlyWeather, is48Hours: Boolean = false) {
    // Dynamically build the time label
    val timeLabel = if (is48Hours && data.timestamp > 0) {
        java.time.Instant.ofEpochSecond(data.timestamp)
            .atZone(java.time.ZoneId.systemDefault())
            .format(java.time.format.DateTimeFormatter.ofPattern("EEE HH:mm"))
    } else {
        data.timeString.take(5)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Use the new dynamic timeLabel here
            Text(text = timeLabel, fontSize = 18.sp, fontWeight = FontWeight.Medium)

            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "Out: ${String.format("%.1f", data.avgTempOut)}°C",
                        fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF26A69A)
                    )
                    Text(text = " | ", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "In: ${String.format("%.1f", data.avgTemperature)}°C",
                        fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFFF7043)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Hum: ${data.avgHumidity.toInt()}% | Pres: ${data.avgPressure.toInt()} hPa",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}