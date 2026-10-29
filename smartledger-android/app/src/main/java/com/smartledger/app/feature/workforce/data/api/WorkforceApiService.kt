package com.smartledger.app.feature.workforce.data.api

import com.smartledger.app.feature.workforce.data.dto.CreateWorkRecordRequest
import com.smartledger.app.feature.workforce.data.dto.WorkRecordResponse
import com.smartledger.app.feature.workforce.data.dto.WorkerPayableResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface WorkforceApiService {

    @GET("api/v1/work-records")
    suspend fun getWorkRecords(
        @Query("workerId") workerId: String? = null
    ): List<WorkRecordResponse>

    @POST("api/v1/work-records")
    suspend fun createWorkRecord(
        @Body request: CreateWorkRecordRequest
    ): WorkRecordResponse

    @GET("api/v1/workers/{id}/payable")
    suspend fun getWorkerPayable(
        @Path("id") workerId: String
    ): WorkerPayableResponse

    @GET("api/v1/workers/payables")
    suspend fun getAllWorkersPayable(): List<WorkerPayableResponse>
}
