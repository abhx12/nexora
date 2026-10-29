package com.smartledger.app.feature.reports.data.repository

import com.google.gson.Gson
import com.smartledger.app.BuildConfig
import com.smartledger.app.feature.reports.data.api.ReportsApiService
import com.smartledger.app.feature.reports.data.dto.CustomerOutstandingItem
import com.smartledger.app.feature.reports.data.dto.CustomerOutstandingReportResponse
import com.smartledger.app.feature.reports.data.dto.ExpenseSummaryCategoryItem
import com.smartledger.app.feature.reports.data.dto.ExpenseSummaryReportResponse
import com.smartledger.app.feature.reports.data.dto.SupplierPayableItem
import com.smartledger.app.feature.reports.data.dto.SupplierPayablesReportResponse
import com.smartledger.app.feature.reports.domain.ReportsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportsRepositoryImpl @Inject constructor(
    private val apiService: ReportsApiService,
    private val gson: Gson
) : ReportsRepository {

    // Mock customer receivables aligned with dashboard KPI: ₹625,000.00
    private val mockCustomers = listOf(
        CustomerOutstandingItem(
            customerId = "cust-1",
            customerName = "Sri Lakshmi Silks",
            customerCode = "CUST-001",
            phone = "9842100001",
            totalOrderValue = 850000.0,
            totalPaid = 500000.0,
            outstandingAmount = 350000.0,
            pendingOrdersCount = 2,
            oldestPendingOrderDate = "2026-09-28"
        ),
        CustomerOutstandingItem(
            customerId = "cust-2",
            customerName = "Venkateshwara Tex",
            customerCode = "CUST-002",
            phone = "9842100002",
            totalOrderValue = 420000.0,
            totalPaid = 245000.0,
            outstandingAmount = 175000.0,
            pendingOrdersCount = 1,
            oldestPendingOrderDate = "2026-10-04"
        ),
        CustomerOutstandingItem(
            customerId = "cust-3",
            customerName = "Royal Silk Emporium",
            customerCode = "CUST-003",
            phone = "9842100003",
            totalOrderValue = 280000.0,
            totalPaid = 180000.0,
            outstandingAmount = 100000.0,
            pendingOrdersCount = 1,
            oldestPendingOrderDate = "2026-10-12"
        )
    )

    // Mock supplier payables aligned with dashboard KPI: ₹410,000.00
    private val mockSuppliers = listOf(
        SupplierPayableItem(
            supplierId = "supp-1",
            supplierName = "Karnataka Silk Cocoon Traders",
            supplierCode = "SUPP-001",
            phone = "9443100001",
            totalPurchasedValue = 650000.0,
            totalPaid = 390000.0,
            outstandingPayable = 260000.0,
            isOverdue = true, // Highlighted overdue row
            dueDate = "2026-10-15"
        ),
        SupplierPayableItem(
            supplierId = "supp-2",
            supplierName = "Ramanagara Cocoon Market Assn",
            supplierCode = "SUPP-002",
            phone = "9443100002",
            totalPurchasedValue = 350000.0,
            totalPaid = 200000.0,
            outstandingPayable = 150000.0,
            isOverdue = false,
            dueDate = "2026-10-28"
        )
    )

    override suspend fun getCustomerOutstandingReport(): Result<CustomerOutstandingReportResponse> =
        withContext(Dispatchers.IO) {
            if (BuildConfig.USE_MOCK) {
                val total = mockCustomers.sumOf { it.outstandingAmount }
                return@withContext Result.success(
                    CustomerOutstandingReportResponse(
                        items = mockCustomers,
                        totalOutstanding = total,
                        generatedAt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(Date())
                    )
                )
            }

            try {
                val result = apiService.getCustomerOutstandingReport()
                Result.success(result)
            } catch (e: HttpException) {
                Result.failure(e)
            } catch (e: IOException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun getSupplierPayablesReport(): Result<SupplierPayablesReportResponse> =
        withContext(Dispatchers.IO) {
            if (BuildConfig.USE_MOCK) {
                val total = mockSuppliers.sumOf { it.outstandingPayable }
                val overdue = mockSuppliers.filter { it.isOverdue }.sumOf { it.outstandingPayable }
                return@withContext Result.success(
                    SupplierPayablesReportResponse(
                        items = mockSuppliers,
                        totalPayable = total,
                        overduePayable = overdue,
                        generatedAt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(Date())
                    )
                )
            }

            try {
                val result = apiService.getSupplierPayablesReport()
                Result.success(result)
            } catch (e: HttpException) {
                Result.failure(e)
            } catch (e: IOException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun getExpenseSummaryReport(startDate: String?, endDate: String?): Result<ExpenseSummaryReportResponse> =
        withContext(Dispatchers.IO) {
            if (BuildConfig.USE_MOCK) {
                val categories = listOf(
                    ExpenseSummaryCategoryItem(category = "RAW_MATERIAL", amount = 145000.0, percentage = 68.4),
                    ExpenseSummaryCategoryItem(category = "ELECTRICITY", amount = 28500.0, percentage = 13.4),
                    ExpenseSummaryCategoryItem(category = "LABOR_WAGES", amount = 22000.0, percentage = 10.4),
                    ExpenseSummaryCategoryItem(category = "TRANSPORT", amount = 9800.0, percentage = 4.6),
                    ExpenseSummaryCategoryItem(category = "MAINTENANCE", amount = 6800.0, percentage = 3.2)
                )
                val total = categories.sumOf { it.amount }
                return@withContext Result.success(
                    ExpenseSummaryReportResponse(
                        categories = categories,
                        totalExpenses = total,
                        startDate = startDate ?: "2026-10-01",
                        endDate = endDate ?: "2026-10-21"
                    )
                )
            }

            try {
                val result = apiService.getExpenseSummaryReport(startDate, endDate)
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
