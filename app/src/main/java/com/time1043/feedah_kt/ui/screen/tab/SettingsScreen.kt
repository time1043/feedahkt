package com.time1043.feedah_kt.ui.screen.tab

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val count by viewModel.count.collectAsState()
    val doubleCount = viewModel.doubleCount

    Column {
        Text(text = "$count x 2 = $doubleCount")
        Button(onClick = { viewModel.increment() } ) { Text("Inc") }
    }
}
