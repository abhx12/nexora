package com.smartledger.app.feature.finance.domain

import com.smartledger.app.feature.finance.data.dto.AccountTransferRequest
import com.smartledger.app.feature.finance.data.dto.AccountTransferResponse
import com.smartledger.app.feature.finance.data.dto.CreateAccountRequest
import com.smartledger.app.feature.finance.data.dto.CreateExpenseRequest
import com.smartledger.app.feature.finance.data.dto.CreatePaymentRequest
import com.smartledger.app.feature.finance.data.dto.ExpenseResponse
import com.smartledger.app.feature.finance.data.dto.FinancialAccountResponse
import com.smartledger.app.feature.finance.data.dto.PaymentResponse

interface FinanceRepository {
    suspend fun getPayments(direction: String? = null): Result<List<PaymentResponse>>
    suspend fun getPaymentById(id: String): Result<PaymentResponse>
    suspend fun createPayment(request: CreatePaymentRequest): Result<PaymentResponse>

    suspend fun getExpenses(category: String? = null): Result<List<ExpenseResponse>>
    suspend fun createExpense(request: CreateExpenseRequest): Result<ExpenseResponse>

    suspend fun getAccounts(): Result<List<FinancialAccountResponse>>
    suspend fun createAccount(request: CreateAccountRequest): Result<FinancialAccountResponse>
    suspend fun transferFunds(request: AccountTransferRequest): Result<AccountTransferResponse>
}
