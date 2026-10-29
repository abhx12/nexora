package com.smartledger.app.feature.production

import androidx.lifecycle.SavedStateHandle
import com.google.gson.Gson
import com.smartledger.app.core.ui.theme.StatusCancelledRed
import com.smartledger.app.core.ui.theme.StatusCompletedGreen
import com.smartledger.app.core.ui.theme.StatusCreatedGrey
import com.smartledger.app.core.ui.theme.StatusPendingOrange
import com.smartledger.app.core.util.UiState
import com.smartledger.app.feature.orders.data.dto.OrderItemResponse
import com.smartledger.app.feature.orders.data.dto.OrderResponse
import com.smartledger.app.feature.orders.domain.OrderRepository
import com.smartledger.app.feature.orders.domain.model.OrderStatus
import com.smartledger.app.feature.production.data.api.ProductionApiService
import com.smartledger.app.feature.production.data.dto.CompleteProductionRequest
import com.smartledger.app.feature.production.data.dto.CreateProductionBatchRequest
import com.smartledger.app.feature.production.data.dto.ProductionBatchResponse
import com.smartledger.app.feature.production.data.dto.StartProductionRequest
import com.smartledger.app.feature.production.data.repository.ProductionRepositoryImpl
import com.smartledger.app.feature.production.domain.ProductionRepository
import com.smartledger.app.feature.production.domain.model.BatchStatus
import com.smartledger.app.feature.production.presentation.viewmodel.CompleteProductionViewModel
import com.smartledger.app.feature.production.presentation.viewmodel.CreateProductionBatchViewModel
import com.smartledger.app.feature.production.presentation.viewmodel.ProductionDetailViewModel
import com.smartledger.app.feature.production.presentation.viewmodel.ProductionListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID
import kotlin.math.abs

/**
 * Fake ProductionApiService for repository tests.
 */
class FakeProductionApiService : ProductionApiService {
    val batches = mutableListOf<ProductionBatchResponse>()

    override suspend fun getProductionBatches(status: String?): List<ProductionBatchResponse> {
        return if (status.isNullOrBlank()) batches else batches.filter { it.status.equals(status, ignoreCase = true) }
    }

    override suspend fun getProductionBatchById(id: String): ProductionBatchResponse {
        return batches.find { it.id == id } ?: throw NoSuchElementException("Batch not found")
    }

    override suspend fun createProductionBatch(request: CreateProductionBatchRequest): ProductionBatchResponse {
        val newBatch = ProductionBatchResponse(
            id = UUID.randomUUID().toString(),
            batchNumber = "BATCH-2026-9999",
            orderId = request.orderId,
            productType = request.productType,
            status = "PLANNED",
            inputWeightKg = request.inputWeightKg,
            startDate = request.startDate,
            notes = request.notes
        )
        batches.add(0, newBatch)
        return newBatch
    }

    override suspend fun startProduction(id: String, request: StartProductionRequest): ProductionBatchResponse {
        val index = batches.indexOfFirst { it.id == id }
        val curr = batches[index]
        val updated = curr.copy(status = "IN_PROGRESS", startDate = request.startDate ?: curr.startDate)
        batches[index] = updated
        return updated
    }

    override suspend fun completeProduction(id: String, request: CompleteProductionRequest): ProductionBatchResponse {
        val index = batches.indexOfFirst { it.id == id }
        val curr = batches[index]
        val updated = curr.copy(
            status = "COMPLETED",
            outputWeightKg = request.outputWeightKg,
            wastageWeightKg = request.wastageWeightKg,
            completionDate = request.completionDate
        )
        batches[index] = updated
        return updated
    }
}

/**
 * Fake ProductionRepository for fast, deterministic ViewModel tests.
 */
class FakeProductionRepository : ProductionRepository {
    var availableStock: Double = 324.500

    val batches = mutableListOf(
        ProductionBatchResponse(
            id = "batch-1",
            batchNumber = "BATCH-2026-0001",
            orderId = "order-1",
            orderNumber = "ORD-2026-0001",
            customerName = "Sri Lakshmi Silks",
            productType = "SILK_WARP",
            status = "COMPLETED",
            inputWeightKg = 100.0,
            outputWeightKg = 96.5,
            wastageWeightKg = 3.5,
            startDate = "2026-10-10",
            completionDate = "2026-10-12"
        ),
        ProductionBatchResponse(
            id = "batch-2",
            batchNumber = "BATCH-2026-0002",
            orderId = "order-2",
            orderNumber = "ORD-2026-0002",
            customerName = "Venkateshwara Tex",
            productType = "KORA_WARP",
            status = "IN_PROGRESS",
            inputWeightKg = 85.0,
            startDate = "2026-10-18"
        ),
        ProductionBatchResponse(
            id = "batch-3",
            batchNumber = "BATCH-2026-0003",
            productType = "VUDA_WARP",
            status = "PLANNED",
            inputWeightKg = 50.0,
            startDate = "2026-10-22"
        )
    )

    override suspend fun getProductionBatches(status: String?): Result<List<ProductionBatchResponse>> {
        val filtered = if (status.isNullOrBlank() || status.equals("ALL", true)) {
            batches.toList()
        } else {
            batches.filter { it.status.equals(status, true) }
        }
        return Result.success(filtered)
    }

    override suspend fun getProductionBatchById(id: String): Result<ProductionBatchResponse> {
        val found = batches.find { it.id == id }
        return if (found != null) Result.success(found) else Result.failure(Exception("Batch not found"))
    }

    override suspend fun createProductionBatch(request: CreateProductionBatchRequest): Result<ProductionBatchResponse> {
        if (request.inputWeightKg <= 0) {
            return Result.failure(IllegalArgumentException("Input weight must be greater than 0 kg"))
        }
        if (request.inputWeightKg > availableStock) {
            return Result.failure(Exception("Not enough raw silk (%.3f kg needed, %.3f kg available)".format(request.inputWeightKg, availableStock)))
        }

        val newBatch = ProductionBatchResponse(
            id = "batch-new",
            batchNumber = "BATCH-2026-0004",
            orderId = request.orderId,
            productType = request.productType,
            status = "PLANNED",
            inputWeightKg = request.inputWeightKg,
            startDate = request.startDate,
            notes = request.notes
        )
        batches.add(0, newBatch)
        return Result.success(newBatch)
    }

    override suspend fun startProduction(id: String, startDate: String?, notes: String?): Result<ProductionBatchResponse> {
        val idx = batches.indexOfFirst { it.id == id }
        if (idx == -1) return Result.failure(Exception("Batch not found"))
        val current = batches[idx]
        if (current.status != "PLANNED") return Result.failure(Exception("Batch is not PLANNED"))
        if (current.inputWeightKg > availableStock) return Result.failure(Exception("Not enough raw silk"))

        availableStock -= current.inputWeightKg
        val updated = current.copy(status = "IN_PROGRESS", startDate = startDate ?: "2026-10-20")
        batches[idx] = updated
        return Result.success(updated)
    }

    override suspend fun completeProduction(id: String, request: CompleteProductionRequest): Result<ProductionBatchResponse> {
        val idx = batches.indexOfFirst { it.id == id }
        if (idx == -1) return Result.failure(Exception("Batch not found"))
        val current = batches[idx]
        if (current.status != "IN_PROGRESS") return Result.failure(Exception("Batch not IN_PROGRESS"))

        val diff = abs(current.inputWeightKg - (request.outputWeightKg + request.wastageWeightKg))
        if (diff > 0.005) {
            return Result.failure(IllegalArgumentException("Input weight (%.3f kg) must equal Output (%.3f kg) + Wastage (%.3f kg)".format(
                current.inputWeightKg, request.outputWeightKg, request.wastageWeightKg
            )))
        }

        val updated = current.copy(
            status = "COMPLETED",
            outputWeightKg = request.outputWeightKg,
            wastageWeightKg = request.wastageWeightKg,
            completionDate = request.completionDate
        )
        batches[idx] = updated
        return Result.success(updated)
    }

    override suspend fun getAvailableRawSilkKg(): Double = availableStock
}

/**
 * Fake OrderRepository for CreateProductionBatchViewModel testing.
 */
class FakeOrderRepositoryForProduction : OrderRepository {
    override suspend fun getOrders(customerId: String?, status: OrderStatus?): Result<List<OrderResponse>> =
        Result.success(
            listOf(
                OrderResponse(
                    id = "ord-1",
                    orderNumber = "ORD-2026-0001",
                    customerId = "cust-1",
                    customerName = "Sri Lakshmi Silks",
                    orderDate = "2026-10-10",
                    requiredDate = "2026-10-25",
                    status = "CONFIRMED",
                    isLocked = false,
                    items = listOf(
                        OrderItemResponse("item-1", "ord-1", "SILK_WARP", 100.0, 4800.0, "60s", 480000.0)
                    )
                )
            )
        )

    override suspend fun getOrderById(id: String): Result<OrderResponse> = throw UnsupportedOperationException()
    override suspend fun createOrder(request: com.smartledger.app.feature.orders.data.dto.CreateOrderRequest): Result<OrderResponse> = throw UnsupportedOperationException()
    override suspend fun updateOrder(id: String, request: com.smartledger.app.feature.orders.data.dto.UpdateOrderRequest): Result<OrderResponse> = throw UnsupportedOperationException()
    override suspend fun lockOrder(id: String): Result<OrderResponse> = throw UnsupportedOperationException()
    override suspend fun cancelOrder(id: String): Result<OrderResponse> = throw UnsupportedOperationException()
}

@OptIn(ExperimentalCoroutinesApi::class)
class ProductionUnitTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeApi: FakeProductionApiService
    private lateinit var repository: ProductionRepositoryImpl

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeApi = FakeProductionApiService()
        repository = ProductionRepositoryImpl(fakeApi, Gson())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ─────────────────────────────────────────────────────────────
    // 1. BatchStatus Enum & Color Tests
    // ─────────────────────────────────────────────────────────────

    @Test
    fun testBatchStatusFromStringAndColors() {
        assertEquals(BatchStatus.PLANNED, BatchStatus.fromString("PLANNED"))
        assertEquals(BatchStatus.PLANNED, BatchStatus.fromString("CREATED"))
        assertEquals(BatchStatus.IN_PROGRESS, BatchStatus.fromString("IN_PROGRESS"))
        assertEquals(BatchStatus.COMPLETED, BatchStatus.fromString("COMPLETED"))
        assertEquals(BatchStatus.CANCELLED, BatchStatus.fromString("CANCELLED"))

        assertEquals(StatusCreatedGrey, BatchStatus.PLANNED.color)
        assertEquals(StatusPendingOrange, BatchStatus.IN_PROGRESS.color)
        assertEquals(StatusCompletedGreen, BatchStatus.COMPLETED.color)
        assertEquals(StatusCancelledRed, BatchStatus.CANCELLED.color)
    }

    // ─────────────────────────────────────────────────────────────
    // 2. ProductionBatchResponse Calculations
    // ─────────────────────────────────────────────────────────────

    @Test
    fun testYieldAndWastageCalculations() {
        val completedBatch = ProductionBatchResponse(
            id = "b1",
            batchNumber = "BATCH-2026-0001",
            productType = "SILK_WARP",
            status = "COMPLETED",
            inputWeightKg = 100.0,
            outputWeightKg = 96.5,
            wastageWeightKg = 3.5
        )

        assertEquals(96.5, completedBatch.yieldPercentage!!, 0.001)
        assertEquals(3.5, completedBatch.wastagePercentage!!, 0.001)

        val inProgressBatch = ProductionBatchResponse(
            id = "b2",
            batchNumber = "BATCH-2026-0002",
            productType = "KORA_WARP",
            status = "IN_PROGRESS",
            inputWeightKg = 85.0
        )
        assertNull(inProgressBatch.yieldPercentage)
        assertNull(inProgressBatch.wastagePercentage)
    }

    // ─────────────────────────────────────────────────────────────
    // 3. ProductionRepository Tests (Lifecycle, Inventory & Equation)
    // ─────────────────────────────────────────────────────────────

    @Test
    fun testRepositoryGetBatchesAndFilter() = runTest {
        val result = repository.getProductionBatches(null)
        assertTrue(result.isSuccess)
        val batches = result.getOrNull()!!
        assertEquals(3, batches.size)

        val completedOnly = repository.getProductionBatches("COMPLETED").getOrNull()!!
        assertEquals(1, completedOnly.size)
        assertEquals("COMPLETED", completedOnly[0].status)
    }

    @Test
    fun testRepositoryCreateBatchWithValidationAndStockCheck() = runTest {
        // Test invalid weight <= 0
        val invalidReq = CreateProductionBatchRequest(
            productType = "SILK_WARP",
            inputWeightKg = 0.0,
            startDate = "2026-10-20"
        )
        val failResult = repository.createProductionBatch(invalidReq)
        assertTrue(failResult.isFailure)

        // Test weight exceeding stock (> 324.500 kg)
        val excessiveReq = CreateProductionBatchRequest(
            productType = "SILK_WARP",
            inputWeightKg = 500.0,
            startDate = "2026-10-20"
        )
        val excessResult = repository.createProductionBatch(excessiveReq)
        assertTrue(excessResult.isFailure)
        assertTrue(excessResult.exceptionOrNull()?.message?.contains("Not enough raw silk") == true)

        // Test valid creation within stock
        val validReq = CreateProductionBatchRequest(
            productType = "SILK_WARP",
            inputWeightKg = 50.0,
            startDate = "2026-10-20",
            notes = "Test batch"
        )
        val successResult = repository.createProductionBatch(validReq)
        assertTrue(successResult.isSuccess)
        val created = successResult.getOrNull()!!
        assertEquals("PLANNED", created.status)
        assertEquals(50.0, created.inputWeightKg, 0.001)
    }

    @Test
    fun testRepositoryStartProduction() = runTest {
        // Start PLANNED batch-3 (input 50.0 kg, stock is 324.5 kg)
        val startResult = repository.startProduction("batch-3")
        assertTrue(startResult.isSuccess)
        val started = startResult.getOrNull()!!
        assertEquals("IN_PROGRESS", started.status)

        // Available raw silk should now be reduced by 50 kg
        assertEquals(274.500, repository.getAvailableRawSilkKg(), 0.001)

        // Starting again should fail
        val duplicateStart = repository.startProduction("batch-3")
        assertTrue(duplicateStart.isFailure)
    }

    @Test
    fun testRepositoryCompleteProductionEquationValidation() = runTest {
        // batch-2 is IN_PROGRESS with inputWeightKg = 85.0 kg
        // Unbalanced: 80.0 + 3.0 = 83.0 != 85.0
        val unbalancedReq = CompleteProductionRequest(
            outputWeightKg = 80.0,
            wastageWeightKg = 3.0,
            completionDate = "2026-10-20"
        )
        val failResult = repository.completeProduction("batch-2", unbalancedReq)
        assertTrue(failResult.isFailure)
        assertTrue(failResult.exceptionOrNull()?.message?.contains("must equal Output") == true)

        // Balanced: 82.0 + 3.0 = 85.0 == inputWeightKg
        val balancedReq = CompleteProductionRequest(
            outputWeightKg = 82.0,
            wastageWeightKg = 3.0,
            completionDate = "2026-10-20",
            notes = "Completed successfully"
        )
        val successResult = repository.completeProduction("batch-2", balancedReq)
        assertTrue(successResult.isSuccess)
        val completed = successResult.getOrNull()!!
        assertEquals("COMPLETED", completed.status)
        assertEquals(82.0, completed.outputWeightKg!!, 0.001)
        assertEquals(3.0, completed.wastageWeightKg!!, 0.001)
    }

    // ─────────────────────────────────────────────────────────────
    // 4. ProductionListViewModel Tests
    // ─────────────────────────────────────────────────────────────

    @Test
    fun testProductionListViewModelSearchAndFilter() = runTest {
        val fakeRepo = FakeProductionRepository()
        val viewModel = ProductionListViewModel(fakeRepo)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        val list = (state as UiState.Success).data
        assertEquals(3, list.size)

        // Filter by IN_PROGRESS
        viewModel.setStatusFilter("IN_PROGRESS")
        advanceUntilIdle()
        val filteredState = viewModel.uiState.value as UiState.Success
        assertEquals(1, filteredState.data.size)
        assertEquals("BATCH-2026-0002", filteredState.data[0].batchNumber)

        // Search query
        viewModel.setStatusFilter("ALL")
        viewModel.setSearchQuery("Lakshmi")
        advanceUntilIdle()
        val searchState = viewModel.uiState.value as UiState.Success
        assertEquals(1, searchState.data.size)
        assertEquals("Sri Lakshmi Silks", searchState.data[0].customerName)
    }

    // ─────────────────────────────────────────────────────────────
    // 5. CreateProductionBatchViewModel Tests
    // ─────────────────────────────────────────────────────────────

    @Test
    fun testCreateProductionBatchViewModel() = runTest {
        val fakeRepo = FakeProductionRepository()
        val fakeOrders = FakeOrderRepositoryForProduction()
        val viewModel = CreateProductionBatchViewModel(fakeRepo, fakeOrders)
        advanceUntilIdle()

        assertEquals(324.500, viewModel.availableRawSilkKg.value, 0.001)
        assertEquals(1, viewModel.orders.value.size)

        // Invalid weight (exceeds stock)
        viewModel.setInputWeight("400.0")
        var createdId: String? = null
        viewModel.createBatch { createdId = it }
        advanceUntilIdle()
        assertNull(createdId)
        assertNotNull(viewModel.errorMessage.value)

        // Valid weight
        viewModel.setInputWeight("60.0")
        viewModel.createBatch { createdId = it }
        advanceUntilIdle()
        assertNotNull(createdId)
        assertNull(viewModel.errorMessage.value)
    }

    // ─────────────────────────────────────────────────────────────
    // 6. CompleteProductionViewModel Equation Balancing & Auto-calculate
    // ─────────────────────────────────────────────────────────────

    @Test
    fun testCompleteProductionViewModelEquationBalancing() = runTest {
        val fakeRepo = FakeProductionRepository()
        val savedStateHandle = SavedStateHandle(mapOf("batchId" to "batch-2"))
        val viewModel = CompleteProductionViewModel(fakeRepo, savedStateHandle)
        advanceUntilIdle()

        // Batch 2 input weight is 85.0 kg
        assertEquals(85.0, viewModel.inputWeight.value, 0.001)

        // Initially empty -> unbalanced
        assertFalse(viewModel.equationState.value.isBalanced)

        // Enter output 82.0
        viewModel.setOutputWeight("82.0")
        advanceUntilIdle()
        assertFalse(viewModel.equationState.value.isBalanced)

        // Use auto-calculate wastage helper
        viewModel.autoBalanceWastage()
        advanceUntilIdle()
        assertEquals("3.000", viewModel.wastageWeight.value)
        assertTrue(viewModel.equationState.value.isBalanced)

        // Submit completion
        var completed = false
        viewModel.submitCompletion { completed = true }
        advanceUntilIdle()
        assertTrue(completed)
    }

    // ─────────────────────────────────────────────────────────────
    // 7. ProductionDetailViewModel Tests
    // ─────────────────────────────────────────────────────────────

    @Test
    fun testProductionDetailViewModelStartProduction() = runTest {
        val fakeRepo = FakeProductionRepository()
        val savedStateHandle = SavedStateHandle(mapOf("batchId" to "batch-3"))
        val viewModel = ProductionDetailViewModel(fakeRepo, savedStateHandle)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        assertEquals("PLANNED", (state as UiState.Success).data.status)

        viewModel.startProduction()
        advanceUntilIdle()

        val updatedState = viewModel.uiState.value
        assertTrue(updatedState is UiState.Success)
        assertEquals("IN_PROGRESS", (updatedState as UiState.Success).data.status)
        assertNotNull(viewModel.actionSuccess.value)
    }
}
