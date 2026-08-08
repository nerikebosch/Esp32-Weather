package com.example.esp32_weather.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun WeatherHistory(
    viewModel:
) {
    Scaffold {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Esp32 Weather App")
            Spacer(Modifier.size(16.dp))
            Text("Today's History")

            SimpleTable(

            )
        }
    }

}

@Composable
fun SimpleTable(data: List<Pair<String, String>>) {
    Column(modifier = Modifier.padding(16.dp)) {
        // Table Header
        Row(Modifier.background(Color.LightGray).fillMaxWidth().padding(8.dp)) {
            Text("Key", Modifier.weight(1f), fontWeight = FontWeight.Bold)
            Text("Value", Modifier.weight(1.0f), fontWeight = FontWeight.Bold)
        }

        // Table Rows
        data.forEach { (key, value) ->
            Row(Modifier.fillMaxWidth().padding(8.dp)) {
                Text(key, Modifier.weight(1f))
                Text(value, Modifier.weight(1f))
            }
        }
    }
}


@Preview
@Composable
fun WeatherHistoryPreview() {
    WeatherHistory()
}

