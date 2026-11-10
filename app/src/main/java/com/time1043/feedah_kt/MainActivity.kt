package com.time1043.feedah_kt

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.time1043.feedah_kt.db.getDb
import com.time1043.feedah_kt.ui.FeedahApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // The db triggers in main: open + seed gate the whole UI, so
            // every screen reads a ready database.
            var dbReady by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                runCatching { getDb(applicationContext) }
                    .onSuccess { dbReady = true }
                    .onFailure { Log.e("MainActivity", "Failed to init DB", it) }
            }

            if (!dbReady) {
                return@setContent
            }

            FeedahApp()
        }
    }
}
