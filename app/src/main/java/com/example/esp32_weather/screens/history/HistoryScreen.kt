package com.example.esp32_weather.screens.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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

@Composable
fun HistoryScreen(viewModel: WeatherViewModel) {
    val historyList by viewModel.todayHistory.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Today's Trends",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp, top = 24.dp)
        )

        if (historyList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Waiting for ESP32 hourly data...", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {

            // 1. The Graph Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (historyList.size < 2) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Need at least 2 hours of data to draw a graph.")
                    }
                } else {
                    TemperatureGraph(historyList)
                }
            }

            Text(
                text = "Detailed Log",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // 2. The List Section
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // Reverse the list so the newest hour is at the top
                items(historyList.reversed()) { hourData ->
                    HourlyRowItem(hourData)
                }
            }
        }
    }
}

@Composable
fun TemperatureGraph(history: List<HourlyWeather>) {
    // Extract temperatures and find the highest and lowest points to scale the graph
    val temperatures = history.map { it.avgTemperature }
    val maxTemp = temperatures.maxOrNull() ?: 0f
    val minTemp = temperatures.minOrNull() ?: 0f

    // Add a little padding to the top and bottom of the graph
    val range = (maxTemp - minTemp).coerceAtLeast(1f)
    val maxGraphY = maxTemp + (range * 0.1f)
    val minGraphY = minTemp - (range * 0.1f)
    val totalRange = maxGraphY - minGraphY

    val lineColor = MaterialTheme.colorScheme.primary
    val gradientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        val width = size.width
        val height = size.height

        // Distance between each point on the X axis
        val xStep = width / (temperatures.size - 1)

        val strokePath = Path()
        val fillPath = Path()

        temperatures.forEachIndexed { index, temp ->
            val x = index * xStep
            // Calculate Y position (invert it because Y goes down in Canvas)
            val y = height - ((temp - minGraphY) / totalRange * height)

            if (index == 0) {
                strokePath.moveTo(x, y)
                fillPath.moveTo(x, height) // Start fill from the bottom
                fillPath.lineTo(x, y)
            } else {
                strokePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }

            // Draw a dot for each data point
            drawCircle(
                color = lineColor,
                radius = 6.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(x, y)
            )
        }

        // Finish the fill path by drawing down to the bottom right, then closing it
        fillPath.lineTo(width, height)
        fillPath.close()

        // Draw the soft gradient under the line
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(gradientColor, Color.Transparent),
                startY = 0f,
                endY = height
            ),
            style = Fill
        )

        // Draw the actual temperature line
        drawPath(
            path = strokePath,
            color = lineColor,
            style = Stroke(width = 3.dp.toPx())
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
            Text(
                text = data.timeString,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${String.format("%.1f", data.avgTemperature)}°C",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Hum: ${String.format("%.0f", data.avgHumidity)}%",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}