package com.time1043.feedah_kt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.time1043.feedah_kt.ui.FeedahApp
import com.time1043.feedah_kt.ui.theme.FeedahktTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FeedahktTheme {
                FeedahApp()
            }
        }
    }
}
