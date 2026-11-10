package com.time1043.feedah_kt

import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.time1043.feedah_kt.db.DB_NAME
import com.time1043.feedah_kt.db.getRawDb
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
            if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
                StudioBridge()
            }
        }
    }
}

/** Dev-only bridge that exposes the raw database handle to the database
 * tools — Android Studio's Database Inspector attaches to the open handle
 * of a debuggable process on its own, so the bridge only names the file.
 * Rendered only after getDb() has resolved, so the handle is non-null.
 * (RN hands getRawDb() to expo-drizzle-studio-plugin under __DEV__.) */@Composable
private fun StudioBridge() {
    LaunchedEffect(Unit) {
        val raw = getRawDb()
        if (raw != null && raw.isOpen) {
            Log.i("feedah-studio", "database open for inspection: $DB_NAME")
        }
    }
}
