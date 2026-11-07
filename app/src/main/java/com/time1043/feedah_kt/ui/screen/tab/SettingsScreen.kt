package com.time1043.feedah_kt.ui.screen.tab

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    var count by remember { mutableIntStateOf(1) } // preserve across recompositions
    println("App recomposed, count is: $count") // 1, 2, 3, ... // reset to 1 on dark theme toggle (Activity recreation)

    Button(onClick = { count++ }) { Text("$count") }
}
