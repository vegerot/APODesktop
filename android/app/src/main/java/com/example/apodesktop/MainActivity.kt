package com.example.apodesktop

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.apodesktop.theme.APODesktopTheme
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            APODesktopTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ApodScreen()
                }
            }
        }
    }
}

@Composable
fun ApodScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    // For V1, we just default the switch to false, in a real app we'd read from SharedPreferences
    var isDailyEnabled by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "APODesktop",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(32.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "Enable Daily Wallpaper")
            Switch(
                checked = isDailyEnabled,
                onCheckedChange = { enabled ->
                    isDailyEnabled = enabled
                    val workManager = WorkManager.getInstance(context)
                    if (enabled) {
                        val constraints = Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()
                        val workRequest = PeriodicWorkRequestBuilder<ApodWorker>(24, TimeUnit.HOURS)
                            .setConstraints(constraints)
                            .build()
                        workManager.enqueueUniquePeriodicWork(
                            "DailyApodWorker",
                            ExistingPeriodicWorkPolicy.UPDATE,
                            workRequest
                        )
                        Toast.makeText(context, "Daily updates enabled", Toast.LENGTH_SHORT).show()
                    } else {
                        workManager.cancelUniqueWork("DailyApodWorker")
                        Toast.makeText(context, "Daily updates disabled", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = {
            Toast.makeText(context, "Updating wallpaper...", Toast.LENGTH_SHORT).show()
            coroutineScope.launch {
                val success = ApodDesktop.updateWallpaper(context)
                if (success) {
                    Toast.makeText(context, "Wallpaper updated successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to update wallpaper", Toast.LENGTH_SHORT).show()
                }
            }
        }) {
            Text("Update Now")
        }
    }
}
