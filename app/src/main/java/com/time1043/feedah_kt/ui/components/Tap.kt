package com.time1043.feedah_kt.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * The app's taps are ripple-free: the source's Pressables carry no
 * android_ripple. Every clickable pairs this remembered interaction source
 * with `indication = null`.
 */
@Composable
fun rememberNoIndication(): MutableInteractionSource = remember { MutableInteractionSource() }
