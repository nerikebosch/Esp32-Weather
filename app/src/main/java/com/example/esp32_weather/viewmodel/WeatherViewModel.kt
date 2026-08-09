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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Date
import java.util.Locale

enum class RainPeriod { WEEK, MONTH, YEAR }

data class RainBarItem(val label: String, val amountMm: Float)

class WeatherViewModel(
    private val repository: WeatherRepository = WeatherRepositoryImpl()
) : ViewModel() {

    // --- DASHBOARD & HISTORY ---
    val currentWeather: StateFlow<CurrentWeather?> = repository.getLiveWeather()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val todayString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    val todayHistory: StateFlow<List<HourlyWeather>> = repository.getHourlyHistory(todayString)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val sunlightHoursToday: StateFlow<Int> = todayHistory.map { historyList ->
        historyList.count { it.avgLux > 1000f }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0)

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedDateHistory: StateFlow<List<HourlyWeather>> = _selectedDate
        .flatMapLatest { date ->
            val dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            repository.getHourlyHistory(dateStr)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun previousDay() { _selectedDate.value = _selectedDate.value.minusDays(1) }
    fun nextDay() {
        if (_selectedDate.value.isBefore(LocalDate.now())) {
            _selectedDate.value = _selectedDate.value.plusDays(1)
        }
    }


    // --- RAIN TRACKER LOGIC ---
    private val rainMap = repository.getRainHistory()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    val selectedRainPeriod = MutableStateFlow(RainPeriod.WEEK)
    val selectedRainDate = MutableStateFlow(LocalDate.now())

    val todayRainAmount: StateFlow<Float> = rainMap.map { map ->
        map[todayString] ?: 0f
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0f)

    fun saveTodayRain(amountMm: Float) {
        repository.saveRainData(todayString, amountMm)
    }

    // Process graph bars based on selected Period (Week / Month / Year)
    val rainGraphData: StateFlow<List<RainBarItem>> = combine(
        rainMap,
        selectedRainPeriod,
        selectedRainDate
    ) { map, period, date ->
        when (period) {
            RainPeriod.WEEK -> {
                val startOfWeek = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                (0..6).map { dayIndex ->
                    val day = startOfWeek.plusDays(dayIndex.toLong())
                    val dayStr = day.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    val label = day.format(DateTimeFormatter.ofPattern("EEE"))
                    RainBarItem(label, map[dayStr] ?: 0f)
                }
            }
            RainPeriod.MONTH -> {
                val yearMonth = YearMonth.from(date)
                (1..yearMonth.lengthOfMonth()).map { dayNum ->
                    val day = yearMonth.atDay(dayNum)
                    val dayStr = day.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    RainBarItem("$dayNum", map[dayStr] ?: 0f)
                }
            }
            RainPeriod.YEAR -> {
                val year = date.year
                (1..12).map { monthNum ->
                    val ym = YearMonth.of(year, monthNum)
                    val monthLabel = ym.format(DateTimeFormatter.ofPattern("MMM"))
                    var monthTotal = 0f
                    map.forEach { (dateKey, amount) ->
                        if (dateKey.startsWith(String.format("%04d-%02d", year, monthNum))) {
                            monthTotal += amount
                        }
                    }
                    RainBarItem(monthLabel, monthTotal)
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun previousRainPeriod() {
        when (selectedRainPeriod.value) {
            RainPeriod.WEEK -> selectedRainDate.value = selectedRainDate.value.minusWeeks(1)
            RainPeriod.MONTH -> selectedRainDate.value = selectedRainDate.value.minusMonths(1)
            RainPeriod.YEAR -> selectedRainDate.value = selectedRainDate.value.minusYears(1)
        }
    }

    fun nextRainPeriod() {
        val today = LocalDate.now()
        when (selectedRainPeriod.value) {
            RainPeriod.WEEK -> if (selectedRainDate.value.plusWeeks(1).isBefore(today.plusWeeks(1))) selectedRainDate.value = selectedRainDate.value.plusWeeks(1)
            RainPeriod.MONTH -> if (selectedRainDate.value.plusMonths(1).isBefore(today.plusMonths(1))) selectedRainDate.value = selectedRainDate.value.plusMonths(1)
            RainPeriod.YEAR -> if (selectedRainDate.value.plusYears(1).year <= today.year) selectedRainDate.value = selectedRainDate.value.plusYears(1)
        }
    }
}