package com.vegerot.apodesktop.ui.main

import com.vegerot.apodesktop.data.DataRepository
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MainScreenViewModelTest {
    @Test
    fun uiState_initiallyLoading() = runTest {
        val viewModel = MainScreenViewModel(FakeDataRepository())
        // The state starts as Loading before the launched refresh finishes
        assertEquals(MainScreenUiState.Loading, viewModel.uiState.value)
    }
}

private class FakeDataRepository : DataRepository {
    private val _isDailyEnabled = MutableStateFlow(false)
    override val isDailyEnabled: StateFlow<Boolean> = _isDailyEnabled.asStateFlow()

    override fun setDailyEnabled(enabled: Boolean) {
        _isDailyEnabled.value = enabled
    }
}
