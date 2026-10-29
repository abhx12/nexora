package com.smartledger.app.feature.reports

import com.google.gson.Gson
import com.smartledger.app.core.util.UiState
import com.smartledger.app.feature.reports.data.api.ReportsApiService
import com.smartledger.app.feature.reports.data.dto.CustomerOutstandingItem
import com.smartledger.app.feature.reports.data.dto.CustomerOutstandingReportResponse
import com.smartledger.app.feature.reports.data.dto.ExpenseSummaryCategoryItem
import com.smartledger.app.feature.reports.data.dto.ExpenseSummaryReportResponse
import com.smartledger.app.feature.reports.data.dto.SupplierPayableItem
import com.smartledger.app.feature.reports.data.dto.SupplierPayablesReportResponse
import com.smartledger.app.feature.reports.data.repository.ReportsRepositoryImpl
import com.smartledger.app.feature.reports.domain.ReportsRepository
import com.smartledger.app.feature.reports.presentation.viewmodel.ReportsViewModel
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

class FakeReportsApiService : ReportsApiService {
    override suspend fun getCustomerOutstandingReport(): CustomerOutstandingReportResponse =
        CustomerOutstandingReportResponse(
            items = listOf(
                CustomerOutstandingItem("c1", "Customer 1", "CUST-001", null, 100000.0, 40000.0, 60000.0, 1, "2026-10-01")
            ),
            totalOutstanding = 60000.0
        )

    override suspend fun getSupplierPayablesReport(): SupplierPayablesReportResponse =
        SupplierPayablesReportResponse(
            items = listOf(
                SupplierPayableItem("s1", "Supplier 1", "SUPP-001", null, 80000.0, 30000.0, 50000.0, true, "2026-10-10")
            ),
            totalPayable = 50000.0,
            overduePayable = 50000.0
        )

    override suspend fun getExpenseSummaryReport(startDate: String?, endDate: String?): ExpenseSummaryReportResponse =
        ExpenseSummaryReportResponse(
            categories = listOf(ExpenseSummaryCategoryItem("TRANSPORT", 10000.0, 100.0)),
            totalExpenses = 10000.0
        )
}

class FakeReportsRepository : ReportsRepository {
    override suspend fun getCustomerOutstandingReport(): Result<CustomerOutstandingReportResponse> =
        Result.success(
            CustomerOutstandingReportResponse(
                items = listOf(CustomerOutstandingItem("c1", "Cust 1", "C-01", null, 100000.0, 50000.0, 50000.0, 1, "2026-10-01")),
                totalOutstanding = 50000.0
            )
        )

    override suspend fun getSupplierPayablesReport(): Result<SupplierPayablesReportResponse> =
        Result.success(
            SupplierPayablesReportResponse(
                items = listOf(SupplierPayableItem("s1", "Supp 1", "S-01", null, 80000.0, 30000.0, 50000.0, true, "2026-10-10")),
                totalPayable = 50000.0,
                overduePayable = 50000.0
            )
        )

    override suspend fun getExpenseSummaryReport(startDate: String?, endDate: String?): Result<ExpenseSummaryReportResponse> =
        Result.success(
            ExpenseSummaryReportResponse(
                categories = listOf(ExpenseSummaryCategoryItem("RAW_MATERIAL", 10000.0, 100.0)),
                totalExpenses = 10000.0
            )
        )
}

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsUnitTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var apiService: FakeReportsApiService
    private lateinit var repository: ReportsRepositoryImpl

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        apiService = FakeReportsApiService()
        repository = ReportsRepositoryImpl(apiService, Gson())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testCustomerOutstandingReport() = runTest {
        val result = repository.getCustomerOutstandingReport()
        assertTrue(result.isSuccess)
        val report = result.getOrThrow()
        assertTrue(report.items.isNotEmpty())
        assertEquals(625000.0, report.totalOutstanding, 0.001)
    }

    @Test
    fun testSupplierPayablesReport_HasOverdueFlagging() = runTest {
        val result = repository.getSupplierPayablesReport()
        assertTrue(result.isSuccess)
        val report = result.getOrThrow()
        assertTrue(report.items.any { it.isOverdue })
        assertTrue(report.overduePayable > 0)
        assertEquals(410000.0, report.totalPayable, 0.001)
    }

    @Test
    fun testExpenseSummaryReport() = runTest {
        val result = repository.getExpenseSummaryReport("2026-10-01", "2026-10-21")
        assertTrue(result.isSuccess)
        val report = result.getOrThrow()
        assertTrue(report.categories.isNotEmpty())
        assertTrue(report.totalExpenses > 0)
    }

    @Test
    fun testReportsViewModel() = runTest {
        val fakeRepo = FakeReportsRepository()
        val viewModel = ReportsViewModel(fakeRepo)
        advanceUntilIdle()

        assertTrue(viewModel.customerOutstandingState.value is UiState.Success)
        assertTrue(viewModel.supplierPayablesState.value is UiState.Success)
        assertTrue(viewModel.expenseSummaryState.value is UiState.Success)
    }
}
