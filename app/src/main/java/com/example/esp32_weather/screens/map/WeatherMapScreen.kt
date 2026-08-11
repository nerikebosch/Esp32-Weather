package com.example.esp32_weather.screens.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.UrlTileProvider
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import java.net.URL

// --- PASTE YOUR OPENWEATHERMAP API KEY HERE ---
private const val OPENWEATHER_API_KEY = ""

@Composable
fun WeatherMapScreen() {
    val southAfrica = LatLng(-28.4793, 24.6727)
    val poland = LatLng(51.9194, 19.1451)

    var currentLayer by remember { mutableStateOf("clouds_new") }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(southAfrica, 5f)
    }

    val coroutineScope = rememberCoroutineScope()

    val weatherTileProvider = remember(currentLayer) {
        object : UrlTileProvider(256, 256) {
            override fun getTileUrl(x: Int, y: Int, zoom: Int): URL? {
                return try {
                    URL("https://tile.openweathermap.org/map/$currentLayer/$zoom/$x/$y.png?appid=$OPENWEATHER_API_KEY")
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // --- HEADER CONTROLS ---
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Live Weather Radar",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = {
                        coroutineScope.launch {
                            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(southAfrica, 5f))
                        }
                    }) {
                        Text("South Africa")
                    }

                    Button(onClick = {
                        coroutineScope.launch {
                            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(poland, 5.5f))
                        }
                    }) {
                        Text("Poland")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = currentLayer == "clouds_new",
                        onClick = { currentLayer = "clouds_new" },
                        label = { Text("Clouds") }
                    )
                    FilterChip(
                        selected = currentLayer == "precipitation_new",
                        onClick = { currentLayer = "precipitation_new" },
                        label = { Text("Rain") }
                    )
                    FilterChip(
                        selected = currentLayer == "temp_new",
                        onClick = { currentLayer = "temp_new" },
                        label = { Text("Temp") }
                    )
                }
            }
        }

        // --- MAP AND LEGEND OVERLAY ---
        Box(modifier = Modifier.fillMaxSize()) {

            // 1. Google Map (Hybrid Satellite View)
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(mapType = MapType.HYBRID),
                uiSettings = MapUiSettings(zoomControlsEnabled = false)
            ) {
                // Main Weather Overlay
                TileOverlay(
                    tileProvider = weatherTileProvider,
                    transparency = 0f
                )

                // THE FIX FOR DARKER RAIN:
                // Stacking a 2nd and 3rd layer specifically for Rain multiplies the color density,
                // making transparent blue/purple rain clouds significantly darker and richer!
                if (currentLayer == "precipitation_new") {
                    TileOverlay(
                        tileProvider = weatherTileProvider,
                        transparency = 0f
                    )
                    TileOverlay(
                        tileProvider = weatherTileProvider,
                        transparency = 0f
                    )
                }
            }

            // 2. Dynamic Legend at the bottom
            DynamicWeatherLegend(
                currentLayer = currentLayer,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            )
        }
    }
}

// --- DYNAMIC FLOATING LEGEND COMPOSABLE ---
@Composable
fun DynamicWeatherLegend(currentLayer: String, modifier: Modifier = Modifier) {

    val (legendTitle, colors, startLabel, midLabel, endLabel) = when (currentLayer) {
        "temp_new" -> {
            listOf(
                "Temperature (°C)",
                listOf(Color(0xFF8257DB), Color(0xFF20C4E8), Color(0xFFFFF028), Color(0xFFFC8014)),
                "-30°C", "5°C", "40°C"
            )
        }
        "precipitation_new" -> {
            listOf(
                "Rainfall Intensity (mm/h)",
                // Darkened legend gradient to match the stacked radar overlay
                listOf(Color(0xFF0077FF), Color(0xFF5A00D6), Color(0xFFD6005A)),
                "Light", "Moderate", "Heavy"
            )
        }
        else -> {
            listOf(
                "Cloud Cover (%)",
                listOf(Color(0x44FFFFFF), Color(0xAAFFFFFF), Color(0xFF9E9E9E)),
                "Clear (0%)", "Partly (50%)", "Overcast (100%)"
            )
        }
    }

    val title = legendTitle as String
    val gradientColors = colors as List<Color>
    val textStart = startLabel as String
    val textMid = midLabel as String
    val textEnd = endLabel as String

    Card(
        modifier = modifier
            .fillMaxWidth(0.9f)
            .height(80.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Brush.horizontalGradient(gradientColors))
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = textStart, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                Text(text = textMid, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                Text(text = textEnd, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}