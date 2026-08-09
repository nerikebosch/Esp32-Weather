package com.example.esp32_weather.data.repository

import com.example.esp32_weather.data.model.CurrentWeather
import com.example.esp32_weather.data.model.HourlyWeather
import kotlinx.coroutines.flow.Flow

interface WeatherRepository {
    // Returns a constant stream of the live data
    fun getLiveWeather(): Flow<CurrentWeather?>

    // Returns a stream of the history list for a specific date (e.g., "2026-08-09")
    fun getHourlyHistory(dateString: String): Flow<List<HourlyWeather>>

    fun saveRainData(dateString: String, amountMm: Float)
    fun getRainHistory(): Flow<Map<String, Float>>
}