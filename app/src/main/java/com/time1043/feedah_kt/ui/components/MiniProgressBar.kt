package com.time1043.feedah_kt.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The home readout's and the bucket card's mini progress bar: a 6dp gray
 * track with a fractional accent fill. The fill color defaults to the
 * accent; the bucket card passes gray for the buckets not being studied.
 */
@Composable
fun MiniProgressBar(
    /** The filled portion, as a fraction of the track (0..1). */
    value: Float,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp, // h-1.5
    track: Color = MaterialTheme.colorScheme.surfaceVariant, // bg-gray-200/800
    fill: Color = MaterialTheme.colorScheme.primary, // bg-accent
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .background(track, CircleShape),
    ) {
        Box(
            Modifier
                .fillMaxWidth(value.coerceIn(0f, 1f))
                .height(height)
                .background(fill, CircleShape),
        )
    }
}
