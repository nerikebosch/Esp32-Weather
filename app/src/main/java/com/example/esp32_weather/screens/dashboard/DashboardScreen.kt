package com.example.esp32_weather.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.esp32_weather.utils.TimeFormatter
import com.example.esp32_weather.viewmodel.WeatherViewModel

@Composable
fun DashboardScreen(viewModel: WeatherViewModel) {
    val current by viewModel.currentWeather.collectAsState()
    val sunlightHours by viewModel.sunlightHoursToday.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (current == null) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 100.dp))
            Text("Waiting for ESP32 data...", modifier = Modifier.padding(top = 16.dp))
        } else {
            // Hero Card (Live Temperature)
            HeroCard(temperature = current!!.temperature, lux = current!!.lux)

            Spacer(modifier = Modifier.height(24.dp))

            // 2x2 Metrics Grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MetricCard(title = "Humidity", value = "${current!!.humidity}%", modifier = Modifier.weight(1f))
                MetricCard(title = "Pressure", value = "${current!!.pressure} hPa", modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MetricCard(title = "Light Level", value = "${current!!.lux} lx", modifier = Modifier.weight(1f))
                MetricCard(title = "Sun Today", value = "$sunlightHours hrs", modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun HeroCard(temperature: Float, lux: Float) {
    // Dynamic background: Dark blue for low light (night), bright orange/yellow for high light (day)
    val bgColors = if (lux > 500f) {
        listOf(Color(0xFFFF9800), Color(0xFFFFC107)) // Daytime
    } else {
        listOf(Color(0xFF1A237E), Color(0xFF3949AB)) // Nighttime
    }

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
                Text(
                    text = "Live Weather",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 18.sp
                )
                Text(
                    text = "${String.format("%.1f", temperature)}°C",
                    color = Color.White,
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Updated just ${TimeFormatter.formatTimestamp(current!!.timestamp)}",
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