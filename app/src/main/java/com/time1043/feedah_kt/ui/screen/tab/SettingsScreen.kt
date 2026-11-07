package com.time1043.feedah_kt.ui.screen.tab

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    // var count = mutableStateOf(1)
    var count = mutableIntStateOf(1)

    // Button(onClick = { count.value++ }) { Text("$count") } // MutableState(value=1)@937520472
    Button(onClick = { count.value++ }) { Text("${count.value}") } // 1
}
