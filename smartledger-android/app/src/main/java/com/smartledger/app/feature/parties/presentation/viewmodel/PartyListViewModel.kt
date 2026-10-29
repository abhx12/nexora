package com.smartledger.app.feature.parties.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartledger.app.core.util.UiState
import com.smartledger.app.feature.parties.data.dto.PartyResponse
import com.smartledger.app.feature.parties.domain.PartiesRepository
import com.smartledger.app.feature.parties.domain.model.PartyType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PartyListViewModel @Inject constructor(
    private val partiesRepository: PartiesRepository
) : ViewModel() {

    private val _selectedType = MutableStateFlow<PartyType?>(PartyType.CUSTOMER)
    val selectedType: StateFlow<PartyType?> = _selectedType.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _uiState = MutableStateFlow<UiState<List<PartyResponse>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<PartyResponse>>> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadParties()
    }

    fun selectType(type: PartyType?) {
        if (_selectedType.value != type) {
            _selectedType.value = type
            loadParties()
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            // Debounce user search typing by 300ms
            delay(300)
            loadParties()
        }
    }

    fun loadParties() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            fetchParties()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            fetchParties()
            _isRefreshing.value = false
        }
    }

    private suspend fun fetchParties() {
        val result = partiesRepository.getParties(
            type = _selectedType.value,
            search = _searchQuery.value
        )
        result.fold(
            onSuccess = { parties ->
                _uiState.value = UiState.Success(parties)
            },
            onFailure = { error ->
                _uiState.value = UiState.Error(
                    code = null,
                    message = error.localizedMessage ?: "Failed to load parties"
                )
            }
        )
    }
}
