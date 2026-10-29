package com.smartledger.app.feature.workforce.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartledger.app.core.util.UiState
import com.smartledger.app.feature.workforce.data.dto.WorkerPayableResponse
import com.smartledger.app.feature.workforce.domain.WorkforceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkerPayableViewModel @Inject constructor(
    private val repository: WorkforceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<WorkerPayableResponse>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<WorkerPayableResponse>>> = _uiState.asStateFlow()

    init {
        loadPayables()
    }

    fun loadPayables() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            repository.getAllWorkersPayable().fold(
                onSuccess = { _uiState.value = UiState.Success(it) },
                onFailure = { _uiState.value = UiState.Error(null, it.localizedMessage ?: "Failed to load worker payables") }
            )
        }
    }
}
