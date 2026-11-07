package com.time1043.feedah_kt.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BucketScreen(
    onOpenWords: () -> Unit,
    onOpenRounds: () -> Unit,
    onOpenBucketSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "Bucket page")
        Button(onClick = onOpenWords) { Text("Open Words") }
        Button(onClick = onOpenRounds) { Text("Open Rounds") }
        Button(onClick = onOpenBucketSettings) { Text("Open Bucket Settings") }
    }
}
