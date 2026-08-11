package com.example.esp32_weather.data.model

data class DailyWeatherSummary(
    val dayOfWeek: String = "",
    val dateString: String = "",
    val minTempOut: Float = 0f,
    val maxTempOut: Float = 0f,
    val avgHumidity: Float = 0f
)