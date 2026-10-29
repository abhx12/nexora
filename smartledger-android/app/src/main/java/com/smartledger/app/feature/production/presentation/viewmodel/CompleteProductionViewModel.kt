package com.smartledger.app.feature.production.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartledger.app.core.util.UiState
import com.smartledger.app.feature.production.data.dto.CompleteProductionRequest
import com.smartledger.app.feature.production.data.dto.ProductionBatchResponse
import com.smartledger.app.feature.production.domain.ProductionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs

data class EquationBalanceState(
    val inputWeight: Double = 0.0,
    val outputWeight: Double = 0.0,
    val wastageWeight: Double = 0.0,
    val sumOutputWastage: Double = 0.0,
    val difference: Double = 0.0,
    val isBalanced: Boolean = false,
    val validationMessage: String = ""
)

@HiltViewModel
class CompleteProductionViewModel @Inject constructor(
    private val repository: ProductionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val batchId: String = checkNotNull(savedStateHandle["batchId"])

    private val _batchState = MutableStateFlow<UiState<ProductionBatchResponse>>(UiState.Loading)
    val batchState: StateFlow<UiState<ProductionBatchResponse>> = _batchState.asStateFlow()

    private val _inputWeight = MutableStateFlow(0.0)
    val inputWeight: StateFlow<Double> = _inputWeight.asStateFlow()

    private val _outputWeight = MutableStateFlow("")
    val outputWeight: StateFlow<String> = _outputWeight.asStateFlow()

    private val _wastageWeight = MutableStateFlow("")
    val wastageWeight: StateFlow<String> = _wastageWeight.asStateFlow()

    private val _completionDate = MutableStateFlow(
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    )
    val completionDate: StateFlow<String> = _completionDate.asStateFlow()

    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _equationState = MutableStateFlow(EquationBalanceState())
    val equationState: StateFlow<EquationBalanceState> = _equationState.asStateFlow()

    init {
        loadBatch()
    }

    fun loadBatch() {
        viewModelScope.launch {
            _batchState.value = UiState.Loading
            repository.getProductionBatchById(batchId)
                .onSuccess { batch ->
                    _batchState.value = UiState.Success(batch)
                    _inputWeight.value = batch.inputWeightKg
                    recalculateEquation()
                }
                .onFailure { error ->
                    _batchState.value = UiState.Error(null, error.message ?: "Failed to load batch")
                }
        }
    }

    fun setOutputWeight(weight: String) {
        _outputWeight.value = weight
        _errorMessage.value = null
        recalculateEquation()
    }

    fun setWastageWeight(weight: String) {
        _wastageWeight.value = weight
        _errorMessage.value = null
        recalculateEquation()
    }

    fun setCompletionDate(date: String) {
        _completionDate.value = date
    }

    fun setNotes(notes: String) {
        _notes.value = notes
    }

    private fun recalculateEquation() {
        val input = _inputWeight.value
        val outStr = _outputWeight.value
        val wasteStr = _wastageWeight.value

        val output = outStr.toDoubleOrNull() ?: 0.0
        val waste = wasteStr.toDoubleOrNull() ?: 0.0
        val sum = output + waste
        val diff = input - sum

        val hasInputs = outStr.isNotBlank() && wasteStr.isNotBlank()
        val balanced = hasInputs && abs(diff) <= 0.005 && output > 0 && waste >= 0

        val msg = when {
            outStr.isBlank() -> "Enter output weight"
            wasteStr.isBlank() -> "Enter wastage weight"
            output <= 0 -> "Output weight must be greater than 0 kg"
            waste < 0 -> "Wastage weight cannot be negative"
            !balanced && diff > 0.005 -> "Output + Wastage is short by %.3f kg".format(diff)
            !balanced && diff < -0.005 -> "Output + Wastage exceeds Input by %.3f kg".format(-diff)
            else -> "Equation Balanced: %.3f kg = %.3f kg + %.3f kg".format(input, output, waste)
        }

        _equationState.value = EquationBalanceState(
            inputWeight = input,
            outputWeight = output,
            wastageWeight = waste,
            sumOutputWastage = sum,
            difference = diff,
            isBalanced = balanced,
            validationMessage = msg
        )
    }

    /**
     * Helper to auto-calculate wastage so that Input = Output + Wastage.
     */
    fun autoBalanceWastage() {
        val outVal = _outputWeight.value.toDoubleOrNull()
        val inVal = _inputWeight.value
        if (outVal != null && outVal > 0 && outVal <= inVal) {
            val autoWaste = inVal - outVal
            _wastageWeight.value = "%.3f".format(Locale.US, autoWaste)
            recalculateEquation()
        }
    }

    fun submitCompletion(onSuccess: () -> Unit) {
        val eq = _equationState.value
        if (!eq.isBalanced) {
            _errorMessage.value = eq.validationMessage
            return
        }

        viewModelScope.launch {
            _isSubmitting.value = true
            _errorMessage.value = null

            val request = CompleteProductionRequest(
                outputWeightKg = eq.outputWeight,
                wastageWeightKg = eq.wastageWeight,
                completionDate = _completionDate.value,
                notes = _notes.value.ifBlank { null }
            )

            repository.completeProduction(batchId, request)
                .onSuccess {
                    _isSubmitting.value = false
                    onSuccess()
                }
                .onFailure { error ->
                    _isSubmitting.value = false
                    _errorMessage.value = error.message ?: "Failed to complete production batch"
                }
        }
    }
}
