package com.smartledger.app.feature.parties.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartledger.app.core.util.UiState
import com.smartledger.app.feature.parties.data.dto.PartyResponse
import com.smartledger.app.feature.parties.data.dto.UpdatePartyRequest
import com.smartledger.app.feature.parties.domain.PartiesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PartyDetailViewModel @Inject constructor(
    private val partiesRepository: PartiesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<PartyResponse>>(UiState.Loading)
    val uiState: StateFlow<UiState<PartyResponse>> = _uiState.asStateFlow()

    private val _isUpdating = MutableStateFlow(false)
    val isUpdating: StateFlow<Boolean> = _isUpdating.asStateFlow()

    fun loadParty(id: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = partiesRepository.getPartyById(id)
            result.fold(
                onSuccess = { party ->
                    _uiState.value = UiState.Success(party)
                },
                onFailure = { error ->
                    _uiState.value = UiState.Error(
                        code = null,
                        message = error.localizedMessage ?: "Failed to load party details"
                    )
                }
            )
        }
    }

    fun toggleStatus(id: String) {
        val currentState = _uiState.value
        if (currentState !is UiState.Success) return

        val newStatus = if (currentState.data.isActive) "INACTIVE" else "ACTIVE"

        viewModelScope.launch {
            _isUpdating.value = true
            val result = partiesRepository.updateParty(
                id = id,
                request = UpdatePartyRequest(status = newStatus)
            )
            result.fold(
                onSuccess = { updated ->
                    _uiState.value = UiState.Success(updated)
                },
                onFailure = { error ->
                    // Keep current data but log/show error if needed
                }
            )
            _isUpdating.value = false
        }
    }
}
