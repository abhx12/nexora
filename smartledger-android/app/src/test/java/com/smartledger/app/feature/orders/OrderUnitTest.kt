package com.smartledger.app.feature.orders

import com.google.gson.Gson
import com.smartledger.app.core.ui.theme.StatusCancelledRed
import com.smartledger.app.core.ui.theme.StatusCompletedGreen
import com.smartledger.app.core.ui.theme.StatusCreatedGrey
import com.smartledger.app.core.ui.theme.StatusPartialBlue
import com.smartledger.app.core.ui.theme.StatusPendingOrange
import com.smartledger.app.core.util.UiState
import com.smartledger.app.feature.orders.data.api.OrderApiService
import com.smartledger.app.feature.orders.data.dto.CreateOrderItemRequest
import com.smartledger.app.feature.orders.data.dto.CreateOrderRequest
import com.smartledger.app.feature.orders.data.dto.OrderItemResponse
import com.smartledger.app.feature.orders.data.dto.OrderResponse
import com.smartledger.app.feature.orders.data.dto.UpdateOrderRequest
import com.smartledger.app.feature.orders.data.repository.OrderRepositoryImpl
import com.smartledger.app.feature.orders.domain.OrderRepository
import com.smartledger.app.feature.orders.domain.model.OrderProductType
import com.smartledger.app.feature.orders.domain.model.OrderStatus
import com.smartledger.app.feature.orders.presentation.viewmodel.CreateOrderViewModel
import com.smartledger.app.feature.orders.presentation.viewmodel.OrderDetailViewModel
import com.smartledger.app.feature.orders.presentation.viewmodel.OrderListViewModel
import com.smartledger.app.feature.parties.data.dto.CreatePartyRequest
import com.smartledger.app.feature.parties.data.dto.PartyResponse
import com.smartledger.app.feature.parties.data.dto.UpdatePartyRequest
import com.smartledger.app.feature.parties.domain.PartiesRepository
import com.smartledger.app.feature.parties.domain.model.PartyType
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
 * Fake OrderApiService for repository testing.
 */
class FakeOrderApiService : OrderApiService {
    var shouldFail: Boolean = false
    var failureCode: Int = 400
    var failureMessage: String = "Bad request"

    val sampleOrders = mutableListOf(
        OrderResponse(
            id = "ord-1",
            orderNumber = "ORD-2026-1001",
            customerId = "cust-1",
            customerName = "Ramesh Silks & Sarees",
            orderDate = "2026-10-10",
            requiredDate = "2026-10-25",
            status = "CREATED",
            isLocked = false,
            items = listOf(
                OrderItemResponse("item-1", "ord-1", "SILK_WARP", 50.0, 4800.0, "60s x 40s", 240000.0),
                OrderItemResponse("item-2", "ord-1", "KORA_WARP", 30.0, 4400.0, "28/30 denier", 132000.0)
            )
        ),
        OrderResponse(
            id = "ord-2",
            orderNumber = "ORD-2026-1002",
            customerId = "cust-2",
            customerName = "Mysore Silk Weavers",
            orderDate = "2026-10-12",
            requiredDate = "2026-10-22",
            status = "IN_PROGRESS",
            isLocked = true,
            items = listOf(
                OrderItemResponse("item-3", "ord-2", "SILK_WARP", 80.0, 4900.0, "2-ply warp", 392000.0)
            )
        )
    )

    override suspend fun getOrders(customerId: String?, status: String?): Response<List<OrderResponse>> {
        if (shouldFail) {
            val errorJson = """{"status":$failureCode,"message":"$failureMessage"}"""
            return Response.error(failureCode, errorJson.toResponseBody("application/json".toMediaTypeOrNull()))
        }
        var list = sampleOrders.toList()
        if (customerId != null) list = list.filter { it.customerId == customerId }
        if (status != null) list = list.filter { it.status.equals(status, ignoreCase = true) }
        return Response.success(list)
    }

    override suspend fun getOrderById(id: String): Response<OrderResponse> {
        val found = sampleOrders.firstOrNull { it.id == id || it.orderNumber == id }
        return if (found != null && !shouldFail) {
            Response.success(found)
        } else {
            Response.error(404, "Not found".toResponseBody("text/plain".toMediaTypeOrNull()))
        }
    }

    override suspend fun createOrder(request: CreateOrderRequest): Response<OrderResponse> {
        if (shouldFail) {
            val errorJson = """{"status":$failureCode,"message":"$failureMessage"}"""
            return Response.error(failureCode, errorJson.toResponseBody("application/json".toMediaTypeOrNull()))
        }
        val items = request.items.mapIndexed { idx, itm ->
            OrderItemResponse("new-item-$idx", "ord-new", itm.productType, itm.requiredWeightKg, itm.ratePerKg, itm.warpSpecification, itm.requiredWeightKg * itm.ratePerKg)
        }
        val created = OrderResponse(
            id = "ord-${sampleOrders.size + 1}",
            orderNumber = "ORD-2026-%04d".format(sampleOrders.size + 1001),
            customerId = request.customerId,
            customerName = "Customer",
            orderDate = request.orderDate,
            requiredDate = request.requiredDate,
            status = "CREATED",
            isLocked = false,
            items = items,
            notes = request.notes
        )
        sampleOrders.add(0, created)
        return Response.success(created)
    }

    override suspend fun updateOrder(id: String, request: UpdateOrderRequest): Response<OrderResponse> {
        val idx = sampleOrders.indexOfFirst { it.id == id }
        if (idx == -1) return Response.error(404, "Not found".toResponseBody("text/plain".toMediaTypeOrNull()))

        val current = sampleOrders[idx]
        if (current.isOrderLocked) {
            val errorJson = """{"status":409,"code":"ORDER_LOCKED","message":"This order is in production and cannot be changed."}"""
            return Response.error(409, errorJson.toResponseBody("application/json".toMediaTypeOrNull()))
        }
        val updated = current.copy(requiredDate = request.requiredDate ?: current.requiredDate)
        sampleOrders[idx] = updated
        return Response.success(updated)
    }

    override suspend fun lockOrder(id: String): Response<OrderResponse> {
        val idx = sampleOrders.indexOfFirst { it.id == id }
        if (idx == -1) return Response.error(404, "Not found".toResponseBody("text/plain".toMediaTypeOrNull()))
        val updated = sampleOrders[idx].copy(isLocked = true, status = "IN_PROGRESS")
        sampleOrders[idx] = updated
        return Response.success(updated)
    }

    override suspend fun cancelOrder(id: String): Response<OrderResponse> {
        val idx = sampleOrders.indexOfFirst { it.id == id }
        if (idx == -1) return Response.error(404, "Not found".toResponseBody("text/plain".toMediaTypeOrNull()))
        val updated = sampleOrders[idx].copy(status = "CANCELLED")
        sampleOrders[idx] = updated
        return Response.success(updated)
    }
}

/**
 * Fake PartiesRepository for Customer dropdown.
 */
class FakeOrderPartiesRepository : PartiesRepository {
    val customers = mutableListOf(
        PartyResponse("cust-1", "CUST-001", "Ramesh Silks & Sarees", "CUSTOMER", "9845012345", "Bangalore", "ACTIVE"),
        PartyResponse("cust-2", "CUST-002", "Mysore Silk Weavers Co-op", "CUSTOMER", "9845098765", "Mysore", "ACTIVE")
    )

    override suspend fun getParties(type: PartyType?, search: String?): Result<List<PartyResponse>> {
        var list = customers.toList()
        if (type != null) list = list.filter { it.partyType == type.name }
        if (!search.isNullOrBlank()) list = list.filter { it.name.contains(search, true) }
        return Result.success(list)
    }

    override suspend fun getPartyById(id: String): Result<PartyResponse> {
        val found = customers.firstOrNull { it.id == id }
        return if (found != null) Result.success(found) else Result.failure(Exception("Customer not found"))
    }

    override suspend fun createParty(request: CreatePartyRequest): Result<PartyResponse> = Result.failure(Exception("Not implemented"))
    override suspend fun updateParty(id: String, request: UpdatePartyRequest): Result<PartyResponse> = Result.failure(Exception("Not implemented"))
}

/**
 * Fake OrderRepository for ViewModel tests.
 */
class FakeOrderRepository : OrderRepository {
    var shouldFail: Boolean = false
    var failureMessage: String = "Order repository error"

    val orders = mutableListOf(
        OrderResponse(
            id = "ord-1",
            orderNumber = "ORD-2026-1001",
            customerId = "cust-1",
            customerName = "Ramesh Silks & Sarees",
            orderDate = "2026-10-10",
            requiredDate = "2026-10-25",
            status = "CREATED",
            isLocked = false,
            items = listOf(
                OrderItemResponse("item-1", "ord-1", "SILK_WARP", 50.0, 4800.0, "60s x 40s", 240000.0),
                OrderItemResponse("item-2", "ord-1", "KORA_WARP", 30.0, 4400.0, "28/30 denier", 132000.0)
            )
        ),
        OrderResponse(
            id = "ord-2",
            orderNumber = "ORD-2026-1002",
            customerId = "cust-2",
            customerName = "Mysore Silk Weavers Co-op",
            orderDate = "2026-10-12",
            requiredDate = "2026-10-22",
            status = "IN_PROGRESS",
            isLocked = true,
            items = listOf(
                OrderItemResponse("item-3", "ord-2", "SILK_WARP", 80.0, 4900.0, "2-ply warp", 392000.0)
            )
        )
    )

    override suspend fun getOrders(customerId: String?, status: OrderStatus?): Result<List<OrderResponse>> {
        return if (shouldFail) {
            Result.failure(Exception(failureMessage))
        } else {
            var list = orders.toList()
            if (customerId != null) list = list.filter { it.customerId == customerId }
            if (status != null) list = list.filter { it.status.equals(status.name, ignoreCase = true) }
            Result.success(list)
        }
    }

    override suspend fun getOrderById(id: String): Result<OrderResponse> {
        val found = orders.firstOrNull { it.id == id }
        return if (found != null && !shouldFail) {
            Result.success(found)
        } else {
            Result.failure(Exception(if (shouldFail) failureMessage else "Order not found"))
        }
    }

    override suspend fun createOrder(request: CreateOrderRequest): Result<OrderResponse> {
        if (shouldFail) return Result.failure(Exception(failureMessage))
        val items = request.items.mapIndexed { idx, itm ->
            OrderItemResponse("new-item-$idx", "ord-new", itm.productType, itm.requiredWeightKg, itm.ratePerKg, itm.warpSpecification, itm.requiredWeightKg * itm.ratePerKg)
        }
        val created = OrderResponse(
            id = "ord-new",
            orderNumber = "ORD-2026-9999",
            customerId = request.customerId,
            customerName = "Ramesh Silks & Sarees",
            orderDate = request.orderDate,
            requiredDate = request.requiredDate,
            status = "CREATED",
            isLocked = false,
            items = items,
            notes = request.notes
        )
        orders.add(0, created)
        return Result.success(created)
    }

    override suspend fun updateOrder(id: String, request: UpdateOrderRequest): Result<OrderResponse> {
        val idx = orders.indexOfFirst { it.id == id }
        if (idx == -1) return Result.failure(Exception("Order not found"))
        val current = orders[idx]
        if (current.isOrderLocked) {
            return Result.failure(Exception("This order is in production and cannot be changed."))
        }
        val updated = current.copy(requiredDate = request.requiredDate ?: current.requiredDate)
        orders[idx] = updated
        return Result.success(updated)
    }

    override suspend fun lockOrder(id: String): Result<OrderResponse> {
        val idx = orders.indexOfFirst { it.id == id }
        if (idx == -1) return Result.failure(Exception("Order not found"))
        val updated = orders[idx].copy(isLocked = true, status = "IN_PROGRESS")
        orders[idx] = updated
        return Result.success(updated)
    }

    override suspend fun cancelOrder(id: String): Result<OrderResponse> {
        val idx = orders.indexOfFirst { it.id == id }
        if (idx == -1) return Result.failure(Exception("Order not found"))
        val current = orders[idx]
        if (current.isOrderLocked) {
            return Result.failure(Exception("Cannot cancel an order that is in production or completed"))
        }
        val updated = current.copy(status = "CANCELLED")
        orders[idx] = updated
        return Result.success(updated)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class OrderUnitTest {

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
    fun orderStatus_colorMappingMatchesPlan() {
        assertEquals(StatusCreatedGrey, OrderStatus.CREATED.color)
        assertEquals(StatusPartialBlue, OrderStatus.CONFIRMED.color)
        assertEquals(StatusPendingOrange, OrderStatus.IN_PROGRESS.color)
        assertEquals(StatusCompletedGreen, OrderStatus.COMPLETED.color)
        assertEquals(StatusCancelledRed, OrderStatus.CANCELLED.color)
    }

    // ── Repository Tests ────────────────────────────────────────────────────

    @Test
    fun repository_createOrder_withMultipleItems_calculatesTotals() = runTest {
        val fakeApi = FakeOrderApiService()
        val fakeParties = FakeOrderPartiesRepository()
        val repo = OrderRepositoryImpl(fakeApi, fakeParties, Gson())

        val request = CreateOrderRequest(
            customerId = "cust-1",
            orderDate = "2026-10-20",
            requiredDate = "2026-11-05",
            items = listOf(
                CreateOrderItemRequest("SILK_WARP", 40.0, 4800.0, "60s x 40s"),
                CreateOrderItemRequest("KORA_WARP", 20.0, 4500.0, "28/30 denier")
            ),
            notes = "Diwali shipment"
        )
        val result = repo.createOrder(request)

        assertTrue(result.isSuccess)
        val order = result.getOrNull()!!
        assertEquals("cust-1", order.customerId)
        assertEquals(2, order.items.size)
        assertEquals(60.0, order.totalWeightKg, 0.001)
        assertEquals(282000.0, order.totalAmount, 0.001)
        assertEquals("CREATED", order.status)
        assertFalse(order.isOrderLocked)
    }

    @Test
    fun repository_createOrder_emptyItems_fails() = runTest {
        val fakeApi = FakeOrderApiService()
        val fakeParties = FakeOrderPartiesRepository()
        val repo = OrderRepositoryImpl(fakeApi, fakeParties, Gson())

        val request = CreateOrderRequest(
            customerId = "cust-1",
            orderDate = "2026-10-20",
            requiredDate = "2026-11-05",
            items = emptyList()
        )
        val result = repo.createOrder(request)

        assertTrue(result.isFailure)
        assertEquals("Order must have at least one line item", result.exceptionOrNull()?.message)
    }

    @Test
    fun repository_createOrder_zeroWeight_fails() = runTest {
        val fakeApi = FakeOrderApiService()
        val fakeParties = FakeOrderPartiesRepository()
        val repo = OrderRepositoryImpl(fakeApi, fakeParties, Gson())

        val request = CreateOrderRequest(
            customerId = "cust-1",
            orderDate = "2026-10-20",
            requiredDate = "2026-11-05",
            items = listOf(CreateOrderItemRequest("SILK_WARP", 0.0, 4800.0, null))
        )
        val result = repo.createOrder(request)

        assertTrue(result.isFailure)
        assertEquals("Item #1: Required weight must be greater than 0 kg", result.exceptionOrNull()?.message)
    }

    @Test
    fun repository_updateOrder_whenLocked_failsWithProductionBannerMessage() = runTest {
        val fakeApi = FakeOrderApiService()
        val fakeParties = FakeOrderPartiesRepository()
        val repo = OrderRepositoryImpl(fakeApi, fakeParties, Gson())

        // ord-2 is in production (isLocked = true, status = IN_PROGRESS)
        val result = repo.updateOrder("ord-2", UpdateOrderRequest(requiredDate = "2026-11-15"))

        assertTrue(result.isFailure)
        assertEquals(
            "This order is in production and cannot be changed.",
            result.exceptionOrNull()?.message
        )
    }

    @Test
    fun repository_lockOrder_setsStatusToInProgress() = runTest {
        val fakeApi = FakeOrderApiService()
        val fakeParties = FakeOrderPartiesRepository()
        val repo = OrderRepositoryImpl(fakeApi, fakeParties, Gson())

        val result = repo.lockOrder("ord-1")

        assertTrue(result.isSuccess)
        val lockedOrder = result.getOrNull()!!
        assertTrue(lockedOrder.isLocked)
        assertEquals("IN_PROGRESS", lockedOrder.status)
        assertTrue(lockedOrder.isOrderLocked)
    }

    // ── ViewModel Tests ────────────────────────────────────────────────────

    @Test
    fun createOrderViewModel_dynamicItems() {
        val fakeRepo = FakeOrderRepository()
        val fakeParties = FakeOrderPartiesRepository()
        val viewModel = CreateOrderViewModel(fakeRepo, fakeParties)

        // Initial state has 1 item
        assertEquals(1, viewModel.uiState.value.items.size)

        // Add 2 items -> 3 items
        viewModel.addItem()
        viewModel.addItem()
        assertEquals(3, viewModel.uiState.value.items.size)

        // Remove 1 item -> 2 items
        val toRemoveId = viewModel.uiState.value.items[1].id
        viewModel.removeItem(toRemoveId)
        assertEquals(2, viewModel.uiState.value.items.size)
        assertFalse(viewModel.uiState.value.items.any { it.id == toRemoveId })
    }

    @Test
    fun createOrderViewModel_liveTotalCalculations() {
        val fakeRepo = FakeOrderRepository()
        val fakeParties = FakeOrderPartiesRepository()
        val viewModel = CreateOrderViewModel(fakeRepo, fakeParties)

        val item1Id = viewModel.uiState.value.items[0].id
        viewModel.updateItemWeight(item1Id, "50.0")
        viewModel.updateItemRate(item1Id, "4000.0")

        viewModel.addItem()
        val item2Id = viewModel.uiState.value.items[1].id
        viewModel.updateItemWeight(item2Id, "25.0")
        viewModel.updateItemRate(item2Id, "4500.0")

        // Item 1: 50 * 4000 = 200,000; Item 2: 25 * 4500 = 112,500; Total = 312,500
        // Weight: 50 + 25 = 75 kg
        assertEquals(75.0, viewModel.uiState.value.totalWeightKg, 0.001)
        assertEquals(312500.0, viewModel.uiState.value.totalAmount, 0.001)
    }

    @Test
    fun createOrderViewModel_validation_itemWeightAndRate() = runTest {
        val fakeRepo = FakeOrderRepository()
        val fakeParties = FakeOrderPartiesRepository()
        val viewModel = CreateOrderViewModel(fakeRepo, fakeParties)
        advanceUntilIdle()

        val item1Id = viewModel.uiState.value.items[0].id
        viewModel.updateItemWeight(item1Id, "0")
        viewModel.createOrder()

        assertEquals("Item #1: Required weight must be greater than 0 kg", viewModel.uiState.value.errorMessage)

        viewModel.updateItemWeight(item1Id, "50")
        viewModel.updateItemRate(item1Id, "0")
        viewModel.createOrder()

        assertEquals("Item #1: Rate per kg must be greater than ₹0", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun orderDetailViewModel_loadAndLockOrder() = runTest {
        val fakeRepo = FakeOrderRepository()
        val viewModel = OrderDetailViewModel(fakeRepo)

        viewModel.loadOrder("ord-1")
        advanceUntilIdle()

        val state = viewModel.uiState.value.orderState as UiState.Success
        assertEquals("ORD-2026-1001", state.data.orderNumber)
        assertFalse(state.data.isOrderLocked)

        // Lock order
        viewModel.lockOrder()
        advanceUntilIdle()

        val updatedState = viewModel.uiState.value.orderState as UiState.Success
        assertTrue(updatedState.data.isOrderLocked)
        assertEquals("IN_PROGRESS", updatedState.data.status)
        assertEquals("Order locked and moved into production", viewModel.uiState.value.actionSuccessMessage)
    }

    @Test
    fun orderListViewModel_filteringAndSearch() = runTest {
        val fakeRepo = FakeOrderRepository()
        val viewModel = OrderListViewModel(fakeRepo)
        advanceUntilIdle()

        val state = viewModel.uiState.value as UiState.Success
        assertEquals(2, state.data.size)

        // Filter by IN_PROGRESS
        viewModel.selectStatus(OrderStatus.IN_PROGRESS)
        advanceUntilIdle()

        val inProgressState = viewModel.uiState.value as UiState.Success
        assertEquals(1, inProgressState.data.size)
        assertEquals("ORD-2026-1002", inProgressState.data[0].orderNumber)

        // Clear status filter
        viewModel.selectStatus(null)
        advanceUntilIdle()

        // Search debounce
        viewModel.onSearchQueryChanged("Ramesh")
        advanceTimeBy(350)
        advanceUntilIdle()

        val searchState = viewModel.uiState.value as UiState.Success
        assertEquals(1, searchState.data.size)
        assertEquals("Ramesh Silks & Sarees", searchState.data[0].customerName)
    }
}
