package com.time1043.feedah_kt

import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.time1043.feedah_kt.db.DB_NAME
import com.time1043.feedah_kt.db.getRawDb
import com.time1043.feedah_kt.db.getDb
import com.time1043.feedah_kt.ui.FeedahApp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var dbReady by remember { mutableStateOf(false) }
            var dbError by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()
            val appContext = applicationContext

            // Both outcomes land in the coroutine's callbacks — nothing sets
            // state synchronously in the effect body, which would only buy an
            // extra render on mount.
            val openDb: () -> Unit = {
                scope.launch {
                    runCatching { getDb(appContext) }
                        .onSuccess { dbReady = true }
                        .onFailure {
                            Log.e("MainActivity", "Failed to init DB", it)
                            dbError = true
                        }
                }
            }
            LaunchedEffect(Unit) { openDb() }

            /** The retry button is the only path that resets the flags:
             * clearing them here (an event handler) keeps the effect free of
             * a synchronous setState. */
            val retry: () -> Unit = {
                dbReady = false
                dbError = false
                openDb()
            }

            if (!dbReady) {
                if (!dbError) {
                    return@setContent
                }
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "The database failed to open.",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "Retry",
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.Button, onClick = retry),
                    )
                }
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
 * (RN hands getRawDb() to expo-drizzle-studio-plugin under __DEV__.) */
@Composable
private fun StudioBridge() {
    LaunchedEffect(Unit) {
        val raw = getRawDb()
        if (raw != null && raw.isOpen) {
            Log.i("feedah-studio", "database open for inspection: $DB_NAME")
        }
    }
}
