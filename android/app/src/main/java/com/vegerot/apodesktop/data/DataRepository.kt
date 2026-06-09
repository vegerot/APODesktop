package com.vegerot.apodesktop.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface DataRepository {
    val isDailyEnabled: StateFlow<Boolean>
    fun setDailyEnabled(enabled: Boolean)
}

class DefaultDataRepository(context: Context) : DataRepository {
    private val prefs = context.applicationContext.getSharedPreferences("apod_prefs", Context.MODE_PRIVATE)
    private val _isDailyEnabled = MutableStateFlow(prefs.getBoolean("daily_enabled", false))
    override val isDailyEnabled: StateFlow<Boolean> = _isDailyEnabled.asStateFlow()

    override fun setDailyEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("daily_enabled", enabled).apply()
        _isDailyEnabled.value = enabled
    }
}
