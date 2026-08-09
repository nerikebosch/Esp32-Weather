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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

@Composable
fun HistoryScreen(viewModel: WeatherViewModel) {
    val historyList by viewModel.selectedDateHistory.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()

    // Format the date (e.g., "Aug 09, 2026")
    val dateDisplay = selectedDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
    val isToday = selectedDate.isEqual(LocalDate.now())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // --- Header with Date Navigation Arrows ---
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

            // Disable the right arrow if we are already on "Today"
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

            // --- The Graph Section with Axes ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp) // Made slightly taller for labels
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Temperature (°C)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (historyList.size < 2) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Need at least 2 hours of data.")
                        }
                    } else {
                        TemperatureGraphWithAxes(historyList)
                    }
                }
            }

            Text(
                text = "Detailed Log",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // --- The List Section ---
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(historyList.reversed()) { hourData ->
                    HourlyRowItem(hourData)
                }
            }
        }
    }
}

@Composable
fun TemperatureGraphWithAxes(history: List<HourlyWeather>) {
    val temperatures = history.map { it.avgTemperature }
    val maxTemp = temperatures.maxOrNull() ?: 0f
    val minTemp = temperatures.minOrNull() ?: 0f

    val range = (maxTemp - minTemp).coerceAtLeast(1f)
    val maxGraphY = maxTemp + (range * 0.1f)
    val minGraphY = minTemp - (range * 0.1f)
    val totalRange = maxGraphY - minGraphY

    val lineColor = MaterialTheme.colorScheme.primary
    val gradientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)

    // A Box lets us draw the Canvas in the background, and place Text labels on top
    Box(modifier = Modifier.fillMaxSize()) {

        // 1. The Canvas Drawing (Padded to make room for text)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 32.dp, bottom = 24.dp, top = 8.dp, end = 8.dp)
        ) {
            val width = size.width
            val height = size.height
            val xStep = width / (temperatures.size - 1)
            val strokePath = Path()
            val fillPath = Path()

            temperatures.forEachIndexed { index, temp ->
                val x = index * xStep
                val y = height - ((temp - minGraphY) / totalRange * height)

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

        // 2. Y-Axis Overlays (Temperatures)
        Text(
            text = "${maxTemp.toInt()}°",
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.TopStart).padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "${minTemp.toInt()}°",
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 24.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // 3. X-Axis Overlays (Times)
        // Earliest time recorded today
        Text(
            text = history.first().timeString.take(5), // e.g., "08:00"
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 32.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        // Latest time recorded today
        Text(
            text = history.last().timeString.take(5),
            fontSize = 12.sp,
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
                Text(text = "${String.format("%.1f", data.avgTemperature)}°C", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(text = "Hum: ${String.format("%.0f", data.avgHumidity)}%", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}