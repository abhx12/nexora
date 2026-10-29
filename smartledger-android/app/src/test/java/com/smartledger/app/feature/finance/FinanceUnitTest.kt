package com.smartledger.app.feature.finance

import com.google.gson.Gson
import com.smartledger.app.core.util.UiState
import com.smartledger.app.feature.finance.data.api.FinanceApiService
import com.smartledger.app.feature.finance.data.dto.AccountTransferRequest
import com.smartledger.app.feature.finance.data.dto.AccountTransferResponse
import com.smartledger.app.feature.finance.data.dto.CreateAccountRequest
import com.smartledger.app.feature.finance.data.dto.CreateExpenseRequest
import com.smartledger.app.feature.finance.data.dto.CreatePaymentRequest
import com.smartledger.app.feature.finance.data.dto.ExpenseResponse
import com.smartledger.app.feature.finance.data.dto.FinancialAccountResponse
import com.smartledger.app.feature.finance.data.dto.PaymentAllocationRequest
import com.smartledger.app.feature.finance.data.dto.PaymentResponse
import com.smartledger.app.feature.finance.data.repository.FinanceRepositoryImpl
import com.smartledger.app.feature.finance.domain.FinanceRepository
import com.smartledger.app.feature.finance.presentation.viewmodel.AccountListViewModel
import com.smartledger.app.feature.finance.presentation.viewmodel.AccountTransferViewModel
import com.smartledger.app.feature.finance.presentation.viewmodel.FinanceViewModel
import com.smartledger.app.feature.finance.presentation.viewmodel.RecordExpenseViewModel
import com.smartledger.app.feature.finance.presentation.viewmodel.RecordPaymentViewModel
import com.smartledger.app.feature.parties.data.dto.CreatePartyRequest
import com.smartledger.app.feature.parties.data.dto.PartyResponse
import com.smartledger.app.feature.parties.data.dto.UpdatePartyRequest
import com.smartledger.app.feature.parties.domain.PartiesRepository
import com.smartledger.app.feature.parties.domain.model.PartyType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class FakeFinanceApiService : FinanceApiService {
    val payments = mutableListOf<PaymentResponse>()
    val expenses = mutableListOf<ExpenseResponse>()
    val accounts = mutableListOf<FinancialAccountResponse>()

    override suspend fun getPayments(direction: String?): List<PaymentResponse> = payments
    override suspend fun getPaymentById(id: String): PaymentResponse = payments.first { it.id == id }
    override suspend fun createPayment(request: CreatePaymentRequest): PaymentResponse {
        val p = PaymentResponse(
            id = UUID.randomUUID().toString(),
            paymentNumber = "PAY-2026-9999",
            partyId = request.partyId,
            partyName = "Party",
            direction = request.direction,
            amount = request.amount,
            paymentMethod = request.paymentMethod,
            financialAccountId = request.financialAccountId,
            paymentDate = request.paymentDate
        )
        payments.add(0, p)
        return p
    }

    override suspend fun getExpenses(category: String?): List<ExpenseResponse> = expenses
    override suspend fun createExpense(request: CreateExpenseRequest): ExpenseResponse {
        val e = ExpenseResponse(
            id = UUID.randomUUID().toString(),
            expenseNumber = "EXP-2026-9999",
            category = request.category,
            amount = request.amount,
            financialAccountId = request.financialAccountId,
            expenseDate = request.expenseDate,
            description = request.description
        )
        expenses.add(0, e)
        return e
    }

    override suspend fun getAccounts(): List<FinancialAccountResponse> = accounts
    override suspend fun createAccount(request: CreateAccountRequest): FinancialAccountResponse {
        val acc = FinancialAccountResponse(UUID.randomUUID().toString(), request.accountName, request.accountType, request.initialBalance)
        accounts.add(acc)
        return acc
    }

    override suspend fun transferFunds(request: AccountTransferRequest): AccountTransferResponse =
        AccountTransferResponse("trf-1", "TRF-2026-0001", request.fromAccountId, "From", request.toAccountId, "To", request.amount, request.transferDate)
}

class FakeFinancePartiesRepo : PartiesRepository {
    override suspend fun getParties(type: PartyType?, search: String?): Result<List<PartyResponse>> =
        Result.success(listOf(PartyResponse("cust-1", "CUST-001", "Sri Lakshmi Silks", "CUSTOMER", null, null, "ACTIVE")))
    override suspend fun getPartyById(id: String): Result<PartyResponse> =
        Result.success(PartyResponse("cust-1", "CUST-001", "Sri Lakshmi Silks", "CUSTOMER", null, null, "ACTIVE"))
    override suspend fun createParty(request: CreatePartyRequest): Result<PartyResponse> =
        Result.success(PartyResponse("c-1", "C-1", request.name, request.partyType, null, null, "ACTIVE"))
    override suspend fun updateParty(id: String, request: UpdatePartyRequest): Result<PartyResponse> =
        Result.success(PartyResponse(id, "C-1", request.name ?: "", "CUSTOMER", null, null, "ACTIVE"))
}

class FakeFinanceRepository : FinanceRepository {
    val accounts = mutableListOf(
        FinancialAccountResponse("acc-cash", "Cash in Hand", "CASH", 125000.0),
        FinancialAccountResponse("acc-bank-1", "HDFC Bank", "BANK", 850000.0)
    )

    val payments = mutableListOf(
        PaymentResponse("pay-1", "PAY-2026-0001", "cust-1", "Sri Lakshmi Silks", "IN", 150000.0, "BANK_TRANSFER", "acc-bank-1", "HDFC Bank", "2026-10-15")
    )

    val expenses = mutableListOf(
        ExpenseResponse("exp-1", "EXP-2026-0001", "ELECTRICITY", 14500.0, "acc-bank-1", "HDFC Bank", "2026-10-10", "EB Bill")
    )

    override suspend fun getPayments(direction: String?): Result<List<PaymentResponse>> = Result.success(payments)
    override suspend fun getPaymentById(id: String): Result<PaymentResponse> = Result.success(payments.first { it.id == id })
    override suspend fun createPayment(request: CreatePaymentRequest): Result<PaymentResponse> {
        val p = PaymentResponse("pay-new", "PAY-2026-9999", request.partyId, "Party", request.direction, request.amount, request.paymentMethod, request.financialAccountId, "Account", request.paymentDate)
        payments.add(0, p)
        return Result.success(p)
    }

    override suspend fun getExpenses(category: String?): Result<List<ExpenseResponse>> = Result.success(expenses)
    override suspend fun createExpense(request: CreateExpenseRequest): Result<ExpenseResponse> {
        val e = ExpenseResponse("exp-new", "EXP-2026-9999", request.category, request.amount, request.financialAccountId, "Account", request.expenseDate, request.description)
        expenses.add(0, e)
        return Result.success(e)
    }

    override suspend fun getAccounts(): Result<List<FinancialAccountResponse>> = Result.success(accounts)
    override suspend fun createAccount(request: CreateAccountRequest): Result<FinancialAccountResponse> {
        val acc = FinancialAccountResponse("acc-new", request.accountName, request.accountType, request.initialBalance)
        accounts.add(acc)
        return Result.success(acc)
    }

    override suspend fun transferFunds(request: AccountTransferRequest): Result<AccountTransferResponse> {
        return Result.success(AccountTransferResponse("trf-1", "TRF-2026-0001", request.fromAccountId, "From", request.toAccountId, "To", request.amount, request.transferDate))
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class FinanceUnitTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var apiService: FakeFinanceApiService
    private lateinit var repository: FinanceRepositoryImpl

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        apiService = FakeFinanceApiService()
        repository = FinanceRepositoryImpl(apiService, Gson())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testGetAccounts() = runTest {
        val result = repository.getAccounts()
        assertTrue(result.isSuccess)
        val accounts = result.getOrThrow()
        assertTrue(accounts.any { it.accountType == "CASH" })
        assertTrue(accounts.any { it.accountType == "BANK" })
    }

    @Test
    fun testCreatePayment_DirectionInIncreasesBalance() = runTest {
        val initialAccounts = repository.getAccounts().getOrThrow()
        val cashAccount = initialAccounts.first { it.id == "acc-cash" }
        val prevBalance = cashAccount.balance

        val request = CreatePaymentRequest(
            partyId = "cust-1",
            direction = "IN",
            amount = 25000.0,
            paymentMethod = "CASH",
            financialAccountId = "acc-cash",
            paymentDate = "2026-10-21"
        )
        val result = repository.createPayment(request)
        assertTrue(result.isSuccess)

        val updatedAccounts = repository.getAccounts().getOrThrow()
        val updatedCash = updatedAccounts.first { it.id == "acc-cash" }
        assertEquals(prevBalance + 25000.0, updatedCash.balance, 0.001)
    }

    @Test
    fun testCreatePayment_AllocationExceededFails() = runTest {
        val request = CreatePaymentRequest(
            partyId = "cust-1",
            direction = "IN",
            amount = 10000.0,
            paymentMethod = "UPI",
            financialAccountId = "acc-cash",
            paymentDate = "2026-10-21",
            allocations = listOf(PaymentAllocationRequest(orderItemId = "ord-1", amount = 15000.0))
        )
        val result = repository.createPayment(request)
        assertTrue(result.isFailure)
    }

    @Test
    fun testCreateExpense_DeductsFromAccount() = runTest {
        val prevBank = repository.getAccounts().getOrThrow().first { it.id == "acc-bank-1" }.balance

        val request = CreateExpenseRequest(
            category = "MAINTENANCE",
            amount = 5000.0,
            financialAccountId = "acc-bank-1",
            expenseDate = "2026-10-21",
            description = "Loom spindle replacement"
        )
        val result = repository.createExpense(request)
        assertTrue(result.isSuccess)

        val updatedBank = repository.getAccounts().getOrThrow().first { it.id == "acc-bank-1" }.balance
        assertEquals(prevBank - 5000.0, updatedBank, 0.001)
    }

    @Test
    fun testTransferFunds_SameAccountFails() = runTest {
        val request = AccountTransferRequest(
            fromAccountId = "acc-cash",
            toAccountId = "acc-cash",
            amount = 1000.0,
            transferDate = "2026-10-21"
        )
        val result = repository.transferFunds(request)
        assertTrue(result.isFailure)
    }

    @Test
    fun testTransferFunds_MovesFundsCorrectly() = runTest {
        val prevCash = repository.getAccounts().getOrThrow().first { it.id == "acc-cash" }.balance
        val prevBank = repository.getAccounts().getOrThrow().first { it.id == "acc-bank-1" }.balance

        val request = AccountTransferRequest(
            fromAccountId = "acc-cash",
            toAccountId = "acc-bank-1",
            amount = 10000.0,
            transferDate = "2026-10-21"
        )
        val result = repository.transferFunds(request)
        assertTrue(result.isSuccess)

        val newCash = repository.getAccounts().getOrThrow().first { it.id == "acc-cash" }.balance
        val newBank = repository.getAccounts().getOrThrow().first { it.id == "acc-bank-1" }.balance

        assertEquals(prevCash - 10000.0, newCash, 0.001)
        assertEquals(prevBank + 10000.0, newBank, 0.001)
    }

    @Test
    fun testFinanceViewModel() = runTest {
        val fakeRepo = FakeFinanceRepository()
        val viewModel = FinanceViewModel(fakeRepo)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        val overview = (state as UiState.Success).data
        assertTrue(overview.totalLiquid > 0)
    }

    @Test
    fun testRecordPaymentViewModel() = runTest {
        val fakeRepo = FakeFinanceRepository()
        val partiesRepo = FakeFinancePartiesRepo()
        val viewModel = RecordPaymentViewModel(fakeRepo, partiesRepo)
        advanceUntilIdle()

        viewModel.recordPayment(
            partyId = "cust-1",
            direction = "IN",
            amount = 50000.0,
            paymentMethod = "BANK_TRANSFER",
            financialAccountId = "acc-bank-1",
            paymentDate = "2026-10-21",
            referenceNumber = "UTR123",
            notes = "Test",
            allocations = emptyList()
        )
        advanceUntilIdle()

        val state = viewModel.submitState.value
        assertTrue(state is UiState.Success)
    }

    @Test
    fun testRecordExpenseViewModel() = runTest {
        val fakeRepo = FakeFinanceRepository()
        val viewModel = RecordExpenseViewModel(fakeRepo)
        advanceUntilIdle()

        viewModel.recordExpense(
            category = "TRANSPORT",
            amount = 1200.0,
            financialAccountId = "acc-cash",
            expenseDate = "2026-10-21",
            description = "Freight"
        )
        advanceUntilIdle()

        val state = viewModel.submitState.value
        assertTrue(state is UiState.Success)
    }

    @Test
    fun testAccountTransferViewModel() = runTest {
        val fakeRepo = FakeFinanceRepository()
        val viewModel = AccountTransferViewModel(fakeRepo)
        advanceUntilIdle()

        viewModel.transferFunds(
            fromAccountId = "acc-cash",
            toAccountId = "acc-bank-1",
            amount = 5000.0,
            transferDate = "2026-10-21",
            notes = "Cash deposit"
        )
        advanceUntilIdle()

        val state = viewModel.submitState.value
        assertTrue(state is UiState.Success)
    }
}
