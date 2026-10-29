package com.smartledger.app.feature.reports.data.api

import com.smartledger.app.feature.reports.data.dto.CustomerOutstandingReportResponse
import com.smartledger.app.feature.reports.data.dto.ExpenseSummaryReportResponse
import com.smartledger.app.feature.reports.data.dto.SupplierPayablesReportResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ReportsApiService {

    @GET("api/v1/reports/customer-outstanding")
    suspend fun getCustomerOutstandingReport(): CustomerOutstandingReportResponse

    @GET("api/v1/reports/supplier-payables")
    suspend fun getSupplierPayablesReport(): SupplierPayablesReportResponse

    @GET("api/v1/reports/expenses")
    suspend fun getExpenseSummaryReport(
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null
    ): ExpenseSummaryReportResponse
}
