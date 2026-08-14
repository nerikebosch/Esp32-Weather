package com.example.esp32_weather.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.esp32_weather.data.model.CurrentWeather
import com.example.esp32_weather.data.model.DailyWeatherSummary
import com.example.esp32_weather.data.model.HourlyWeather
import com.example.esp32_weather.data.repository.WeatherRepositoryImpl
import com.example.esp32_weather.screens.history.HistoryPeriod
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

enum class RainPeriod {
    WEEK, MONTH, YEAR
}

data class RainBarItem(
    val label: String,
    val amountMm: Float
)

class WeatherViewModel : ViewModel() {

    private val database = FirebaseDatabase.getInstance("https://esp32-weather-f6baa-default-rtdb.europe-west1.firebasedatabase.app").getReference("weather")
    private val repository = WeatherRepositoryImpl()

    // ====================================================================
    // 0. BASE DATA FLOWS (Must be declared first!)
    // ====================================================================

    val selectedPeriod = MutableStateFlow(HistoryPeriod.HOURS_24)
    private val _hourly24h = MutableStateFlow<List<HourlyWeather>>(emptyList())
    private val _hourlyYesterday = MutableStateFlow<List<HourlyWeather>>(emptyList())
    private val _hourly48h = MutableStateFlow<List<HourlyWeather>>(emptyList())
    private val _sevenDaySummaries = MutableStateFlow<List<DailyWeatherSummary>>(emptyList())

    init {
        fetchHistoricalData()
    }

    // ====================================================================
    // 1. DASHBOARD SCREEN STATE (Live Weather)
    // ====================================================================

    val currentWeather: StateFlow<CurrentWeather?> = repository.getLiveWeather()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    // Now this works perfectly because _hourly24h already exists above!
    val sunlightHoursToday: StateFlow<String> = _hourly24h.map { todayList ->
        val sunCount = todayList.count { it.avgLux > 50f }
        sunCount.toString()
    }.stateIn(viewModelScope, SharingStarted.Lazily, "0")

    val todayHistory: StateFlow<List<HourlyWeather>> get() = _hourly24h
    val yesterdayHistory: StateFlow<List<HourlyWeather>> get() = _hourlyYesterday


    // ====================================================================
    // 2. HISTORY SCREEN STATE (24h / 48h / 7-Day)
    // ====================================================================

    val sevenDaySummaries: StateFlow<List<DailyWeatherSummary>> = _sevenDaySummaries

    val selectedDateHistory: StateFlow<List<HourlyWeather>> = combine(
        selectedPeriod,
        _hourly24h,
        _hourly48h
    ) { period, h24, h48 ->
        when (period) {
            HistoryPeriod.HOURS_24 -> h24
            HistoryPeriod.HOURS_48 -> h48
            HistoryPeriod.DAYS_7 -> emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun fetchHistoricalData() {
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val dayOfWeekFormatter = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
        val displayDateFormatter = DateTimeFormatter.ofPattern("MMM dd", Locale.getDefault())

        val today = LocalDate.now()
        val datesToFetch = (0..6).map { today.minusDays(it.toLong()) }

        database.child("history").child("hourly")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val allFetchedHours = mutableMapOf<LocalDate, List<HourlyWeather>>()
                    val summaries = mutableListOf<DailyWeatherSummary>()

                    println("FIREBASE DEBUG: Found ${snapshot.childrenCount} total date folders in history/hourly")

                    for (date in datesToFetch) {
                        val dateKey = date.format(dateFormatter)
                        val dateNode = snapshot.child(dateKey)

                        val hoursList = mutableListOf<HourlyWeather>()

                        for (hourNode in dateNode.children) {
                            val item = hourNode.getValue(HourlyWeather::class.java)
                            if (item != null) {
                                hoursList.add(item)
                            } else {
                                println("FIREBASE DEBUG: Failed to parse hour node: ${hourNode.key}")
                            }
                        }

                        println("FIREBASE DEBUG: Fetched ${hoursList.size} hours for date: $dateKey")

                        val sortedHours = hoursList.sortedBy { it.timestamp }
                        allFetchedHours[date] = sortedHours

                        if (sortedHours.isNotEmpty()) {
                            val minOut = sortedHours.minOf { it.minTempOut }
                            val maxOut = sortedHours.maxOf { it.maxTempOut }
                            val avgHum = sortedHours.map { it.avgHumidity }.average().toFloat()

                            val dayLabel = if (date == today) "Today" else date.format(dayOfWeekFormatter)

                            summaries.add(
                                DailyWeatherSummary(
                                    dayOfWeek = dayLabel,
                                    dateString = date.format(displayDateFormatter),
                                    minTempOut = minOut,
                                    maxTempOut = maxOut,
                                    avgHumidity = avgHum
                                )
                            )
                        }
                    }

                    _hourly24h.value = allFetchedHours[today] ?: emptyList()

                    val yesterday = today.minusDays(1)
                    val yesterdayHours = allFetchedHours[yesterday] ?: emptyList()
                    val todayHours = allFetchedHours[today] ?: emptyList()
                    _hourlyYesterday.value = yesterdayHours
                    _hourly48h.value = (yesterdayHours + todayHours).sortedBy { it.timestamp }

                    _sevenDaySummaries.value = summaries
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }


    // ====================================================================
    // 3. RAIN SCREEN STATE
    // ====================================================================

    private val todayString = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

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