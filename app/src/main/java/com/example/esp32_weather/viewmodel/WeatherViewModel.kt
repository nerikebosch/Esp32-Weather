package com.example.esp32_weather.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.esp32_weather.data.model.CurrentWeather
import com.example.esp32_weather.data.model.HourlyWeather
import com.example.esp32_weather.data.repository.WeatherRepository
import com.example.esp32_weather.data.repository.WeatherRepositoryImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

class WeatherViewModel(
    private val repository: WeatherRepository = WeatherRepositoryImpl()
) : ViewModel() {

    // --- DASHBOARD DATA (Always Today) ---
    val currentWeather: StateFlow<CurrentWeather?> = repository.getLiveWeather()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val todayString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    val todayHistory: StateFlow<List<HourlyWeather>> = repository.getHourlyHistory(todayString)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val sunlightHoursToday: StateFlow<Int> = todayHistory.map { historyList ->
        historyList.count { it.avgLux > 1000f }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0)


    // --- HISTORY SCREEN DATA (Navigable Dates) ---
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedDateHistory: StateFlow<List<HourlyWeather>> = _selectedDate
        .flatMapLatest { date ->
            val dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            repository.getHourlyHistory(dateStr)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun previousDay() {
        _selectedDate.value = _selectedDate.value.minusDays(1)
    }

    fun nextDay() {
        // Prevent navigating into the future
        if (_selectedDate.value.isBefore(LocalDate.now())) {
            _selectedDate.value = _selectedDate.value.plusDays(1)
        }
    }
}