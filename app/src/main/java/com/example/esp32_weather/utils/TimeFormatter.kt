package com.example.esp32_weather.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeFormatter {
    fun formatTimestamp(timestampInSeconds: Long): String {
        if (timestampInSeconds == 0L) return "--:--"

        // The ESP32 sends seconds, but Java's Date object expects milliseconds
        val date = Date(timestampInSeconds * 1000)

        // Formats to "3:16 PM" for example.
        // Use "HH:mm" instead of "h:mm a" if you prefer a 24-hour clock.
        val format = SimpleDateFormat("h:mm a", Locale.getDefault())
        return format.format(date)
    }
}