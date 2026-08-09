package com.example.esp32_weather.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.esp32_weather.data.model.CurrentWeather
import com.example.esp32_weather.data.model.HourlyWeather
import com.example.esp32_weather.data.repository.WeatherRepository
import com.example.esp32_weather.data.repository.WeatherRepositoryImpl
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WeatherViewModel(
    private val repository: WeatherRepository = WeatherRepositoryImpl()
) : ViewModel() {

    // 1. Live Weather Stream
    val currentWeather: StateFlow<CurrentWeather?> = repository.getLiveWeather()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    // Get today's date in "YYYY-MM-DD" format to query Firebase
    private val todayString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    // 2. Today's Hourly History Stream
    val todayHistory: StateFlow<List<HourlyWeather>> = repository.getHourlyHistory(todayString)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // 3. Calculate Sunlight duration (Estimate: Hours where avgLux > 1000)
    val sunlightHoursToday: StateFlow<Int> = todayHistory.map { historyList ->
        historyList.count { it.avgLux > 1000f }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0)
}