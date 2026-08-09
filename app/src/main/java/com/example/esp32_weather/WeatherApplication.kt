package com.example.esp32_weather

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.esp32_weather.screens.WeatherHistory
import com.example.esp32_weather.ui.theme.Esp32WeatherTheme

class WeatherApplication : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Esp32WeatherTheme {
                WeatherHistory()
            }
        }
    }
}