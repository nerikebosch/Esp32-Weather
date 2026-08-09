package com.example.esp32_weather.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.esp32_weather.data.model.HourlyWeather
import com.example.esp32_weather.utils.TimeFormatter
import com.example.esp32_weather.viewmodel.WeatherViewModel

@Composable
fun DashboardScreen(viewModel: WeatherViewModel) {
    val current by viewModel.currentWeather.collectAsState()
    val sunlightHours by viewModel.sunlightHoursToday.collectAsState()

    // We bring in today's history so we can calculate highs/lows and draw the timeline!
    val todayHistory by viewModel.todayHistory.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()) // Makes the screen scrollable!
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (current == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(modifier = Modifier.padding(top = 100.dp))
                    Text("Waiting for ESP32 data...", modifier = Modifier.padding(top = 16.dp))
                }
            }
        } else {
            // 1. Hero Card (Now with dynamic colors & weather status)
            HeroCard(
                temperature = current!!.temperature,
                lux = current!!.lux,
                timestamp = current!!.timestamp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. High & Low Temperatures Summary
            val highTemp = todayHistory.maxOfOrNull { it.maxTemperature } ?: current!!.temperature
            val lowTemp = todayHistory.minOfOrNull { it.minTemperature } ?: current!!.temperature

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Low: ${String.format("%.1f", lowTemp)}°C", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                Text("High: ${String.format("%.1f", highTemp)}°C", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. 2x2 Metrics Grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MetricCard(title = "Humidity", value = "${current!!.humidity}%", modifier = Modifier.weight(1f))
                MetricCard(title = "Pressure", value = "${current!!.pressure} hPa", modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MetricCard(title = "Light Level", value = "${current!!.lux} lx", modifier = Modifier.weight(1f))
                MetricCard(title = "Sun Today", value = "$sunlightHours hrs", modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. The New Daylight Spectrum Graph
            LuxTimelineGraph(todayHistory)

            Spacer(modifier = Modifier.height(32.dp)) // Extra padding at the bottom for the nav bar
        }
    }
}

// --- NEW HELPER: Maps your exact Lux ranges to colors and text! ---
fun getWeatherStyle(lux: Float): Pair<String, List<Color>> {
    return when {
        // Direct Sun / Bright Daylight
        lux > 10000f -> Pair("Bright & Clear", listOf(Color(0xFF29B6F6), Color(0xFF0288D1))) // Bright Sky Blue

        // Overcast / Ambient Daylight
        lux > 1000f -> Pair("Overcast / Daylight", listOf(Color(0xFF90A4AE), Color(0xFF546E7A))) // Grey-Blue

        // Sunrise / Sunset / Heavy Clouds
        lux > 100f -> Pair("Sunrise / Low Light", listOf(Color(0xFF3F51B5), Color(0xFFFF9800))) // Blue fading to Orange!

        // Nighttime
        else -> Pair("Nighttime", listOf(Color(0xFF1A237E), Color(0xFF000000))) // Deep Blue to Black
    }
}

@Composable
fun HeroCard(temperature: Float, lux: Float, timestamp: Long) {
    // We call our new helper function to get the correct text and colors
    val (statusText, bgColors) = getWeatherStyle(lux)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(bgColors)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // The status text changes dynamically based on the sun!
                Text(
                    text = statusText,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${String.format("%.1f", temperature)}°C",
                    color = Color.White,
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Updated at ${TimeFormatter.formatTimestamp(timestamp)}",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun MetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

// --- Solid Color Mapping for the Lux Ribbon ---
fun getSolidLuxColor(lux: Float): Color {
    return when {
        lux > 10000f -> Color(0xFFFFB703) // Bright Golden Yellow (Direct Sunlight)
        lux > 2000f  -> Color(0xFF4EA8DE) // Clear Sky Blue (Full Daylight)
        lux > 500f   -> Color(0xFF90A4AE) // Muted Grey-Blue (Overcast / Cloudy)
        lux > 100f   -> Color(0xFFE07A5F) // Warm Sunset / Sunrise Orange
        else         -> Color(0xFF1B263B) // Deep Midnight Blue (Night)
    }
}

// --- The Full-Height Daylight Spectrum Timeline ---
@Composable
fun LuxTimelineGraph(history: List<HourlyWeather>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Hourly Daylight Spectrum",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Color intensity ribbon across recorded hours",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (history.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hourly records stored yet today.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {

                    // 1. Full-Height Color Spectrum Band
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f) // Fills entire remaining vertical space
                            .clip(RoundedCornerShape(8.dp)),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        history.forEach { hourData ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight() // 100% Full Height!
                                    .background(getSolidLuxColor(hourData.avgLux))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 2. X-Axis: Time Labels Underneath Each Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        history.forEach { hourData ->
                            // Formats "14:00:00" down to "14:00"
                            val timeLabel = if (hourData.timeString.length >= 5) {
                                hourData.timeString.substring(0, 5)
                            } else {
                                hourData.timeString
                            }

                            Text(
                                text = timeLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
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
}