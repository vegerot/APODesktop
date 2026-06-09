package com.vegerot.apodesktop.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vegerot.apodesktop.ApodDesktop
import com.vegerot.apodesktop.ApodEntry
import com.vegerot.apodesktop.data.DataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainScreenViewModel(private val dataRepository: DataRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<MainScreenUiState>(MainScreenUiState.Loading)
    val uiState: StateFlow<MainScreenUiState> = _uiState.asStateFlow()

    val isDailyEnabled: StateFlow<Boolean> = dataRepository.isDailyEnabled

    init {
        refreshApod()
    }

    fun refreshApod() {
        viewModelScope.launch {
            _uiState.value = MainScreenUiState.Loading
            val entry = ApodDesktop.fetchRecentApod()
            if (entry != null) {
                _uiState.value = MainScreenUiState.Success(entry)
            } else {
                _uiState.value = MainScreenUiState.Error("Failed to load today's Astronomy Picture from NASA.")
            }
        }
    }

    fun setDailyEnabled(enabled: Boolean) {
        dataRepository.setDailyEnabled(enabled)
    }
}

sealed interface MainScreenUiState {
    data object Loading : MainScreenUiState

    data class Error(val message: String) : MainScreenUiState

    data class Success(val entry: ApodEntry) : MainScreenUiState
}
