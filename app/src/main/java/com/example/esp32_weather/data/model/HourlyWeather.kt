package com.example.esp32_weather.data.model

data class HourlyWeather(
    val avgTemperature: Float = 0f,
    val avgHumidity: Float = 0f,
    val avgPressure: Float = 0f,
    val avgLux: Float = 0f,
    val maxTemperature: Float = 0f,
    val minTemperature: Float = 0f,
    val dataPointsCount: Int = 0,
    val timeString: String = ""
)