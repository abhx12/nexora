package com.smartledger.app.feature.finance.data.api

import com.smartledger.app.feature.finance.data.dto.AccountTransferRequest
import com.smartledger.app.feature.finance.data.dto.AccountTransferResponse
import com.smartledger.app.feature.finance.data.dto.CreateAccountRequest
import com.smartledger.app.feature.finance.data.dto.CreateExpenseRequest
import com.smartledger.app.feature.finance.data.dto.CreatePaymentRequest
import com.smartledger.app.feature.finance.data.dto.ExpenseResponse
import com.smartledger.app.feature.finance.data.dto.FinancialAccountResponse
import com.smartledger.app.feature.finance.data.dto.PaymentResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface FinanceApiService {

    @GET("api/v1/payments")
    suspend fun getPayments(
        @Query("direction") direction: String? = null
    ): List<PaymentResponse>

    @GET("api/v1/payments/{id}")
    suspend fun getPaymentById(
        @Path("id") id: String
    ): PaymentResponse

    @POST("api/v1/payments")
    suspend fun createPayment(
        @Body request: CreatePaymentRequest
    ): PaymentResponse

    @GET("api/v1/expenses")
    suspend fun getExpenses(
        @Query("category") category: String? = null
    ): List<ExpenseResponse>

    @POST("api/v1/expenses")
    suspend fun createExpense(
        @Body request: CreateExpenseRequest
    ): ExpenseResponse

    @GET("api/v1/finance/accounts")
    suspend fun getAccounts(): List<FinancialAccountResponse>

    @POST("api/v1/finance/accounts")
    suspend fun createAccount(
        @Body request: CreateAccountRequest
    ): FinancialAccountResponse

    @POST("api/v1/finance/transfers")
    suspend fun transferFunds(
        @Body request: AccountTransferRequest
    ): AccountTransferResponse
}
