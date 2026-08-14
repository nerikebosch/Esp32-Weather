package com.example.esp32_weather.screens.history

import  androidx.compose.foundation.Canvas
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
import java.time.LocalDate
import java.time.ZoneId

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: WeatherViewModel) {
    val hourlyHistory by viewModel.selectedDateHistory.collectAsState()
    val todayData by viewModel.todayHistory.collectAsState()
    val yesterdayData by viewModel.yesterdayHistory.collectAsState()
    val sevenDaySummaries by viewModel.sevenDaySummaries.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()

    var selectedMetric by remember { mutableStateOf(GraphMetric.TEMPERATURE) }

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
                    onClick = { viewModel.selectedPeriod.value = period }, // Updates ViewModel directly!
                    label = { Text(period.label, fontSize = 13.sp) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 2. Dynamic Content Based on Selected Toggle ---
        when (selectedPeriod) {

            HistoryPeriod.DAYS_7 -> {
                Text("Last 7 Days Overview", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 12.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(sevenDaySummaries) { dailyData ->
                        DailySummaryRowItem(dailyData)
                    }
                }
            }

            HistoryPeriod.HOURS_48 -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                    item {
                        ComparativeSummaryCard(yesterday = yesterdayData, today = todayData)
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().height(340.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    GraphMetric.values().forEach { metric ->
                                        FilterChip(
                                            selected = selectedMetric == metric,
                                            onClick = { selectedMetric = metric },
                                            label = { Text(metric.label, fontSize = 12.sp) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                if (todayData.isEmpty() && yesterdayData.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("No data recorded for yesterday or today.")
                                    }
                                } else {
                                    ComparativeMetricGraph(yesterday = yesterdayData, today = todayData, metric = selectedMetric)
                                }
                            }
                        }
                    }
                }
            }

            HistoryPeriod.HOURS_24 -> {
                if (todayData.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No hourly data recorded for today yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                        item { DailyHighlightsSummary(todayData) }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().height(310.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        GraphMetric.values().forEach { metric ->
                                            FilterChip(
                                                selected = selectedMetric == metric,
                                                onClick = { selectedMetric = metric },
                                                label = { Text(metric.label, fontSize = 12.sp) }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    if (todayData.size < 2) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("Need at least 2 hours of data to render graph.")
                                        }
                                    } else {
                                        MultiMetricGraph(history = todayData, metric = selectedMetric)
                                    }
                                }
                            }
                        }

                        item {
                            Text("Hourly Readings Log", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                        }

                        items(todayData.reversed()) { hourData ->
                            HourlyRowItem(data = hourData)
                        }
                    }
                }
            }
        }
    }
}

// ====================================================================
// NEW 48-HOUR COMPARATIVE COMPONENTS
// ====================================================================

@Composable
fun ComparativeSummaryCard(yesterday: List<HourlyWeather>, today: List<HourlyWeather>) {
    val yMax = yesterday.maxOfOrNull { it.maxTempOut } ?: 0f
    val tMax = today.maxOfOrNull { it.maxTempOut } ?: 0f
    val diff = tMax - yMax

    val trendText = when {
        today.isEmpty() || yesterday.isEmpty() -> "Gathering data..."
        diff > 1.0f -> "Today is ${String.format("%.1f", diff)}°C warmer than yesterday. 📈"
        diff < -1.0f -> "Today is ${String.format("%.1f", kotlin.math.abs(diff))}°C cooler than yesterday. 📉"
        else -> "Temperatures are similar to yesterday. ➡"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Yesterday vs. Today", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(trendText, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryTile("Yesterday High", "${String.format("%.1f", yMax)}°C", "")
                SummaryTile("Today High", if (today.isNotEmpty()) "${String.format("%.1f", tMax)}°C" else "--", "")
            }
        }
    }
}

@Composable
fun ComparativeMetricGraph(yesterday: List<HourlyWeather>, today: List<HourlyWeather>, metric: GraphMetric) {
    val colorToday = metric.color
    val colorYesterday = metric.color.copy(alpha = 0.3f) // Faded version for yesterday

    fun extractValue(item: HourlyWeather): Float = when (metric) {
        GraphMetric.TEMPERATURE -> item.avgTempOut
        GraphMetric.HUMIDITY -> item.avgHumidity
        GraphMetric.PRESSURE -> item.avgPressure
        GraphMetric.LUX -> item.avgLux
    }

    val allValues = yesterday.map { extractValue(it) } + today.map { extractValue(it) }
    val maxVal = allValues.maxOrNull() ?: 0f
    val minVal = allValues.minOrNull() ?: 0f

    val range = (maxVal - minVal).coerceAtLeast(0.1f)
    val maxGraphY = maxVal + (range * 0.1f)
    val minGraphY = minVal - (range * 0.1f)
    val totalRange = maxGraphY - minGraphY

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize().padding(start = 36.dp, bottom = 24.dp, top = 24.dp, end = 8.dp)) {
            val width = size.width
            val height = size.height

            // Plotting function that aligns values to their specific hour on a 0-23 timeline
            fun plotLine(data: List<HourlyWeather>, color: Color, strokeWidth: Float, fillAlpha: Float) {
                if (data.isEmpty()) return

                val path = Path()
                val fillPath = Path()
                var isFirst = true

                // Sort purely by the hour of the day (0 to 23)
                val sortedData = data.sortedBy { it.timeString.take(2).toIntOrNull() ?: 0 }

                sortedData.forEach { hourData ->
                    val hour = hourData.timeString.take(2).toIntOrNull() ?: return@forEach
                    val value = extractValue(hourData)

                    val x = (hour / 23f) * width
                    val y = height - ((value - minGraphY) / totalRange * height)

                    if (isFirst) {
                        path.moveTo(x, y)
                        fillPath.moveTo(x, height)
                        fillPath.lineTo(x, y)
                        isFirst = false
                    } else {
                        path.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }
                    drawCircle(color = color, radius = 4.dp.toPx(), center = androidx.compose.ui.geometry.Offset(x, y))
                }

                if (!isFirst && fillAlpha > 0f) {
                    fillPath.lineTo(sortedData.last().timeString.take(2).toIntOrNull()?.div(23f)?.times(width) ?: width, height)
                    fillPath.close()
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(listOf(color.copy(alpha = fillAlpha), Color.Transparent), startY = 0f, endY = height),
                        style = Fill
                    )
                }
                drawPath(path = path, color = color, style = Stroke(width = strokeWidth))
            }

            // Draw Yesterday first (faded in background), then Today (bold in front)
            plotLine(yesterday, colorYesterday, strokeWidth = 2.dp.toPx(), fillAlpha = 0f)
            plotLine(today, colorToday, strokeWidth = 4.dp.toPx(), fillAlpha = 0.2f)
        }

        // Legend Overlay
        Row(
            modifier = Modifier.align(Alignment.TopEnd).padding(end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(colorYesterday))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Yesterday", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(colorToday))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Today", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Y-Axis Overlay
        Text("${maxVal.toInt()}${metric.unit}", fontSize = 11.sp, modifier = Modifier.align(Alignment.TopStart).padding(top = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("${minVal.toInt()}${metric.unit}", fontSize = 11.sp, modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)

        // X-Axis Overlay (Fixed Timeline from Midnight to Midnight)
        Text("00:00", fontSize = 11.sp, modifier = Modifier.align(Alignment.BottomStart).padding(start = 36.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("12:00", fontSize = 11.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(start = 18.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("23:00", fontSize = 11.sp, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}


// ====================================================================
// EXISTING 24-HOUR / 7-DAY COMPONENTS
// ====================================================================

@Composable
fun DailySummaryRowItem(data: DailyWeatherSummary) {
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
            Column(modifier = Modifier.width(60.dp)) {
                Text(text = data.dayOfWeek, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(text = data.dateString, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
            ) {
                Text("${data.minTempOut.toInt()}°", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF42A5F5))
                Box(modifier = Modifier.padding(horizontal = 8.dp).weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(Brush.horizontalGradient(listOf(Color(0xFF42A5F5), Color(0xFFEF5350)))))
                Text("${data.maxTempOut.toInt()}°", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF5350))
            }
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(50.dp)) {
                Text("Hum", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${data.avgHumidity.toInt()}%", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF29B6F6))
            }
        }
    }
}

@Composable
fun DailyHighlightsSummary(history: List<HourlyWeather>) {
    val minTempOut = history.minOfOrNull { it.minTempOut } ?: 0f
    val maxTempOut = history.maxOfOrNull { it.maxTempOut } ?: 0f
    val peakLuxHour = history.maxByOrNull { it.avgLux }
    val avgTempOut = history.map { it.avgTempOut }.average().toFloat()
    val avgHum = history.map { it.avgHumidity }.average().toFloat()

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
            Text("Today's Summary", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryTile("Temp Range", "${minTempOut.toInt()}° - ${maxTempOut.toInt()}°C", "")
                SummaryTile("Comfort Index", comfortLabel.split(" ").take(2).joinToString(" "), comfortLabel)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryTile("Peak Sun Hour", peakLuxHour?.timeString?.take(5) ?: "--:--", "${peakLuxHour?.avgLux?.toInt() ?: 0} lx")
                SummaryTile("Avg Humidity", "${avgHum.toInt()}%", "")
            }
        }
    }
}

@Composable
fun SummaryTile(title: String, value: String, subtitle: String) {
    Column {
        Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        if (subtitle.isNotEmpty()) {
            Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
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
        Canvas(modifier = Modifier.fillMaxSize().padding(start = 36.dp, bottom = 24.dp, top = 24.dp, end = 8.dp)) {
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
                    drawPath(path = fillPath, brush = Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.2f), Color.Transparent), startY = 0f, endY = height), style = Fill)
                }
                drawPath(path = strokePath, color = lineColor, style = Stroke(width = 3.dp.toPx()))
            }

            if (isTemp) drawMetricLine(rawValuesSecondary, colorSecondary, drawGradient = false)
            drawMetricLine(rawValuesPrimary, colorPrimary, drawGradient = true)
        }

        if (isTemp) {
            Row(modifier = Modifier.align(Alignment.TopEnd).padding(end = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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

        Text("${maxVal.toInt()}${metric.unit}", fontSize = 11.sp, modifier = Modifier.align(Alignment.TopStart).padding(top = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("${minVal.toInt()}${metric.unit}", fontSize = 11.sp, modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(history.firstOrNull()?.timeString?.take(5) ?: "", fontSize = 11.sp, modifier = Modifier.align(Alignment.BottomStart).padding(start = 36.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(history.lastOrNull()?.timeString?.take(5) ?: "", fontSize = 11.sp, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = data.timeString.take(5), fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("Out: ${String.format("%.1f", data.avgTempOut)}°C", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF26A69A))
                    Text(" | ", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("In: ${String.format("%.1f", data.avgTemperature)}°C", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFFF7043))
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text("Hum: ${data.avgHumidity.toInt()}% | Pres: ${data.avgPressure.toInt()} hPa", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}