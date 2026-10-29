package com.smartledger.app.feature.dashboard

import com.google.gson.Gson
import com.smartledger.app.core.util.UiState
import com.smartledger.app.feature.dashboard.data.api.DashboardApiService
import com.smartledger.app.feature.dashboard.data.dto.DashboardSummaryResponse
import com.smartledger.app.feature.dashboard.data.repository.DashboardRepositoryImpl
import com.smartledger.app.feature.dashboard.domain.DashboardRepository
import com.smartledger.app.feature.dashboard.presentation.viewmodel.DashboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

/**
 * Fake DashboardApiService for unit tests.
 */
class FakeDashboardApiService : DashboardApiService {
    var shouldFail: Boolean = false
    var failureCode: Int = 500
    var failureMessage: String = "Internal server error"

    var mockResponse = DashboardSummaryResponse(
        cash = 125000.00,
        bank = 850000.00,
        rawSilkKg = 324.500,
        customerOutstanding = 625000.00,
        supplierPayable = 410000.00,
        externalWipKg = 82.500,
        pendingOrders = 12,
        pendingProduction = 3,
        pendingOutsourcing = 5
    )

    override suspend fun getDashboardSummary(): Response<DashboardSummaryResponse> {
        return if (shouldFail) {
            val errorJson = """{"status":$failureCode,"message":"$failureMessage"}"""
            Response.error(
                failureCode,
                errorJson.toResponseBody("application/json".toMediaTypeOrNull())
            )
        } else {
            Response.success(mockResponse)
        }
    }
}

/**
 * Fake DashboardRepository for ViewModel tests.
 */
class FakeDashboardRepository : DashboardRepository {
    var shouldFail: Boolean = false
    var failureMessage: String = "Server unavailable"
    var callCount: Int = 0

    var summary = DashboardSummaryResponse(
        cash = 100000.0,
        bank = 500000.0,
        rawSilkKg = 250.0,
        customerOutstanding = 300000.0,
        supplierPayable = 200000.0,
        externalWipKg = 50.0,
        pendingOrders = 8,
        pendingProduction = 2,
        pendingOutsourcing = 4
    )

    override suspend fun getDashboardSummary(): Result<DashboardSummaryResponse> {
        callCount++
        return if (shouldFail) {
            Result.failure(Exception(failureMessage))
        } else {
            Result.success(summary)
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardUnitTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Repository Tests ──────────────────────────────────────────────────

    @Test
    fun repository_getSummary_success() = runTest {
        val fakeApi = FakeDashboardApiService()
        val repository = DashboardRepositoryImpl(fakeApi, Gson())

        val result = repository.getDashboardSummary()

        assertTrue(result.isSuccess)
        val data = result.getOrNull()!!
        assertEquals(125000.00, data.cash, 0.001)
        assertEquals(850000.00, data.bank, 0.001)
        assertEquals(324.500, data.rawSilkKg, 0.001)
        assertEquals(625000.00, data.customerOutstanding, 0.001)
        assertEquals(410000.00, data.supplierPayable, 0.001)
        assertEquals(82.500, data.externalWipKg, 0.001)
        assertEquals(12, data.pendingOrders)
        assertEquals(3, data.pendingProduction)
        assertEquals(5, data.pendingOutsourcing)
    }

    @Test
    fun repository_getSummary_serverError_returnsFailure() = runTest {
        val fakeApi = FakeDashboardApiService().apply {
            shouldFail = true
            failureCode = 500
            failureMessage = "Database error"
        }
        val repository = DashboardRepositoryImpl(fakeApi, Gson())

        val result = repository.getDashboardSummary()

        assertTrue(result.isFailure)
        assertEquals("Database error", result.exceptionOrNull()?.message)
    }

    // ── ViewModel Tests ────────────────────────────────────────────────────

    @Test
    fun viewModel_initialLoad_success() = runTest {
        val fakeRepo = FakeDashboardRepository()
        val viewModel = DashboardViewModel(fakeRepo)

        // Initially loading
        assertEquals(UiState.Loading, viewModel.uiState.value)

        advanceUntilIdle()

        // Succeeded
        assertTrue(viewModel.uiState.value is UiState.Success)
        val successState = viewModel.uiState.value as UiState.Success
        assertEquals(100000.0, successState.data.cash, 0.001)
        assertEquals(8, successState.data.pendingOrders)
        assertFalse(viewModel.isRefreshing.value)
    }

    @Test
    fun viewModel_initialLoad_failure() = runTest {
        val fakeRepo = FakeDashboardRepository().apply {
            shouldFail = true
            failureMessage = "Connection timeout"
        }
        val viewModel = DashboardViewModel(fakeRepo)

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is UiState.Error)
        val errorState = viewModel.uiState.value as UiState.Error
        assertEquals("Connection timeout", errorState.message)
        assertFalse(viewModel.isRefreshing.value)
    }

    @Test
    fun viewModel_refresh_updatesData() = runTest {
        val fakeRepo = FakeDashboardRepository()
        val viewModel = DashboardViewModel(fakeRepo)

        advanceUntilIdle()
        assertEquals(1, fakeRepo.callCount)

        // Update data in repo and call refresh()
        fakeRepo.summary = fakeRepo.summary.copy(pendingOrders = 15)
        viewModel.refresh()

        advanceUntilIdle()

        assertEquals(2, fakeRepo.callCount)
        val successState = viewModel.uiState.value as UiState.Success
        assertEquals(15, successState.data.pendingOrders)
        assertFalse(viewModel.isRefreshing.value)
    }
}
