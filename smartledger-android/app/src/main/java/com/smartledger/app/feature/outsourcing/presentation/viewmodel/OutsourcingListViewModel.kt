package com.smartledger.app.feature.outsourcing.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartledger.app.core.util.UiState
import com.smartledger.app.feature.outsourcing.data.dto.OutsourcingJobResponse
import com.smartledger.app.feature.outsourcing.domain.OutsourcingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OutsourcingListViewModel @Inject constructor(
    private val repository: OutsourcingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<OutsourcingJobResponse>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<OutsourcingJobResponse>>> = _uiState.asStateFlow()

    private val _selectedStatus = MutableStateFlow<String?>("ALL")
    val selectedStatus: StateFlow<String?> = _selectedStatus.asStateFlow()

    private val _externalWipKg = MutableStateFlow(0.0)
    val externalWipKg: StateFlow<Double> = _externalWipKg.asStateFlow()

    init {
        loadJobs()
    }

    fun setStatusFilter(status: String?) {
        _selectedStatus.value = status
        loadJobs()
    }

    fun loadJobs() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            _externalWipKg.value = repository.getExternalWipKg()

            val filter = if (_selectedStatus.value == "ALL") null else _selectedStatus.value
            repository.getOutsourcingJobs(filter).fold(
                onSuccess = { jobs ->
                    _uiState.value = UiState.Success(jobs)
                },
                onFailure = { error ->
                    _uiState.value = UiState.Error(
                        code = null,
                        message = error.localizedMessage ?: "Failed to load outsourcing jobs"
                    )
                }
            )
        }
    }
}
