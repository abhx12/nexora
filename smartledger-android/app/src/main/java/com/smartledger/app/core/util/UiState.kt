package com.smartledger.app.core.util

/**
 * Sealed class representing the UI state of any data-loading operation.
 * Used universally across all ViewModels to represent Loading / Success / Error states.
 *
 * Usage:
 * ```
 * val uiState: StateFlow<UiState<List<PurchaseResponse>>> = _uiState.asStateFlow()
 * ```
 */
sealed class UiState<out T> {
    /** Data is being fetched from the API or local cache */
    data object Loading : UiState<Nothing>()

    /** Data fetched successfully */
    data class Success<T>(val data: T) : UiState<T>()

    /**
     * An error occurred. [code] is the backend error code (e.g. "INSUFFICIENT_INVENTORY"),
     * or null if the error is a network/IO error.
     */
    data class Error(val code: String?, val message: String) : UiState<Nothing>()
}
