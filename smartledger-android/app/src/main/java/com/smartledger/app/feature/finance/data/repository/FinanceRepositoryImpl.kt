package com.smartledger.app.feature.finance.data.repository

import com.google.gson.Gson
import com.smartledger.app.BuildConfig
import com.smartledger.app.core.util.ApiError
import com.smartledger.app.core.util.ApiErrorCode
import com.smartledger.app.feature.finance.data.api.FinanceApiService
import com.smartledger.app.feature.finance.data.dto.AccountTransferRequest
import com.smartledger.app.feature.finance.data.dto.AccountTransferResponse
import com.smartledger.app.feature.finance.data.dto.CreateAccountRequest
import com.smartledger.app.feature.finance.data.dto.CreateExpenseRequest
import com.smartledger.app.feature.finance.data.dto.CreatePaymentRequest
import com.smartledger.app.feature.finance.data.dto.ExpenseResponse
import com.smartledger.app.feature.finance.data.dto.FinancialAccountResponse
import com.smartledger.app.feature.finance.data.dto.PaymentAllocationResponse
import com.smartledger.app.feature.finance.data.dto.PaymentResponse
import com.smartledger.app.feature.finance.domain.FinanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FinanceRepositoryImpl @Inject constructor(
    private val apiService: FinanceApiService,
    private val gson: Gson
) : FinanceRepository {

    // Initial balances align directly with dashboard KPIs: cash=125,000, bank=850,000
    private val mockAccounts = mutableListOf(
        FinancialAccountResponse(
            id = "acc-cash",
            accountName = "Cash in Hand",
            accountType = "CASH",
            balance = 125000.00,
            accountNumber = null,
            bankName = null
        ),
        FinancialAccountResponse(
            id = "acc-bank-1",
            accountName = "HDFC Bank (Current)",
            accountType = "BANK",
            balance = 850000.00,
            accountNumber = "50200012345678",
            bankName = "HDFC Bank"
        )
    )

    private val mockPayments = mutableListOf(
        PaymentResponse(
            id = "pay-1",
            paymentNumber = "PAY-2026-0001",
            partyId = "cust-1",
            partyName = "Sri Lakshmi Silks",
            direction = "IN",
            amount = 150000.0,
            paymentMethod = "BANK_TRANSFER",
            financialAccountId = "acc-bank-1",
            accountName = "HDFC Bank (Current)",
            paymentDate = "2026-10-15",
            referenceNumber = "UTR987654321",
            notes = "Advance payment against Order ORD-2026-0001",
            allocations = listOf(
                PaymentAllocationResponse(
                    id = "alloc-1",
                    targetType = "ORDER",
                    targetNumber = "ORD-2026-0001",
                    amount = 150000.0
                )
            ),
            createdAt = "2026-10-15T10:30:00Z"
        ),
        PaymentResponse(
            id = "pay-2",
            paymentNumber = "PAY-2026-0002",
            partyId = "supp-1",
            partyName = "Karnataka Silk Cocoon Traders",
            direction = "OUT",
            amount = 80000.0,
            paymentMethod = "UPI",
            financialAccountId = "acc-bank-1",
            accountName = "HDFC Bank (Current)",
            paymentDate = "2026-10-16",
            referenceNumber = "UPI/2026/0091",
            notes = "Part payment for raw silk lot #88",
            allocations = listOf(
                PaymentAllocationResponse(
                    id = "alloc-2",
                    targetType = "PURCHASE",
                    targetNumber = "PUR-2026-0001",
                    amount = 80000.0
                )
            ),
            createdAt = "2026-10-16T14:15:00Z"
        )
    )

    private val mockExpenses = mutableListOf(
        ExpenseResponse(
            id = "exp-1",
            expenseNumber = "EXP-2026-0001",
            category = "ELECTRICITY",
            amount = 14500.0,
            financialAccountId = "acc-bank-1",
            accountName = "HDFC Bank (Current)",
            expenseDate = "2026-10-10",
            description = "EB monthly commercial power tariff for twisting mill",
            createdAt = "2026-10-10T12:00:00Z"
        ),
        ExpenseResponse(
            id = "exp-2",
            expenseNumber = "EXP-2026-0002",
            category = "TRANSPORT",
            amount = 3200.0,
            financialAccountId = "acc-cash",
            accountName = "Cash in Hand",
            expenseDate = "2026-10-12",
            description = "Tempo transport charges for cocoon lot delivery",
            createdAt = "2026-10-12T16:30:00Z"
        )
    )

    override suspend fun getPayments(direction: String?): Result<List<PaymentResponse>> =
        withContext(Dispatchers.IO) {
            if (BuildConfig.USE_MOCK) {
                val filtered = if (direction.isNullOrBlank() || direction.equals("ALL", ignoreCase = true)) {
                    mockPayments.toList()
                } else {
                    mockPayments.filter { it.direction.equals(direction, ignoreCase = true) }
                }
                return@withContext Result.success(filtered)
            }

            try {
                val result = apiService.getPayments(direction)
                Result.success(result)
            } catch (e: HttpException) {
                Result.failure(e)
            } catch (e: IOException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun getPaymentById(id: String): Result<PaymentResponse> =
        withContext(Dispatchers.IO) {
            if (BuildConfig.USE_MOCK) {
                val payment = mockPayments.find { it.id == id }
                return@withContext if (payment != null) {
                    Result.success(payment)
                } else {
                    Result.failure(NoSuchElementException("Payment not found"))
                }
            }

            try {
                val result = apiService.getPaymentById(id)
                Result.success(result)
            } catch (e: HttpException) {
                Result.failure(e)
            } catch (e: IOException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun createPayment(request: CreatePaymentRequest): Result<PaymentResponse> =
        withContext(Dispatchers.IO) {
            if (request.amount <= 0) {
                return@withContext Result.failure(IllegalArgumentException("Payment amount must be greater than 0"))
            }

            val totalAllocated = request.allocations.sumOf { it.amount }
            if (totalAllocated > request.amount) {
                val err = ApiError(
                    status = 409,
                    code = ApiErrorCode.PAYMENT_ALLOCATION_EXCEEDED,
                    message = "Allocation total (₹%.2f) exceeds payment amount (₹%.2f)".format(totalAllocated, request.amount)
                )
                return@withContext Result.failure(IllegalStateException(err.message))
            }

            if (BuildConfig.USE_MOCK) {
                val accountIndex = mockAccounts.indexOfFirst { it.id == request.financialAccountId }
                val account = if (accountIndex != -1) mockAccounts[accountIndex] else mockAccounts.first()

                // Check balance if paying OUT
                if (request.direction == "OUT" && account.balance < request.amount) {
                    return@withContext Result.failure(
                        IllegalStateException("Insufficient funds in ${account.accountName}. Available: ₹%.2f".format(account.balance))
                    )
                }

                // Update account balance
                val newBalance = if (request.direction == "IN") {
                    account.balance + request.amount
                } else {
                    account.balance - request.amount
                }
                if (accountIndex != -1) {
                    mockAccounts[accountIndex] = account.copy(balance = newBalance)
                }

                val nextNum = mockPayments.size + 1
                val paymentNumber = "PAY-2026-%04d".format(nextNum)

                val allocations = request.allocations.mapIndexed { index, alloc ->
                    PaymentAllocationResponse(
                        id = UUID.randomUUID().toString(),
                        targetType = if (alloc.orderItemId != null) "ORDER" else "PURCHASE",
                        targetNumber = alloc.orderItemId ?: alloc.purchaseId ?: "REF-${index + 1}",
                        amount = alloc.amount
                    )
                }

                val newPayment = PaymentResponse(
                    id = UUID.randomUUID().toString(),
                    paymentNumber = paymentNumber,
                    partyId = request.partyId,
                    partyName = "Party ${request.partyId}",
                    direction = request.direction,
                    amount = request.amount,
                    paymentMethod = request.paymentMethod,
                    financialAccountId = account.id,
                    accountName = account.accountName,
                    paymentDate = request.paymentDate,
                    referenceNumber = request.referenceNumber,
                    notes = request.notes,
                    allocations = allocations,
                    createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
                )

                mockPayments.add(0, newPayment)
                return@withContext Result.success(newPayment)
            }

            try {
                val result = apiService.createPayment(request)
                Result.success(result)
            } catch (e: HttpException) {
                Result.failure(e)
            } catch (e: IOException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun getExpenses(category: String?): Result<List<ExpenseResponse>> =
        withContext(Dispatchers.IO) {
            if (BuildConfig.USE_MOCK) {
                val filtered = if (category.isNullOrBlank() || category.equals("ALL", ignoreCase = true)) {
                    mockExpenses.toList()
                } else {
                    mockExpenses.filter { it.category.equals(category, ignoreCase = true) }
                }
                return@withContext Result.success(filtered)
            }

            try {
                val result = apiService.getExpenses(category)
                Result.success(result)
            } catch (e: HttpException) {
                Result.failure(e)
            } catch (e: IOException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun createExpense(request: CreateExpenseRequest): Result<ExpenseResponse> =
        withContext(Dispatchers.IO) {
            if (request.amount <= 0) {
                return@withContext Result.failure(IllegalArgumentException("Expense amount must be greater than 0"))
            }

            if (BuildConfig.USE_MOCK) {
                val accountIndex = mockAccounts.indexOfFirst { it.id == request.financialAccountId }
                val account = if (accountIndex != -1) mockAccounts[accountIndex] else mockAccounts.first()

                if (account.balance < request.amount) {
                    return@withContext Result.failure(
                        IllegalStateException("Insufficient account balance. Available: ₹%.2f".format(account.balance))
                    )
                }

                if (accountIndex != -1) {
                    mockAccounts[accountIndex] = account.copy(balance = account.balance - request.amount)
                }

                val nextNum = mockExpenses.size + 1
                val expenseNumber = "EXP-2026-%04d".format(nextNum)

                val newExpense = ExpenseResponse(
                    id = UUID.randomUUID().toString(),
                    expenseNumber = expenseNumber,
                    category = request.category,
                    amount = request.amount,
                    financialAccountId = account.id,
                    accountName = account.accountName,
                    expenseDate = request.expenseDate,
                    description = request.description,
                    createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
                )
                mockExpenses.add(0, newExpense)
                return@withContext Result.success(newExpense)
            }

            try {
                val result = apiService.createExpense(request)
                Result.success(result)
            } catch (e: HttpException) {
                Result.failure(e)
            } catch (e: IOException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun getAccounts(): Result<List<FinancialAccountResponse>> =
        withContext(Dispatchers.IO) {
            if (BuildConfig.USE_MOCK) {
                return@withContext Result.success(mockAccounts.toList())
            }

            try {
                val result = apiService.getAccounts()
                Result.success(result)
            } catch (e: HttpException) {
                Result.failure(e)
            } catch (e: IOException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun createAccount(request: CreateAccountRequest): Result<FinancialAccountResponse> =
        withContext(Dispatchers.IO) {
            if (BuildConfig.USE_MOCK) {
                val newAccount = FinancialAccountResponse(
                    id = UUID.randomUUID().toString(),
                    accountName = request.accountName,
                    accountType = request.accountType,
                    balance = request.initialBalance,
                    accountNumber = request.accountNumber,
                    bankName = request.bankName
                )
                mockAccounts.add(newAccount)
                return@withContext Result.success(newAccount)
            }

            try {
                val result = apiService.createAccount(request)
                Result.success(result)
            } catch (e: HttpException) {
                Result.failure(e)
            } catch (e: IOException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun transferFunds(request: AccountTransferRequest): Result<AccountTransferResponse> =
        withContext(Dispatchers.IO) {
            if (request.fromAccountId == request.toAccountId) {
                return@withContext Result.failure(
                    IllegalArgumentException("Source and destination accounts must be different")
                )
            }
            if (request.amount <= 0) {
                return@withContext Result.failure(IllegalArgumentException("Transfer amount must be greater than 0"))
            }

            if (BuildConfig.USE_MOCK) {
                val fromIndex = mockAccounts.indexOfFirst { it.id == request.fromAccountId }
                val toIndex = mockAccounts.indexOfFirst { it.id == request.toAccountId }

                if (fromIndex == -1 || toIndex == -1) {
                    return@withContext Result.failure(NoSuchElementException("One or both accounts not found"))
                }

                val fromAcc = mockAccounts[fromIndex]
                val toAcc = mockAccounts[toIndex]

                if (fromAcc.balance < request.amount) {
                    return@withContext Result.failure(
                        IllegalStateException("Insufficient funds in ${fromAcc.accountName}. Available: ₹%.2f".format(fromAcc.balance))
                    )
                }

                mockAccounts[fromIndex] = fromAcc.copy(balance = fromAcc.balance - request.amount)
                mockAccounts[toIndex] = toAcc.copy(balance = toAcc.balance + request.amount)

                val response = AccountTransferResponse(
                    id = UUID.randomUUID().toString(),
                    transferNumber = "TRF-2026-%04d".format((1..9999).random()),
                    fromAccountId = fromAcc.id,
                    fromAccountName = fromAcc.accountName,
                    toAccountId = toAcc.id,
                    toAccountName = toAcc.accountName,
                    amount = request.amount,
                    transferDate = request.transferDate,
                    notes = request.notes,
                    createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
                )
                return@withContext Result.success(response)
            }

            try {
                val result = apiService.transferFunds(request)
                Result.success(result)
            } catch (e: HttpException) {
                Result.failure(e)
            } catch (e: IOException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
