package com.smartledger.app.feature.workforce.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartledger.app.core.util.UiState
import com.smartledger.app.feature.workforce.data.dto.WorkRecordResponse
import com.smartledger.app.feature.workforce.domain.WorkforceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkRecordListViewModel @Inject constructor(
    private val repository: WorkforceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<WorkRecordResponse>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<WorkRecordResponse>>> = _uiState.asStateFlow()

    private val _selectedWorkerId = MutableStateFlow<String?>(null)
    val selectedWorkerId: StateFlow<String?> = _selectedWorkerId.asStateFlow()

    init {
        loadRecords()
    }

    fun filterByWorker(workerId: String?) {
        _selectedWorkerId.value = workerId
        loadRecords()
    }

    fun loadRecords() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            repository.getWorkRecords(_selectedWorkerId.value).fold(
                onSuccess = { _uiState.value = UiState.Success(it) },
                onFailure = { _uiState.value = UiState.Error(null, it.localizedMessage ?: "Failed to load work records") }
            )
        }
    }
}
