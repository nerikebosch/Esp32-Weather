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
    import java.time.Instant
    import java.time.ZoneId

    @Composable
    fun DashboardScreen(viewModel: WeatherViewModel) {
        val current by viewModel.currentWeather.collectAsState()
        val sunlightHours by viewModel.sunlightHoursToday.collectAsState()
        val todayHistory by viewModel.todayHistory.collectAsState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
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
                // 1. Hero Card with Inside & Outside Temperatures and Smart Sun Detection
                HeroCard(
                    tempIn = current!!.temperature,
                    tempOut = current!!.tempOut,
                    lux = current!!.lux,
                    timestamp = current!!.timestamp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 2. High & Low Temperatures Summary (Outside Sensor)
                val highTemp = todayHistory.maxOfOrNull { it.maxTempOut } ?: current!!.tempOut
                val lowTemp = todayHistory.minOfOrNull { it.minTempOut } ?: current!!.tempOut

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Low: ${String.format("%.1f", lowTemp)}°C",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "High: ${String.format("%.1f", highTemp)}°C",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
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

                // 4. Daylight Spectrum Ribbon Graph
                LuxTimelineGraph(todayHistory)

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // --- HELPER: Smart Weather Style (Uses Lux + Hour of Day to detect Sunrise vs Sunset) ---
    fun getWeatherStyle(lux: Float, timestamp: Long): Pair<String, List<Color>> {
        // Extract local hour of the day (0 to 23) from the timestamp
        val hour = if (timestamp > 0) {
            Instant.ofEpochSecond(timestamp).atZone(ZoneId.systemDefault()).hour
        } else 12

        return when {
            // Direct Sun / Bright Daylight
            lux > 10000f -> Pair("Bright & Clear", listOf(Color(0xFF29B6F6), Color(0xFF0288D1)))

            // Overcast / Ambient Daylight
            lux > 1000f -> Pair("Overcast / Daylight", listOf(Color(0xFF90A4AE), Color(0xFF546E7A)))

            // Low Light Transition: Check time to distinguish Sunrise vs Sunset!
            lux > 100f -> {
                if (hour in 3..11) {
                    Pair("Sunrise / Morning Light", listOf(Color(0xFF3F51B5), Color(0xFFFF9800))) // Blue to Warm Orange
                } else {
                    Pair("Sunset / Evening Light", listOf(Color(0xFF1A237E), Color(0xFFE65100))) // Deep Blue to Sunset Orange
                }
            }

            // Nighttime
            else -> Pair("Nighttime", listOf(Color(0xFF1A237E), Color(0xFF000000)))
        }
    }

    @Composable
    fun HeroCard(tempIn: Float, tempOut: Float, lux: Float, timestamp: Long) {
        val (statusText, bgColors) = getWeatherStyle(lux, timestamp)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp), // Height expanded to easily hold both readings
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(bgColors)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Weather Status Header
                    Text(
                        text = statusText,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- TEMPERATURE READINGS STACK ---
                    // Outside Temp (Primary Focus)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "OUT ",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "${String.format("%.1f", tempOut)}°C",
                            color = Color.White,
                            fontSize = 54.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Inside Temp (Secondary Focus)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "IN ",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = "${String.format("%.1f", tempIn)}°C",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Timestamp Footer
                    Text(
                        text = "Updated at ${TimeFormatter.formatTimestamp(timestamp)}",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp
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

    // --- Full-Height Daylight Spectrum Timeline ---
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
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp)),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            history.forEach { hourData ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .background(getSolidLuxColor(hourData.avgLux))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 2. Clean X-Axis Milestone Labels (Start, Middle, End)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val firstTime = history.firstOrNull()?.timeString?.take(5) ?: "00:00"
                            val midIndex = history.size / 2
                            val midTime = if (history.size > 2) history[midIndex].timeString.take(5) else ""
                            val lastTime = if (history.size > 1) history.last().timeString.take(5) else ""

                            Text(
                                text = firstTime,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (midTime.isNotEmpty()) {
                                Text(
                                    text = midTime,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (lastTime.isNotEmpty()) {
                                Text(
                                    text = lastTime,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }