package com.vegerot.apodesktop

import kotlinx.coroutines.delay
import java.net.HttpURLConnection
import java.net.URL
import java.io.IOException

suspend fun <T> executeWithRetry(
    times: Int = 3,
    block: suspend () -> T
): T {
    var exception: Exception? = null
    for (i in 1..times) {
        try {
            return block()
        } catch (e: Exception) {
            exception = e
            if (i < times) {
                delay(1000)
            }
        }
    }
    throw exception ?: RuntimeException("Retry failed")
}

fun setupConnection(url: String, timeoutMs: Int = 5000): HttpURLConnection {
    val connection = URL(url).openConnection() as HttpURLConnection
    connection.connectTimeout = timeoutMs
    connection.readTimeout = timeoutMs
    return connection
}
