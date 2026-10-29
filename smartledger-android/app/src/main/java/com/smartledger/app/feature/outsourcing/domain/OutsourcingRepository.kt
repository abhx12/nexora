package com.smartledger.app.feature.outsourcing.domain

import com.smartledger.app.feature.outsourcing.data.dto.CreateOutsourcingJobRequest
import com.smartledger.app.feature.outsourcing.data.dto.IssueMaterialRequest
import com.smartledger.app.feature.outsourcing.data.dto.OutsourcingJobResponse
import com.smartledger.app.feature.outsourcing.data.dto.ReceiveOutsourcingRequest

interface OutsourcingRepository {
    suspend fun getOutsourcingJobs(status: String? = null): Result<List<OutsourcingJobResponse>>
    suspend fun getOutsourcingJobById(id: String): Result<OutsourcingJobResponse>
    suspend fun createOutsourcingJob(request: CreateOutsourcingJobRequest): Result<OutsourcingJobResponse>
    suspend fun issueMaterial(id: String, request: IssueMaterialRequest): Result<OutsourcingJobResponse>
    suspend fun receiveOutput(id: String, request: ReceiveOutsourcingRequest): Result<OutsourcingJobResponse>
    suspend fun getAvailableRawSilkKg(): Double
    suspend fun getExternalWipKg(): Double
}
