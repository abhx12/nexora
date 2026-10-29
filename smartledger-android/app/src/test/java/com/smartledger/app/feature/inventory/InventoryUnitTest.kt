package com.smartledger.app.feature.inventory

import com.google.gson.Gson
import com.smartledger.app.core.util.UiState
import com.smartledger.app.feature.inventory.data.api.InventoryApiService
import com.smartledger.app.feature.inventory.data.dto.ExternalWipItemResponse
import com.smartledger.app.feature.inventory.data.dto.InventoryMovementResponse
import com.smartledger.app.feature.inventory.data.dto.InventorySummaryResponse
import com.smartledger.app.feature.inventory.data.repository.InventoryRepositoryImpl
import com.smartledger.app.feature.inventory.domain.InventoryRepository
import com.smartledger.app.feature.inventory.domain.model.MovementType
import com.smartledger.app.feature.inventory.domain.model.ProductType
import com.smartledger.app.feature.inventory.presentation.viewmodel.InventorySummaryViewModel
import com.smartledger.app.feature.inventory.presentation.viewmodel.MovementHistoryViewModel
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

/**
 * Fake InventoryApiService for testing repository and offline fallbacks.
 */
class FakeInventoryApiService : InventoryApiService {
    var shouldFail: Boolean = false
    var failureCode: Int = 500
    var failureMessage: String = "Internal server error"

    val sampleSummary = InventorySummaryResponse(
        rawSilkKg = 324.500,
        koraWarpKg = 85.200,
        silkWarpKg = 42.750,
        vudaWarpKg = 31.000,
        externalWipKg = 82.500
    )

    val sampleWip = mutableListOf(
        ExternalWipItemResponse(
            id = "wip-1",
            manufacturerId = "mfg-1",
            manufacturerName = "Sri Balaji Twisters & Warpers",
            productType = "KORA_WARP",
            quantityKg = 52.500,
            jobId = "JOB-2026-0012",
            jobTitle = "Twisting 28/30 denier silk",
            issueDate = "2026-10-14"
        ),
        ExternalWipItemResponse(
            id = "wip-2",
            manufacturerId = "mfg-2",
            manufacturerName = "Chamundeshwari Silk Sizers",
            productType = "SILK_WARP",
            quantityKg = 30.000,
            jobId = "JOB-2026-0015",
            jobTitle = "Sizing and beam preparation",
            issueDate = "2026-10-17"
        )
    )

    val sampleMovements = mutableListOf(
        InventoryMovementResponse(
            id = "mov-1",
            movementNumber = "MOV-2026-0101",
            productType = "RAW_SILK",
            movementType = "RECEIPT",
            quantityKg = 100.000,
            fromLocation = "Karnataka Silk Filatures Ltd",
            toLocation = "Main Godown (Bin A1)",
            movementDate = "2026-10-12",
            referenceType = "PURCHASE",
            referenceId = "PUR-2026-0001",
            notes = "Accepted delivery batch REC-001"
        ),
        InventoryMovementResponse(
            id = "mov-2",
            movementNumber = "MOV-2026-0102",
            productType = "RAW_SILK",
            movementType = "OUTSOURCE_ISSUE",
            quantityKg = 52.500,
            fromLocation = "Main Godown (Bin A1)",
            toLocation = "Sri Balaji Twisters & Warpers",
            movementDate = "2026-10-14",
            referenceType = "OUTSOURCING",
            referenceId = "JOB-2026-0012",
            notes = "Dispatched for twisting"
        ),
        InventoryMovementResponse(
            id = "mov-3",
            movementNumber = "MOV-2026-0103",
            productType = "SILK_WARP",
            movementType = "PRODUCTION_OUTPUT",
            quantityKg = 38.500,
            fromLocation = "In-House Warping Unit",
            toLocation = "Finished Warp Rack (Rack B)",
            movementDate = "2026-10-16",
            referenceType = "PRODUCTION",
            referenceId = "BATCH-2026-0044",
            notes = "Warp beam 60s x 40s"
        )
    )

    override suspend fun getInventorySummary(): Response<InventorySummaryResponse> {
        if (shouldFail) {
            val errorJson = """{"status":$failureCode,"message":"$failureMessage"}"""
            return Response.error(failureCode, errorJson.toResponseBody("application/json".toMediaTypeOrNull()))
        }
        return Response.success(sampleSummary)
    }

    override suspend fun getExternalWip(): Response<List<ExternalWipItemResponse>> {
        if (shouldFail) {
            val errorJson = """{"status":$failureCode,"message":"$failureMessage"}"""
            return Response.error(failureCode, errorJson.toResponseBody("application/json".toMediaTypeOrNull()))
        }
        return Response.success(sampleWip.toList())
    }

    override suspend fun getMovements(
        productType: String?,
        movementType: String?
    ): Response<List<InventoryMovementResponse>> {
        if (shouldFail) {
            val errorJson = """{"status":$failureCode,"message":"$failureMessage"}"""
            return Response.error(failureCode, errorJson.toResponseBody("application/json".toMediaTypeOrNull()))
        }
        var list = sampleMovements.toList()
        if (productType != null) {
            list = list.filter { it.productType.equals(productType, ignoreCase = true) }
        }
        if (movementType != null) {
            list = list.filter { it.movementType.equals(movementType, ignoreCase = true) }
        }
        return Response.success(list)
    }
}

/**
 * Fake InventoryRepository for ViewModel testing.
 */
class FakeInventoryRepository : InventoryRepository {
    var shouldFail: Boolean = false
    var failureMessage: String = "Inventory repository failure"

    val summary = InventorySummaryResponse(
        rawSilkKg = 324.500,
        koraWarpKg = 85.200,
        silkWarpKg = 42.750,
        vudaWarpKg = 31.000,
        externalWipKg = 82.500
    )

    val externalWip = mutableListOf(
        ExternalWipItemResponse("w1", "m1", "Sri Balaji Twisters", "KORA_WARP", 52.500, "JOB-101", null, "2026-10-14")
    )

    val movements = mutableListOf(
        InventoryMovementResponse("m1", "MOV-001", "RAW_SILK", "RECEIPT", 100.0, "Supplier", "Godown", "2026-10-12", "PURCHASE", "PUR-01", null),
        InventoryMovementResponse("m2", "MOV-002", "RAW_SILK", "OUTSOURCE_ISSUE", 52.5, "Godown", "Twister", "2026-10-14", "OUTSOURCING", "JOB-101", null),
        InventoryMovementResponse("m3", "MOV-003", "SILK_WARP", "PRODUCTION_OUTPUT", 38.5, "Warping", "Rack", "2026-10-16", "PRODUCTION", "BATCH-01", null)
    )

    override suspend fun getInventorySummary(): Result<InventorySummaryResponse> {
        return if (shouldFail) Result.failure(Exception(failureMessage)) else Result.success(summary)
    }

    override suspend fun getExternalWip(): Result<List<ExternalWipItemResponse>> {
        return if (shouldFail) Result.failure(Exception(failureMessage)) else Result.success(externalWip.toList())
    }

    override suspend fun getMovements(
        productType: ProductType?,
        movementType: MovementType?
    ): Result<List<InventoryMovementResponse>> {
        return if (shouldFail) {
            Result.failure(Exception(failureMessage))
        } else {
            var list = movements.toList()
            if (productType != null) {
                list = list.filter { it.productType.equals(productType.name, ignoreCase = true) }
            }
            if (movementType != null) {
                list = list.filter { it.movementType.equals(movementType.name, ignoreCase = true) }
            }
            Result.success(list)
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class InventoryUnitTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Repository Tests ────────────────────────────────────────────────────

    @Test
    fun repository_getInventorySummary_returnsCalculatedTotals() = runTest {
        val fakeApi = FakeInventoryApiService()
        val repo = InventoryRepositoryImpl(fakeApi, Gson())

        val result = repo.getInventorySummary()

        assertTrue(result.isSuccess)
        val summary = result.getOrNull()!!
        assertEquals(324.500, summary.rawSilkKg, 0.001)
        assertEquals(85.200, summary.koraWarpKg, 0.001)
        assertEquals(42.750, summary.silkWarpKg, 0.001)
        assertEquals(31.000, summary.vudaWarpKg, 0.001)
        assertEquals(82.500, summary.externalWipKg, 0.001)

        // Computed metrics
        assertEquals(483.450, summary.totalInHouseStockKg, 0.001)
        assertEquals(565.950, summary.totalStockKg, 0.001)
    }

    @Test
    fun repository_getExternalWip_returnsManufacturerConsignments() = runTest {
        val fakeApi = FakeInventoryApiService()
        val repo = InventoryRepositoryImpl(fakeApi, Gson())

        val result = repo.getExternalWip()

        assertTrue(result.isSuccess)
        val list = result.getOrNull()!!
        assertEquals(2, list.size)
        assertEquals("Sri Balaji Twisters & Warpers", list[0].manufacturerName)
        assertEquals(52.500, list[0].quantityKg, 0.001)
    }

    @Test
    fun repository_getMovements_filtersByProductType() = runTest {
        val fakeApi = FakeInventoryApiService()
        val repo = InventoryRepositoryImpl(fakeApi, Gson())

        val result = repo.getMovements(productType = ProductType.RAW_SILK)

        assertTrue(result.isSuccess)
        val list = result.getOrNull()!!
        assertEquals(2, list.size)
        assertTrue(list.all { it.productType == "RAW_SILK" })
    }

    @Test
    fun repository_getMovements_filtersByMovementType() = runTest {
        val fakeApi = FakeInventoryApiService()
        val repo = InventoryRepositoryImpl(fakeApi, Gson())

        val result = repo.getMovements(movementType = MovementType.PRODUCTION_OUTPUT)

        assertTrue(result.isSuccess)
        val list = result.getOrNull()!!
        assertEquals(1, list.size)
        assertEquals("MOV-2026-0103", list[0].movementNumber)
        assertEquals(MovementType.PRODUCTION_OUTPUT, list[0].typedMovement)
    }

    // ── ViewModel Tests ────────────────────────────────────────────────────

    @Test
    fun inventorySummaryViewModel_loadsSummaryAndExternalWip() = runTest {
        val fakeRepo = FakeInventoryRepository()
        val viewModel = InventorySummaryViewModel(fakeRepo)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        val uiData = (state as UiState.Success).data
        assertEquals(324.500, uiData.summary.rawSilkKg, 0.001)
        assertEquals(1, uiData.externalWipList.size)
        assertEquals("Sri Balaji Twisters", uiData.externalWipList[0].manufacturerName)
    }

    @Test
    fun inventorySummaryViewModel_refreshFetchesLatest() = runTest {
        val fakeRepo = FakeInventoryRepository()
        val viewModel = InventorySummaryViewModel(fakeRepo)
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        assertFalse(viewModel.isRefreshing.value)
        assertTrue(viewModel.uiState.value is UiState.Success)
    }

    @Test
    fun movementHistoryViewModel_searchDebounce() = runTest {
        val fakeRepo = FakeInventoryRepository()
        val viewModel = MovementHistoryViewModel(fakeRepo)
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("Twister")
        advanceTimeBy(350)
        advanceUntilIdle()

        val state = viewModel.uiState.value as UiState.Success
        assertEquals(1, state.data.size)
        assertEquals("MOV-002", state.data[0].movementNumber)
    }

    @Test
    fun movementHistoryViewModel_filterProductType() = runTest {
        val fakeRepo = FakeInventoryRepository()
        val viewModel = MovementHistoryViewModel(fakeRepo)
        advanceUntilIdle()

        viewModel.selectProductType(ProductType.SILK_WARP)
        advanceUntilIdle()

        val state = viewModel.uiState.value as UiState.Success
        assertEquals(1, state.data.size)
        assertEquals("SILK_WARP", state.data[0].productType)
    }

    // ── Domain & Invariant Tests ───────────────────────────────────────────

    @Test
    fun productType_enumCorrectness() {
        assertEquals(ProductType.RAW_SILK, ProductType.fromString("raw_silk"))
        assertEquals(ProductType.KORA_WARP, ProductType.fromString("KORA_WARP"))
        assertEquals(ProductType.SILK_WARP, ProductType.fromString("SILK_WARP"))
        assertEquals(ProductType.VUDA_WARP, ProductType.fromString("VUDA_WARP"))
    }

    @Test
    fun movementType_directionsAndColors() {
        assertTrue(MovementType.RECEIPT.isIncoming)
        assertFalse(MovementType.CONSUMPTION.isIncoming)
        assertTrue(MovementType.PRODUCTION_OUTPUT.isIncoming)
        assertFalse(MovementType.OUTSOURCE_ISSUE.isIncoming)
        assertTrue(MovementType.OUTSOURCE_RECEIPT.isIncoming)
        assertFalse(MovementType.DISPATCH.isIncoming)
    }

    @Test
    fun readOnlyInvariant_noMutationMethodsExposedOnRepository() {
        val repoMethods = InventoryRepository::class.java.declaredMethods.map { it.name }
        // Verify that only get/read methods exist and no create, update, delete or post methods
        assertTrue(repoMethods.all { it.startsWith("get") })
        assertFalse(repoMethods.any { it.contains("create", ignoreCase = true) })
        assertFalse(repoMethods.any { it.contains("update", ignoreCase = true) })
        assertFalse(repoMethods.any { it.contains("delete", ignoreCase = true) })
        assertFalse(repoMethods.any { it.contains("record", ignoreCase = true) })
    }
}
