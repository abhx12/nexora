package com.smartledger.app.feature.procurement

import com.google.gson.Gson
import com.smartledger.app.core.ui.theme.StatusCancelledRed
import com.smartledger.app.core.ui.theme.StatusCompletedGreen
import com.smartledger.app.core.ui.theme.StatusPartialBlue
import com.smartledger.app.core.ui.theme.StatusPendingOrange
import com.smartledger.app.core.util.UiState
import com.smartledger.app.feature.parties.data.dto.CreatePartyRequest
import com.smartledger.app.feature.parties.data.dto.PartyResponse
import com.smartledger.app.feature.parties.data.dto.UpdatePartyRequest
import com.smartledger.app.feature.parties.domain.PartiesRepository
import com.smartledger.app.feature.parties.domain.model.PartyType
import com.smartledger.app.feature.procurement.data.api.PurchaseApiService
import com.smartledger.app.feature.procurement.data.dto.CreatePurchaseRequest
import com.smartledger.app.feature.procurement.data.dto.MaterialReceiptResponse
import com.smartledger.app.feature.procurement.data.dto.PurchaseResponse
import com.smartledger.app.feature.procurement.data.dto.RecordReceiptRequest
import com.smartledger.app.feature.procurement.data.repository.PurchaseRepositoryImpl
import com.smartledger.app.feature.procurement.domain.PurchaseRepository
import com.smartledger.app.feature.procurement.domain.model.PurchaseStatus
import com.smartledger.app.feature.procurement.presentation.viewmodel.CreatePurchaseViewModel
import com.smartledger.app.feature.procurement.presentation.viewmodel.PurchaseDetailViewModel
import com.smartledger.app.feature.procurement.presentation.viewmodel.PurchaseListViewModel
import com.smartledger.app.feature.procurement.presentation.viewmodel.RecordReceiptViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

/**
 * Fake PurchaseApiService for testing PurchaseRepositoryImpl.
 */
class FakePurchaseApiService : PurchaseApiService {
    var shouldFail: Boolean = false
    var failureCode: Int = 400
    var failureMessage: String = "Bad request"

    val samplePurchases = mutableListOf(
        PurchaseResponse(
            id = "pur-1",
            purchaseNumber = "PUR-2026-0001",
            supplierId = "supp-1",
            supplierName = "Karnataka Silk Filatures Ltd",
            purchaseDate = "2026-10-10",
            orderedWeightKg = 250.000,
            ratePerKg = 4250.00,
            paymentType = "CREDIT",
            creditDays = 30,
            status = "PARTIALLY_RECEIVED",
            receivedWeightKg = 100.000,
            notes = "Grade A CSR raw silk cocoons",
            receipts = listOf(
                MaterialReceiptResponse(
                    id = "rec-1",
                    purchaseId = "pur-1",
                    receiptNumber = "REC-001",
                    receiptDate = "2026-10-12",
                    receivedWeightKg = 101.500,
                    acceptedWeightKg = 100.000,
                    rejectedWeightKg = 1.500,
                    notes = "Moisture dampness in rejected fraction"
                )
            )
        ),
        PurchaseResponse(
            id = "pur-2",
            purchaseNumber = "PUR-2026-0002",
            supplierId = "supp-2",
            supplierName = "Venkateshwara Silk Reelers",
            purchaseDate = "2026-10-15",
            orderedWeightKg = 150.000,
            ratePerKg = 4300.00,
            paymentType = "CASH",
            creditDays = null,
            status = "PENDING_RECEIPT",
            receivedWeightKg = 0.0,
            notes = "Express consignment",
            receipts = emptyList()
        )
    )

    override suspend fun getPurchases(supplierId: String?, status: String?): Response<List<PurchaseResponse>> {
        if (shouldFail) {
            val errorJson = """{"status":$failureCode,"message":"$failureMessage"}"""
            return Response.error(failureCode, errorJson.toResponseBody("application/json".toMediaTypeOrNull()))
        }
        var list = samplePurchases.toList()
        if (supplierId != null) list = list.filter { it.supplierId == supplierId }
        if (status != null) list = list.filter { it.status.equals(status, ignoreCase = true) }
        return Response.success(list)
    }

    override suspend fun getPurchaseById(id: String): Response<PurchaseResponse> {
        if (shouldFail) {
            return Response.error(failureCode, "Error".toResponseBody("text/plain".toMediaTypeOrNull()))
        }
        val found = samplePurchases.firstOrNull { it.id == id || it.purchaseNumber.equals(id, ignoreCase = true) }
        return if (found != null) {
            Response.success(found)
        } else {
            Response.error(404, "Not found".toResponseBody("text/plain".toMediaTypeOrNull()))
        }
    }

    override suspend fun createPurchase(request: CreatePurchaseRequest): Response<PurchaseResponse> {
        if (shouldFail) {
            val errorJson = """{"status":$failureCode,"message":"$failureMessage"}"""
            return Response.error(failureCode, errorJson.toResponseBody("application/json".toMediaTypeOrNull()))
        }
        val created = PurchaseResponse(
            id = "pur-${samplePurchases.size + 1}",
            purchaseNumber = "PUR-2026-%04d".format(samplePurchases.size + 1),
            supplierId = request.supplierId,
            supplierName = "Supplier ${request.supplierId}",
            purchaseDate = request.purchaseDate,
            orderedWeightKg = request.orderedWeightKg,
            ratePerKg = request.ratePerKg,
            paymentType = request.paymentType,
            creditDays = request.creditDays,
            status = "PENDING_RECEIPT",
            receivedWeightKg = 0.0,
            notes = request.notes,
            receipts = emptyList()
        )
        samplePurchases.add(0, created)
        return Response.success(created)
    }

    override suspend fun recordReceipt(
        id: String,
        request: RecordReceiptRequest
    ): Response<MaterialReceiptResponse> {
        if (shouldFail) {
            val errorJson = """{"status":$failureCode,"message":"$failureMessage"}"""
            return Response.error(failureCode, errorJson.toResponseBody("application/json".toMediaTypeOrNull()))
        }
        val receipt = MaterialReceiptResponse(
            id = "rec-${(1..999).random()}",
            purchaseId = id,
            receiptNumber = "REC-999",
            receiptDate = request.receiptDate,
            receivedWeightKg = request.receivedWeightKg,
            acceptedWeightKg = request.acceptedWeightKg,
            rejectedWeightKg = request.rejectedWeightKg,
            notes = request.notes
        )
        val idx = samplePurchases.indexOfFirst { it.id == id }
        if (idx != -1) {
            val curr = samplePurchases[idx]
            val newReceived = curr.receivedWeightKg + receipt.acceptedWeightKg
            samplePurchases[idx] = curr.copy(
                receivedWeightKg = newReceived,
                status = if (newReceived >= curr.orderedWeightKg) "FULLY_RECEIVED" else "PARTIALLY_RECEIVED",
                receipts = curr.receipts + receipt
            )
        }
        return Response.success(receipt)
    }
}

/**
 * Fake PartiesRepository to support procurement supplier selection.
 */
class FakeProcurementPartiesRepository : PartiesRepository {
    val suppliers = mutableListOf(
        PartyResponse(
            id = "supp-1",
            partyCode = "SUPP-001",
            name = "Karnataka Silk Filatures Ltd",
            partyType = "SUPPLIER",
            phone = "9845011111",
            address = "Ramanagara Silk Market",
            status = "ACTIVE"
        ),
        PartyResponse(
            id = "supp-2",
            partyCode = "SUPP-002",
            name = "Venkateshwara Silk Reelers",
            partyType = "SUPPLIER",
            phone = "9845022222",
            address = "Sidlaghatta Cocoon Hub",
            status = "ACTIVE"
        )
    )

    override suspend fun getParties(type: PartyType?, search: String?): Result<List<PartyResponse>> {
        var list = suppliers.toList()
        if (type != null) list = list.filter { it.partyType == type.name }
        if (!search.isNullOrBlank()) list = list.filter { it.name.contains(search, true) }
        return Result.success(list)
    }

    override suspend fun getPartyById(id: String): Result<PartyResponse> {
        val found = suppliers.firstOrNull { it.id == id }
        return if (found != null) Result.success(found) else Result.failure(Exception("Supplier not found"))
    }

    override suspend fun createParty(request: CreatePartyRequest): Result<PartyResponse> {
        val created = PartyResponse(
            id = "supp-${suppliers.size + 1}",
            partyCode = "SUPP-00${suppliers.size + 1}",
            name = request.name,
            partyType = request.partyType,
            phone = request.phone,
            address = request.address,
            status = "ACTIVE"
        )
        suppliers.add(created)
        return Result.success(created)
    }

    override suspend fun updateParty(id: String, request: UpdatePartyRequest): Result<PartyResponse> {
        val idx = suppliers.indexOfFirst { it.id == id }
        return if (idx != -1) {
            val updated = suppliers[idx].copy(status = request.status ?: suppliers[idx].status)
            suppliers[idx] = updated
            Result.success(updated)
        } else {
            Result.failure(Exception("Not found"))
        }
    }
}

/**
 * Fake PurchaseRepository for isolated ViewModel tests.
 */
class FakePurchaseRepository : PurchaseRepository {
    var shouldFail: Boolean = false
    var failureMessage: String = "Repository error"

    val purchases = mutableListOf(
        PurchaseResponse(
            id = "pur-1",
            purchaseNumber = "PUR-2026-0001",
            supplierId = "supp-1",
            supplierName = "Karnataka Silk Filatures Ltd",
            purchaseDate = "2026-10-10",
            orderedWeightKg = 250.000,
            ratePerKg = 4250.00,
            paymentType = "CREDIT",
            creditDays = 30,
            status = "PARTIALLY_RECEIVED",
            receivedWeightKg = 100.000,
            notes = "Grade A CSR raw silk cocoons",
            receipts = listOf(
                MaterialReceiptResponse(
                    id = "rec-1",
                    purchaseId = "pur-1",
                    receiptNumber = "REC-001",
                    receiptDate = "2026-10-12",
                    receivedWeightKg = 101.500,
                    acceptedWeightKg = 100.000,
                    rejectedWeightKg = 1.500,
                    notes = "Moisture deduction"
                )
            )
        )
    )

    override suspend fun getPurchases(
        supplierId: String?,
        status: PurchaseStatus?
    ): Result<List<PurchaseResponse>> {
        return if (shouldFail) {
            Result.failure(Exception(failureMessage))
        } else {
            var list = purchases.toList()
            if (supplierId != null) list = list.filter { it.supplierId == supplierId }
            if (status != null) list = list.filter { it.status.equals(status.name, ignoreCase = true) }
            Result.success(list)
        }
    }

    override suspend fun getPurchaseById(id: String): Result<PurchaseResponse> {
        val found = purchases.firstOrNull { it.id == id }
        return if (found != null && !shouldFail) {
            Result.success(found)
        } else {
            Result.failure(Exception(if (shouldFail) failureMessage else "Purchase not found"))
        }
    }

    override suspend fun createPurchase(request: CreatePurchaseRequest): Result<PurchaseResponse> {
        if (shouldFail) return Result.failure(Exception(failureMessage))
        if (request.orderedWeightKg <= 0) return Result.failure(IllegalArgumentException("Ordered weight must be greater than 0"))
        if (request.ratePerKg <= 0) return Result.failure(IllegalArgumentException("Rate per kg must be greater than 0"))
        if (request.paymentType.equals("CREDIT", true) && (request.creditDays == null || request.creditDays < 0)) {
            return Result.failure(IllegalArgumentException("Credit days must be >= 0 for credit purchases"))
        }

        val created = PurchaseResponse(
            id = "pur-new",
            purchaseNumber = "PUR-2026-0005",
            supplierId = request.supplierId,
            supplierName = "Mock Supplier",
            purchaseDate = request.purchaseDate,
            orderedWeightKg = request.orderedWeightKg,
            ratePerKg = request.ratePerKg,
            paymentType = request.paymentType,
            creditDays = request.creditDays,
            status = "PENDING_RECEIPT",
            receivedWeightKg = 0.0,
            notes = request.notes,
            receipts = emptyList()
        )
        purchases.add(0, created)
        return Result.success(created)
    }

    override suspend fun recordReceipt(
        purchaseId: String,
        request: RecordReceiptRequest
    ): Result<MaterialReceiptResponse> {
        if (shouldFail) return Result.failure(Exception(failureMessage))
        if (request.receivedWeightKg <= 0) return Result.failure(IllegalArgumentException("Received weight must be greater than 0"))
        if (request.acceptedWeightKg + request.rejectedWeightKg > request.receivedWeightKg + 0.001) {
            return Result.failure(IllegalArgumentException("Accepted weight + rejected weight cannot exceed received weight"))
        }

        val receipt = MaterialReceiptResponse(
            id = "rec-new",
            purchaseId = purchaseId,
            receiptNumber = "REC-100",
            receiptDate = request.receiptDate,
            receivedWeightKg = request.receivedWeightKg,
            acceptedWeightKg = request.acceptedWeightKg,
            rejectedWeightKg = request.rejectedWeightKg,
            notes = request.notes
        )
        val idx = purchases.indexOfFirst { it.id == purchaseId }
        if (idx != -1) {
            val current = purchases[idx]
            val newReceived = current.receivedWeightKg + receipt.acceptedWeightKg
            purchases[idx] = current.copy(
                receivedWeightKg = newReceived,
                status = if (newReceived >= current.orderedWeightKg - 0.001) "FULLY_RECEIVED" else "PARTIALLY_RECEIVED",
                receipts = current.receipts + receipt
            )
        }
        return Result.success(receipt)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ProcurementUnitTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Status Color Mapping Tests ──────────────────────────────────────────

    @Test
    fun purchaseStatus_colorMappingMatchesPlan() {
        assertEquals(StatusPendingOrange, PurchaseStatus.PENDING_RECEIPT.color)
        assertEquals(StatusPartialBlue, PurchaseStatus.PARTIALLY_RECEIVED.color)
        assertEquals(StatusCompletedGreen, PurchaseStatus.FULLY_RECEIVED.color)
        assertEquals(StatusCancelledRed, PurchaseStatus.CANCELLED.color)
    }

    // ── Repository Tests ────────────────────────────────────────────────────

    @Test
    fun repository_createPurchase_success() = runTest {
        val fakeApi = FakePurchaseApiService()
        val fakeParties = FakeProcurementPartiesRepository()
        val repo = PurchaseRepositoryImpl(fakeApi, fakeParties, Gson())

        val request = CreatePurchaseRequest(
            supplierId = "supp-1",
            purchaseDate = "2026-10-20",
            orderedWeightKg = 120.500,
            ratePerKg = 4400.0,
            paymentType = "CREDIT",
            creditDays = 45,
            notes = "Twisted warp yarn order"
        )
        val result = repo.createPurchase(request)

        assertTrue(result.isSuccess)
        val created = result.getOrNull()!!
        assertEquals("supp-1", created.supplierId)
        assertEquals(120.500, created.orderedWeightKg, 0.0001)
        assertEquals(4400.0, created.ratePerKg, 0.0001)
        assertEquals(530200.0, created.totalAmount, 0.0001)
        assertEquals("PENDING_RECEIPT", created.status)
    }

    @Test
    fun repository_createPurchase_weightValidation() = runTest {
        val fakeApi = FakePurchaseApiService()
        val fakeParties = FakeProcurementPartiesRepository()
        val repo = PurchaseRepositoryImpl(fakeApi, fakeParties, Gson())

        val request = CreatePurchaseRequest(
            supplierId = "supp-1",
            purchaseDate = "2026-10-20",
            orderedWeightKg = 0.0, // Invalid!
            ratePerKg = 4000.0
        )
        val result = repo.createPurchase(request)

        assertTrue(result.isFailure)
        assertEquals("Ordered weight must be greater than 0", result.exceptionOrNull()?.message)
    }

    @Test
    fun repository_createPurchase_rateValidation() = runTest {
        val fakeApi = FakePurchaseApiService()
        val fakeParties = FakeProcurementPartiesRepository()
        val repo = PurchaseRepositoryImpl(fakeApi, fakeParties, Gson())

        val request = CreatePurchaseRequest(
            supplierId = "supp-1",
            purchaseDate = "2026-10-20",
            orderedWeightKg = 50.0,
            ratePerKg = -10.0 // Invalid!
        )
        val result = repo.createPurchase(request)

        assertTrue(result.isFailure)
        assertEquals("Rate per kg must be greater than 0", result.exceptionOrNull()?.message)
    }

    @Test
    fun repository_createPurchase_creditDaysValidation() = runTest {
        val fakeApi = FakePurchaseApiService()
        val fakeParties = FakeProcurementPartiesRepository()
        val repo = PurchaseRepositoryImpl(fakeApi, fakeParties, Gson())

        val request = CreatePurchaseRequest(
            supplierId = "supp-1",
            purchaseDate = "2026-10-20",
            orderedWeightKg = 50.0,
            ratePerKg = 4000.0,
            paymentType = "CREDIT",
            creditDays = -5 // Invalid!
        )
        val result = repo.createPurchase(request)

        assertTrue(result.isFailure)
        assertEquals("Credit days must be >= 0 for credit purchases", result.exceptionOrNull()?.message)
    }

    @Test
    fun repository_recordReceipt_acceptedPlusRejectedExceedsReceived_fails() = runTest {
        val fakeApi = FakePurchaseApiService()
        val fakeParties = FakeProcurementPartiesRepository()
        val repo = PurchaseRepositoryImpl(fakeApi, fakeParties, Gson())

        // Critical UI / Domain Rule: accepted + rejected cannot exceed received
        val request = RecordReceiptRequest(
            receiptDate = "2026-10-20",
            receivedWeightKg = 50.0,
            acceptedWeightKg = 45.0,
            rejectedWeightKg = 10.0 // 45 + 10 = 55 > 50!
        )
        val result = repo.recordReceipt("pur-1", request)

        assertTrue(result.isFailure)
        assertEquals(
            "Accepted weight + rejected weight cannot exceed received weight",
            result.exceptionOrNull()?.message
        )
    }

    @Test
    fun repository_recordReceipt_successUpdatesFulfilledWeight() = runTest {
        val fakeApi = FakePurchaseApiService()
        val fakeParties = FakeProcurementPartiesRepository()
        val repo = PurchaseRepositoryImpl(fakeApi, fakeParties, Gson())

        val request = RecordReceiptRequest(
            receiptDate = "2026-10-20",
            receivedWeightKg = 50.500,
            acceptedWeightKg = 50.000,
            rejectedWeightKg = 0.500,
            notes = "Bag dirt deduction"
        )
        val result = repo.recordReceipt("pur-1", request)

        assertTrue(result.isSuccess)
        val receipt = result.getOrNull()!!
        assertEquals(50.000, receipt.acceptedWeightKg, 0.0001)
        assertEquals(0.500, receipt.rejectedWeightKg, 0.0001)
    }

    // ── ViewModel Tests ────────────────────────────────────────────────────

    @Test
    fun purchaseListViewModel_statusFiltering_and_search() = runTest {
        val fakeRepo = FakePurchaseRepository()
        val viewModel = PurchaseListViewModel(fakeRepo)

        advanceUntilIdle()

        val state = viewModel.uiState.value as UiState.Success
        assertEquals(1, state.data.size)
        assertEquals("PUR-2026-0001", state.data[0].purchaseNumber)

        // Filter by FULLY_RECEIVED (none match currently in fakeRepo)
        viewModel.setStatusFilter(PurchaseStatus.FULLY_RECEIVED)
        advanceUntilIdle()

        val emptyState = viewModel.uiState.value as UiState.Success
        assertEquals(0, emptyState.data.size)

        // Clear filter
        viewModel.setStatusFilter(null)
        advanceUntilIdle()

        // Test search query debounce
        viewModel.onSearchQueryChanged("Karnataka")
        advanceTimeBy(350)
        advanceUntilIdle()

        val searchedState = viewModel.uiState.value as UiState.Success
        assertEquals(1, searchedState.data.size)
        assertEquals("Karnataka Silk Filatures Ltd", searchedState.data[0].supplierName)
    }

    @Test
    fun createPurchaseViewModel_dynamicTotalCalculation() {
        val fakeRepo = FakePurchaseRepository()
        val fakeParties = FakeProcurementPartiesRepository()
        val viewModel = CreatePurchaseViewModel(fakeRepo, fakeParties)

        viewModel.onOrderedWeightChanged("100.5")
        viewModel.onRatePerKgChanged("4000")

        assertEquals(402000.0, viewModel.uiState.value.totalAmount, 0.001)
    }

    @Test
    fun createPurchaseViewModel_validationChecks() = runTest {
        val fakeRepo = FakePurchaseRepository()
        val fakeParties = FakeProcurementPartiesRepository()
        val viewModel = CreatePurchaseViewModel(fakeRepo, fakeParties)
        advanceUntilIdle()

        // 1. Missing supplier
        viewModel.createPurchase()
        // Default selected supplier is first supplier from fake repository
        assertNotNull(viewModel.uiState.value.selectedSupplier)

        // 2. Missing/Zero weight
        viewModel.onOrderedWeightChanged("0")
        viewModel.createPurchase()
        assertEquals("Ordered weight must be greater than 0 kg", viewModel.uiState.value.errorMessage)

        // 3. Missing/Zero rate
        viewModel.onOrderedWeightChanged("100")
        viewModel.onRatePerKgChanged("0")
        viewModel.createPurchase()
        assertEquals("Rate per kg must be greater than ₹0", viewModel.uiState.value.errorMessage)

        // 4. Invalid credit days when paymentType == CREDIT
        viewModel.onRatePerKgChanged("4200")
        viewModel.onPaymentTypeChanged("CREDIT")
        viewModel.onCreditDaysChanged("-1")
        viewModel.createPurchase()
        assertEquals("Credit days must be 0 or more", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun createPurchaseViewModel_successfulSubmission() = runTest {
        val fakeRepo = FakePurchaseRepository()
        val fakeParties = FakeProcurementPartiesRepository()
        val viewModel = CreatePurchaseViewModel(fakeRepo, fakeParties)
        advanceUntilIdle()

        viewModel.onOrderedWeightChanged("80.000")
        viewModel.onRatePerKgChanged("4200.00")
        viewModel.onPaymentTypeChanged("CASH")
        viewModel.createPurchase()

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isSuccess)
        assertNull(state.errorMessage)
        assertNotNull(state.createdPurchase)
    }

    @Test
    fun purchaseDetailViewModel_loadsDetails() = runTest {
        val fakeRepo = FakePurchaseRepository()
        val viewModel = PurchaseDetailViewModel(fakeRepo)

        viewModel.loadPurchase("pur-1")
        advanceUntilIdle()

        val state = viewModel.uiState.value as UiState.Success
        assertEquals("PUR-2026-0001", state.data.purchaseNumber)
        assertEquals(250.0, state.data.orderedWeightKg, 0.001)
        assertEquals(100.0, state.data.receivedWeightKg, 0.001)
        assertEquals(150.0, state.data.remainingWeightKg, 0.001)
        assertEquals(1, state.data.receipts.size)
    }

    @Test
    fun recordReceiptViewModel_weightSumRuleEnforcement() = runTest {
        val fakeRepo = FakePurchaseRepository()
        val viewModel = RecordReceiptViewModel(fakeRepo)

        viewModel.initPurchase("pur-1")
        advanceUntilIdle()

        viewModel.onReceivedWeightChanged("100.0")
        viewModel.onAcceptedWeightChanged("95.0")
        viewModel.onRejectedWeightChanged("10.0") // 95 + 10 = 105 > 100!

        // isWeightSumValid property
        assertFalse(viewModel.uiState.value.isWeightSumValid)

        // Attempting to record should fail with rule error
        viewModel.recordReceipt()
        assertEquals(
            "Accepted weight + rejected weight cannot exceed received weight",
            viewModel.uiState.value.errorMessage
        )
    }

    @Test
    fun recordReceiptViewModel_successfulSubmission() = runTest {
        val fakeRepo = FakePurchaseRepository()
        val viewModel = RecordReceiptViewModel(fakeRepo)

        viewModel.initPurchase("pur-1")
        advanceUntilIdle()

        viewModel.onReceivedWeightChanged("100.0")
        viewModel.onAcceptedWeightChanged("98.0")
        viewModel.onRejectedWeightChanged("2.0")
        assertTrue(viewModel.uiState.value.isWeightSumValid)

        viewModel.recordReceipt()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isSuccess)
        assertNull(state.errorMessage)
        assertNotNull(state.recordedReceipt)
    }
}
