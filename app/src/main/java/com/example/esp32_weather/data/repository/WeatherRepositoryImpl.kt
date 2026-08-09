package com.example.esp32_weather.data.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.example.esp32_weather.data.model.CurrentWeather
import com.example.esp32_weather.data.model.HourlyWeather
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class WeatherRepositoryImpl(
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance("https://esp32-weather-f6baa-default-rtdb.europe-west1.firebasedatabase.app")
) : WeatherRepository {

    override fun getLiveWeather(): Flow<CurrentWeather?> = callbackFlow {
        // Point to the exact same path the ESP32 overwrites every minute
        val reference = database.getReference("weather/current")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Firebase automatically maps the JSON into our Kotlin data class
                val weather = snapshot.getValue(CurrentWeather::class.java)

                // Send the new data down the Flow pipeline to the UI
                trySend(weather)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        // Attach the listener
        reference.addValueEventListener(listener)

        // Clean up the listener if the app is closed or the user leaves the screen
        awaitClose { reference.removeEventListener(listener) }
    }

    override fun getHourlyHistory(dateString: String): Flow<List<HourlyWeather>> = callbackFlow {
        // Dynamically point to today's date folder (e.g., "weather/history/hourly/2026-08-09")
        val reference = database.getReference("weather/history/hourly/$dateString")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val historyList = mutableListOf<HourlyWeather>()

                // Loop through every hour that has been saved today
                for (childSnapshot in snapshot.children) {
                    val hourlyData = childSnapshot.getValue(HourlyWeather::class.java)
                    if (hourlyData != null) {
                        historyList.add(hourlyData)
                    }
                }

                trySend(historyList)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        reference.addValueEventListener(listener)
        awaitClose { reference.removeEventListener(listener) }
    }
}