package com.vegerot.apodesktop

import android.app.WallpaperManager
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object ApodDesktop {
    private const val TAG = "ApodDesktop"
    private const val API_KEY = "JvhDwQU1Uhv7yfaQTSqcsncZjwF5ZJR6McrzVE4f"
    private const val NASA_API_URL = "https://api.nasa.gov/planetary/apod"

    suspend fun updateWallpaper(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            // Look back a few days just in case recent ones are videos
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
                return@withContext false
            }

            val responseString = connection.inputStream.bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(responseString)

            var imageUrl: String? = null

            // Iterate backwards to get the most recent image
            for (i in jsonArray.length() - 1 downTo 0) {
                val item = jsonArray.getJSONObject(i)
                if (item.optString("media_type") == "image") {
                    imageUrl = item.optString("hdurl").takeIf { it.isNotEmpty() } 
                        ?: item.optString("url")
                    break
                }
            }

            if (imageUrl == null) {
                Log.e(TAG, "No image found in recent APOD entries")
                return@withContext false
            }

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
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating wallpaper", e)
            false
        }
    }
}
