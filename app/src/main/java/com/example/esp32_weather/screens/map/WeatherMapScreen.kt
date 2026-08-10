package com.example.esp32_weather.screens.map

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.UrlTileProvider
import com.google.maps.android.compose.*
import java.net.URL

// --- PASTE YOUR OPENWEATHERMAP API KEY HERE ---
private const val OPENWEATHER_API_KEY = "YOUR_OPENWEATHER_API_KEY_HERE"

@Composable
fun WeatherMapScreen() {
    val southAfrica = LatLng(-28.4793, 24.6727)
    val poland = LatLng(51.9194, 19.1451)

    var currentLayer by remember { mutableStateOf("clouds_new") }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(southAfrica, 5f)
    }

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
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Regional Forecast Overlay",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Location Jump Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = {
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(southAfrica, 5f)
                    }) {
                        Text("South Africa")
                    }

                    Button(onClick = {
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(poland, 5.5f)
                    }) {
                        Text("Poland")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Layer Toggle Controls (Clouds / Rain / Temp)
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

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(zoomControlsEnabled = true)
        ) {
            TileOverlay(tileProvider = weatherTileProvider)
        }
    }
}