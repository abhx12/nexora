package com.smartledger.app.feature.outsourcing.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
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
class OutsourcingDetailViewModel @Inject constructor(
    private val repository: OutsourcingRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val jobId: String = checkNotNull(savedStateHandle["jobId"])

    private val _uiState = MutableStateFlow<UiState<OutsourcingJobResponse>>(UiState.Loading)
    val uiState: StateFlow<UiState<OutsourcingJobResponse>> = _uiState.asStateFlow()

    init {
        loadDetail()
    }

    fun loadDetail() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            repository.getOutsourcingJobById(jobId).fold(
                onSuccess = { job ->
                    _uiState.value = UiState.Success(job)
                },
                onFailure = { error ->
                    _uiState.value = UiState.Error(
                        code = null,
                        message = error.localizedMessage ?: "Failed to load job details"
                    )
                }
            )
        }
    }
}
