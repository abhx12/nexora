package com.smartledger.app.feature.outsourcing.data.api

import com.smartledger.app.feature.outsourcing.data.dto.CreateOutsourcingJobRequest
import com.smartledger.app.feature.outsourcing.data.dto.IssueMaterialRequest
import com.smartledger.app.feature.outsourcing.data.dto.OutsourcingJobResponse
import com.smartledger.app.feature.outsourcing.data.dto.ReceiveOutsourcingRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface OutsourcingApiService {

    @GET("api/v1/outsourcing-jobs")
    suspend fun getOutsourcingJobs(
        @Query("status") status: String? = null
    ): List<OutsourcingJobResponse>

    @GET("api/v1/outsourcing-jobs/{id}")
    suspend fun getOutsourcingJobById(
        @Path("id") id: String
    ): OutsourcingJobResponse

    @POST("api/v1/outsourcing-jobs")
    suspend fun createOutsourcingJob(
        @Body request: CreateOutsourcingJobRequest
    ): OutsourcingJobResponse

    @POST("api/v1/outsourcing-jobs/{id}/issue")
    suspend fun issueMaterial(
        @Path("id") id: String,
        @Body request: IssueMaterialRequest
    ): OutsourcingJobResponse

    @POST("api/v1/outsourcing-jobs/{id}/receive")
    suspend fun receiveOutput(
        @Path("id") id: String,
        @Body request: ReceiveOutsourcingRequest
    ): OutsourcingJobResponse
}
