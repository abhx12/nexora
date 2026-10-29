package com.smartledger.app.feature.dashboard.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartledger.app.core.util.UiState
import com.smartledger.app.feature.dashboard.data.dto.DashboardSummaryResponse
import com.smartledger.app.feature.dashboard.domain.DashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val dashboardRepository: DashboardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<DashboardSummaryResponse>>(UiState.Loading)
    val uiState: StateFlow<UiState<DashboardSummaryResponse>> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadSummary()
    }

    fun loadSummary() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            fetchSummary()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            fetchSummary()
            _isRefreshing.value = false
        }
    }

    private suspend fun fetchSummary() {
        val result = dashboardRepository.getDashboardSummary()
        result.fold(
            onSuccess = { summary ->
                _uiState.value = UiState.Success(summary)
            },
            onFailure = { error ->
                _uiState.value = UiState.Error(
                    code = null,
                    message = error.localizedMessage ?: "Failed to load dashboard summary"
                )
            }
        )
    }
}
