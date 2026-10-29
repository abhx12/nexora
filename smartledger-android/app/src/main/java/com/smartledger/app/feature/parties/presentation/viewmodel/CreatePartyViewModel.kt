package com.smartledger.app.feature.parties.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartledger.app.feature.parties.data.dto.CreatePartyRequest
import com.smartledger.app.feature.parties.domain.PartiesRepository
import com.smartledger.app.feature.parties.domain.model.PartyType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreatePartyUiState(
    val name: String = "",
    val partyType: PartyType = PartyType.CUSTOMER,
    val phone: String = "",
    val address: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class CreatePartyViewModel @Inject constructor(
    private val partiesRepository: PartiesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatePartyUiState())
    val uiState: StateFlow<CreatePartyUiState> = _uiState.asStateFlow()

    fun onNameChanged(newName: String) {
        _uiState.update { it.copy(name = newName, errorMessage = null) }
    }

    fun onTypeChanged(newType: PartyType) {
        _uiState.update { it.copy(partyType = newType) }
    }

    fun onPhoneChanged(newPhone: String) {
        _uiState.update { it.copy(phone = newPhone) }
    }

    fun onAddressChanged(newAddress: String) {
        _uiState.update { it.copy(address = newAddress) }
    }

    fun createParty() {
        val currentState = _uiState.value
        val name = currentState.name.trim()

        if (name.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Party name is required") }
            return
        }

        // Phone is OPTIONAL — do not mark it required
        val phone = currentState.phone.trim().takeIf { it.isNotBlank() }
        val address = currentState.address.trim().takeIf { it.isNotBlank() }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val request = CreatePartyRequest(
                name = name,
                partyType = currentState.partyType.name,
                phone = phone,
                address = address
            )

            val result = partiesRepository.createParty(request)

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true, errorMessage = null) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = false,
                            errorMessage = error.localizedMessage ?: "Failed to create party"
                        )
                    }
                }
            )
        }
    }
}
