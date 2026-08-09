package com.example.esp32_weather.data.model

data class CurrentWeather(
    val temperature: Float = 0f, // Inside (BME280)
    val tempOut: Float = 0f,     // Outside (DS18B20)
    val humidity: Float = 0f,
    val pressure: Float = 0f,
    val lux: Float = 0f,
    val timestamp: Long = 0L
)