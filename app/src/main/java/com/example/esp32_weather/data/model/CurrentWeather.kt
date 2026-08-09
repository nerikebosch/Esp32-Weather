package com.example.esp32_weather.data.model

// Default values (0f, 0L) are REQUIRED for Firebase to deserialize the JSON properly
data class CurrentWeather(
    val temperature: Float = 0f,
    val humidity: Float = 0f,
    val pressure: Float = 0f,
    val lux: Float = 0f,
    val timestamp: Long = 0L
)