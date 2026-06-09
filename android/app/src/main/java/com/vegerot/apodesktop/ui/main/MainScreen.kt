package com.vegerot.apodesktop.ui.main

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.vegerot.apodesktop.ApodDesktop
import com.vegerot.apodesktop.ApodEntry
import com.vegerot.apodesktop.ApodWorker
import com.vegerot.apodesktop.data.DefaultDataRepository
import com.vegerot.apodesktop.theme.APODesktopTheme
import com.vegerot.apodesktop.ui.components.NetworkImage
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
    context: Context = LocalContext.current,
    viewModel: MainScreenViewModel = viewModel {
        MainScreenViewModel(DefaultDataRepository(context.applicationContext))
    },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isDailyEnabled by viewModel.isDailyEnabled.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    var isUpdatingWallpaper by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("APODesktop", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(
                        onClick = {
                            val success = ApodDesktop.shareApodImage(
                                context,
                                (state as? MainScreenUiState.Success)?.entry?.title ?: "",
                                (state as? MainScreenUiState.Success)?.entry?.date ?: "",
                            )
                            if (!success) {
                                Toast.makeText(context, "Image is still loading. Please wait.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = state is MainScreenUiState.Success,
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share APOD")
                    }
                    IconButton(onClick = { viewModel.refreshApod() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh APOD")
                    }
                },
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (val s = state) {
                MainScreenUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is MainScreenUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = s.message,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.refreshApod() }) {
                            Text("Retry")
                        }
                    }
                }
                is MainScreenUiState.Success -> {
                    ApodContent(
                        entry = s.entry,
                        isDailyEnabled = isDailyEnabled,
                        isUpdatingWallpaper = isUpdatingWallpaper,
                        onDailyEnabledChange = { enabled ->
                            viewModel.setDailyEnabled(enabled)
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
                                    workRequest,
                                )
                                Toast.makeText(context, "Daily updates enabled", Toast.LENGTH_SHORT).show()
                            } else {
                                workManager.cancelUniqueWork("DailyApodWorker")
                                Toast.makeText(context, "Daily updates disabled", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onUpdateNowClick = {
                            isUpdatingWallpaper = true
                            Toast.makeText(context, "Updating wallpaper...", Toast.LENGTH_SHORT).show()
                            coroutineScope.launch {
                                val success = ApodDesktop.updateWallpaper(context)
                                isUpdatingWallpaper = false
                                if (success) {
                                    Toast.makeText(context, "Wallpaper updated successfully", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Failed to update wallpaper", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
internal fun ApodContent(
    entry: ApodEntry,
    isDailyEnabled: Boolean,
    isUpdatingWallpaper: Boolean,
    onDailyEnabledChange: (Boolean) -> Unit,
    onUpdateNowClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        NetworkImage(
            url = entry.hdUrl ?: entry.url,
            contentDescription = entry.title,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)),
            contentScale = ContentScale.Crop,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
        ) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = entry.date,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = entry.explanation,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.25f,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Daily Wallpaper Automation",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Switch(
                            checked = isDailyEnabled,
                            onCheckedChange = onDailyEnabledChange,
                            enabled = !isUpdatingWallpaper,
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onUpdateNowClick,
                        enabled = !isUpdatingWallpaper,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (isUpdatingWallpaper) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text("Set as Wallpaper Now")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    APODesktopTheme {
        ApodContent(
            entry = ApodEntry(
                title = "A Beautiful Space Nebula",
                explanation = "This explanation details the stunning visual features of this space nebula captured by NASA.",
                date = "2026-06-09",
                mediaType = "image",
                url = "",
                hdUrl = "",
            ),
            isDailyEnabled = true,
            isUpdatingWallpaper = false,
            onDailyEnabledChange = {},
            onUpdateNowClick = {},
        )
    }
}
