package com.vegerot.apodesktop

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object ApodDesktop {
    private const val TAG = "ApodDesktop"

    /** TODO: remove secret */
    private const val API_KEY = "JvhDwQU1Uhv7yfaQTSqcsncZjwF5ZJR6McrzVE4f"
    private const val NASA_API_URL = "https://api.nasa.gov/planetary/apod"

    suspend fun fetchRecentImageApods(): List<ApodEntry> = withContext(Dispatchers.IO) {
        try {
            executeWithRetry(times = 3) {
                val daysToLookBack = 5
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                calendar.add(Calendar.DAY_OF_YEAR, -daysToLookBack)

                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                dateFormat.timeZone = TimeZone.getTimeZone("UTC")
                val startDate = dateFormat.format(calendar.time)

                val apiUrl = "$NASA_API_URL?api_key=$API_KEY&start_date=$startDate"
                Log.d(TAG, "Fetching APOD from: $apiUrl")

                val connection = setupConnection(apiUrl, 5000)
                connection.requestMethod = "GET"

                if (connection.responseCode !in 200..299) {
                    throw Exception("API request failed with response code: ${connection.responseCode}")
                }

                val responseString = connection.inputStream.bufferedReader().use { it.readText() }
                val entries = ApodEntry.fromJsonArray(responseString)
                entries.filter { it.mediaType == "image" }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching APOD", e)
            emptyList()
        }
    }

    suspend fun fetchRecentApod(): ApodEntry? {
        return fetchRecentImageApods().lastOrNull()
    }

    suspend fun updateWallpaper(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val imageEntries = fetchRecentImageApods()
            if (imageEntries.isEmpty()) return@withContext false

            val todayEntry = imageEntries.last()
            val yesterdayEntry = if (imageEntries.size >= 2) imageEntries[imageEntries.size - 2] else todayEntry

            val wallpaperManager = WallpaperManager.getInstance(context)
            var success = false

            // Set Homescreen (today's APOD)
            val todayImageUrl = todayEntry.hdUrl ?: todayEntry.url
            Log.d(TAG, "Downloading today's image from: $todayImageUrl")
            try {
                executeWithRetry(times = 3) {
                    val todayConnection = setupConnection(todayImageUrl, 5000)
                    todayConnection.requestMethod = "GET"

                    if (todayConnection.responseCode in 200..299) {
                        Log.d(TAG, "Setting homescreen wallpaper...")
                        wallpaperManager.setStream(todayConnection.inputStream, null, true, WallpaperManager.FLAG_SYSTEM)
                        success = true
                    } else {
                        throw Exception("Today's image download failed with response code: ${todayConnection.responseCode}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting homescreen wallpaper", e)
            }

            // Set Lockscreen (yesterday's APOD)
            val yesterdayImageUrl = yesterdayEntry.hdUrl ?: yesterdayEntry.url
            Log.d(TAG, "Downloading yesterday's image from: $yesterdayImageUrl")
            try {
                executeWithRetry(times = 3) {
                    val yesterdayConnection = setupConnection(yesterdayImageUrl, 5000)
                    yesterdayConnection.requestMethod = "GET"

                    if (yesterdayConnection.responseCode in 200..299) {
                        Log.d(TAG, "Setting lockscreen wallpaper...")
                        val lockBitmap = BitmapFactory.decodeStream(yesterdayConnection.inputStream)
                        if (lockBitmap != null) {
                            val displayMetrics = context.resources.displayMetrics
                            val screenWidth = displayMetrics.widthPixels
                            val screenHeight = displayMetrics.heightPixels

                            val croppedBitmap = centerCropBitmap(lockBitmap, screenWidth, screenHeight)
                            
                            wallpaperManager.setBitmap(croppedBitmap, null, true, WallpaperManager.FLAG_LOCK)
                            
                            if (croppedBitmap != lockBitmap) croppedBitmap.recycle()
                            lockBitmap.recycle()
                            
                            success = true
                        } else {
                            throw Exception("Failed to decode yesterday's image")
                        }
                    } else {
                        throw Exception("Yesterday's image download failed with response code: ${yesterdayConnection.responseCode}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting lockscreen wallpaper", e)
            }

            if (success) {
                Log.d(TAG, "Wallpaper(s) updated successfully")
                showNotification(context)
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error updating wallpaper", e)
            false
        }
    }

    private fun showNotification(context: Context) {
        val channelId = "wallpaper_updates"
        val channelName = "Wallpaper Updates"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Notifications when the APOD wallpaper is successfully updated"
        }
        notificationManager.createNotificationChannel(channel)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Wallpaper Updated")
            .setContentText("The Astronomy Picture of the Day has been set as your wallpaper.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            notificationManager.notify(1, builder.build())
        } catch (e: SecurityException) {
            Log.e(TAG, "Notification permission not granted", e)
        }
    }

    fun shareApodImage(context: Context, title: String, date: String): Boolean {
        try {
            val file = File(context.cacheDir, "apod_today.jpg")
            if (!file.exists()) {
                return false
            }

            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)

            val formattedDate = date.replace("-", "").let {
                if (it.length >= 8) it.substring(2) else ""
            }
            val detailsUrl = if (formattedDate.isNotEmpty()) {
                "https://apod.nasa.gov/apod/ap$formattedDate.html"
            } else {
                "https://apod.nasa.gov/apod/"
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Check out NASA's Astronomy Picture of the Day: \"$title\"\n$detailsUrl",
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooserIntent = Intent.createChooser(shareIntent, "Share APOD").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooserIntent)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing APOD image", e)
            return false
        }
    }

    private fun centerCropBitmap(source: Bitmap, desiredWidth: Int, desiredHeight: Int): Bitmap {
        val scale = maxOf(desiredWidth.toFloat() / source.width, desiredHeight.toFloat() / source.height)
        val scaledWidth = (scale * source.width).toInt()
        val scaledHeight = (scale * source.height).toInt()

        val scaledBitmap = Bitmap.createScaledBitmap(source, scaledWidth, scaledHeight, true)
        val x = maxOf(0, (scaledWidth - desiredWidth) / 2)
        val y = maxOf(0, (scaledHeight - desiredHeight) / 2)

        val croppedBitmap = Bitmap.createBitmap(scaledBitmap, x, y, minOf(desiredWidth, scaledWidth), minOf(desiredHeight, scaledHeight))
        
        if (scaledBitmap != source && scaledBitmap != croppedBitmap) {
            scaledBitmap.recycle()
        }
        
        return croppedBitmap
    }
}

data class ApodEntry(
    val title: String,
    val explanation: String,
    val date: String,
    val mediaType: String,
    val url: String,
    val hdUrl: String?,
) {
    companion object {
        fun fromJsonArray(jsonString: String): List<ApodEntry> {
            val jsonArray = JSONArray(jsonString)
            return (0 until jsonArray.length()).map { i ->
                fromJson(jsonArray.getJSONObject(i))
            }
        }

        fun fromJson(json: JSONObject): ApodEntry = ApodEntry(
            title = json.optString("title"),
            explanation = json.optString("explanation"),
            date = json.optString("date"),
            mediaType = json.optString("media_type"),
            url = json.optString("url"),
            hdUrl = json.optString("hdurl").takeIf { it.isNotEmpty() },
        )
    }
}
