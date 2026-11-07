package com.time1043.feedah_kt.ui.screen.tab

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.time1043.feedah_kt.ui.state.rememberCounterState

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val (count, doubleCount, increment) = rememberCounterState()

    Column {
        Text(text = "$count x 2 = $doubleCount")
        Button(onClick = increment) { Text("Inc") }
    }
}
