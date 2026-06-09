package com.vegerot.apodesktop

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
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

    suspend fun fetchRecentApod(): ApodEntry? = withContext(Dispatchers.IO) {
        try {
            val daysToLookBack = 3
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            calendar.add(Calendar.DAY_OF_YEAR, -daysToLookBack)

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            dateFormat.timeZone = TimeZone.getTimeZone("UTC")
            val startDate = dateFormat.format(calendar.time)

            val apiUrl = "$NASA_API_URL?api_key=$API_KEY&start_date=$startDate"
            Log.d(TAG, "Fetching APOD from: $apiUrl")

            val url = URL(apiUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"

            if (connection.responseCode !in 200..299) {
                Log.e(TAG, "API request failed with response code: ${connection.responseCode}")
                return@withContext null
            }

            val responseString = connection.inputStream.bufferedReader().use { it.readText() }
            val entries = ApodEntry.fromJsonArray(responseString)
            entries.lastOrNull { it.mediaType == "image" }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching APOD", e)
            null
        }
    }

    suspend fun updateWallpaper(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val entry = fetchRecentApod() ?: return@withContext false
            val imageUrl = entry.hdUrl ?: entry.url

            Log.d(TAG, "Downloading image from: $imageUrl")
            val imageConnection = URL(imageUrl).openConnection() as HttpURLConnection
            imageConnection.requestMethod = "GET"

            if (imageConnection.responseCode !in 200..299) {
                Log.e(TAG, "Image download failed with response code: ${imageConnection.responseCode}")
                return@withContext false
            }

            Log.d(TAG, "Setting wallpaper...")
            val inputStream: InputStream = imageConnection.inputStream
            val wallpaperManager = WallpaperManager.getInstance(context)
            wallpaperManager.setStream(inputStream)

            Log.d(TAG, "Wallpaper updated successfully")
            showNotification(context)
            true
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
