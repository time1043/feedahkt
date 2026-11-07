package com.time1043.feedah_kt.ui.screen.tab

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    var count by mutableIntStateOf(1) // re-initialized every time the UI updates / recomposes
    println("App recomposed, count is: $count") // always 1

    Button(onClick = { count++ }) { Text("$count") }
}
