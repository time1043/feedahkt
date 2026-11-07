package com.time1043.feedah_kt.ui.screen.tab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onOpenSearch: () -> Unit,
    onOpenStudy: () -> Unit,
    onOpenReview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "Home page")
        Button(onClick = onOpenSearch) { Text("Open Search") }
        Button(onClick = onOpenStudy) { Text("Open Study") }
        Button(onClick = onOpenReview) { Text("Open Review") }
    }
}
