package com.smartledger.app.feature.workforce.domain

import com.smartledger.app.feature.workforce.data.dto.CreateWorkRecordRequest
import com.smartledger.app.feature.workforce.data.dto.WorkRecordResponse
import com.smartledger.app.feature.workforce.data.dto.WorkerPayableResponse

interface WorkforceRepository {
    suspend fun getWorkRecords(workerId: String? = null): Result<List<WorkRecordResponse>>
    suspend fun createWorkRecord(request: CreateWorkRecordRequest): Result<WorkRecordResponse>
    suspend fun getWorkerPayable(workerId: String): Result<WorkerPayableResponse>
    suspend fun getAllWorkersPayable(): Result<List<WorkerPayableResponse>>
}
