package com.nexora.desktop.viewmodel.dashboard

import androidx.lifecycle.viewModelScope
import com.nexora.desktop.api.dto.AiAdvisoryResponse
import com.nexora.desktop.api.dto.DashboardResponse
import com.nexora.desktop.repository.ApiResult
import com.nexora.desktop.repository.DashboardRepository
import com.nexora.desktop.viewmodel.common.DetailViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Clock

/**
 * S-DASH-01. Holds the server's dashboard exactly as returned: money available, obligations and stock are
 * three separate groups and nothing is added, subtracted or derived here (FR-RPT-02, NFR-USE-02).
 * Also exposes Post-MVP AI Financial Advisory and Forecasting insights.
 */
class DashboardViewModel(private val repository: DashboardRepository, clock: Clock) :
    DetailViewModel<DashboardResponse>(clock, { repository.summary() }) {

    private val _advisory = MutableStateFlow<AiAdvisoryResponse?>(null)
    val advisory: StateFlow<AiAdvisoryResponse?> = _advisory.asStateFlow()

    init {
        loadAdvisory()
    }

    fun loadAdvisory() {
        viewModelScope.launch {
            when (val res = repository.aiAdvisory()) {
                is ApiResult.Success -> _advisory.value = res.value
                is ApiResult.Failure -> { /* graceful fallback */ }
            }
        }
    }

    companion object {
        /** Auto refresh while the dashboard is visible (06 S-DASH-01). */
        const val REFRESH_MILLIS = 5 * 60 * 1000L
    }
}
