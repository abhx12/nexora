package com.smartledger.app.feature.reports.domain

import com.smartledger.app.feature.reports.data.dto.CustomerOutstandingReportResponse
import com.smartledger.app.feature.reports.data.dto.ExpenseSummaryReportResponse
import com.smartledger.app.feature.reports.data.dto.SupplierPayablesReportResponse

interface ReportsRepository {
    suspend fun getCustomerOutstandingReport(): Result<CustomerOutstandingReportResponse>
    suspend fun getSupplierPayablesReport(): Result<SupplierPayablesReportResponse>
    suspend fun getExpenseSummaryReport(startDate: String? = null, endDate: String? = null): Result<ExpenseSummaryReportResponse>
}
