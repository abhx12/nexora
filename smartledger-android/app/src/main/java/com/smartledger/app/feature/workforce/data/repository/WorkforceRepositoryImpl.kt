package com.smartledger.app.feature.workforce.data.repository

import com.google.gson.Gson
import com.smartledger.app.BuildConfig
import com.smartledger.app.feature.workforce.data.api.WorkforceApiService
import com.smartledger.app.feature.workforce.data.dto.CreateWorkRecordRequest
import com.smartledger.app.feature.workforce.data.dto.WorkRecordResponse
import com.smartledger.app.feature.workforce.data.dto.WorkerPayableResponse
import com.smartledger.app.feature.workforce.domain.WorkforceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkforceRepositoryImpl @Inject constructor(
    private val apiService: WorkforceApiService,
    private val gson: Gson
) : WorkforceRepository {

    private val mockRecords = mutableListOf(
        WorkRecordResponse(
            id = "wrk-rec-1",
            recordNumber = "WRK-2026-0001",
            workerId = "worker-1",
            workerName = "Mani Kumar",
            workDate = "2026-10-18",
            workType = "WARPING",
            quantityKg = 45.5,
            hours = null,
            rate = 120.0,
            totalAmount = 5460.0,
            notes = "8-ply pure silk warp preparation",
            createdAt = "2026-10-18T17:00:00Z"
        ),
        WorkRecordResponse(
            id = "wrk-rec-2",
            recordNumber = "WRK-2026-0002",
            workerId = "worker-2",
            workerName = "Selvam R.",
            workDate = "2026-10-18",
            workType = "ROLLING",
            quantityKg = null,
            hours = 8.0,
            rate = 85.0,
            totalAmount = 680.0,
            notes = "Beam loading and machine maintenance",
            createdAt = "2026-10-18T18:00:00Z"
        ),
        WorkRecordResponse(
            id = "wrk-rec-3",
            recordNumber = "WRK-2026-0003",
            workerId = "worker-1",
            workerName = "Mani Kumar",
            workDate = "2026-10-19",
            workType = "WARPING",
            quantityKg = 38.0,
            hours = null,
            rate = 120.0,
            totalAmount = 4560.0,
            notes = "Kora warp sizing and creel setup",
            createdAt = "2026-10-19T17:30:00Z"
        )
    )

    private val mockPayables = mutableMapOf(
        "worker-1" to WorkerPayableResponse(
            workerId = "worker-1",
            workerName = "Mani Kumar",
            totalEarned = 10020.0,
            totalPaid = 6000.0,
            outstandingPayable = 4020.0,
            recordsCount = 2
        ),
        "worker-2" to WorkerPayableResponse(
            workerId = "worker-2",
            workerName = "Selvam R.",
            totalEarned = 5440.0,
            totalPaid = 4000.0,
            outstandingPayable = 1440.0,
            recordsCount = 1
        )
    )

    override suspend fun getWorkRecords(workerId: String?): Result<List<WorkRecordResponse>> =
        withContext(Dispatchers.IO) {
            if (BuildConfig.USE_MOCK) {
                val filtered = if (workerId.isNullOrBlank()) {
                    mockRecords.toList()
                } else {
                    mockRecords.filter { it.workerId == workerId }
                }
                return@withContext Result.success(filtered)
            }

            try {
                val result = apiService.getWorkRecords(workerId)
                Result.success(result)
            } catch (e: HttpException) {
                Result.failure(e)
            } catch (e: IOException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun createWorkRecord(request: CreateWorkRecordRequest): Result<WorkRecordResponse> =
        withContext(Dispatchers.IO) {
            if (request.rate <= 0) {
                return@withContext Result.failure(IllegalArgumentException("Rate must be greater than 0"))
            }

            val totalAmount = when (request.workType) {
                "WARPING" -> {
                    val qty = request.quantityKg ?: 0.0
                    if (qty <= 0) {
                        return@withContext Result.failure(
                            IllegalArgumentException("Warping work requires quantity in kg (> 0)")
                        )
                    }
                    qty * request.rate
                }
                "ROLLING" -> {
                    val hrs = request.hours ?: 0.0
                    if (hrs <= 0) {
                        return@withContext Result.failure(
                            IllegalArgumentException("Rolling work requires hours (> 0)")
                        )
                    }
                    hrs * request.rate
                }
                else -> {
                    return@withContext Result.failure(
                        IllegalArgumentException("Invalid work type: ${request.workType}")
                    )
                }
            }

            if (BuildConfig.USE_MOCK) {
                val nextNum = mockRecords.size + 1
                val recordNumber = "WRK-2026-%04d".format(nextNum)
                val workerName = if (request.workerId == "worker-1") "Mani Kumar" else "Selvam R."

                val newRecord = WorkRecordResponse(
                    id = UUID.randomUUID().toString(),
                    recordNumber = recordNumber,
                    workerId = request.workerId,
                    workerName = workerName,
                    workDate = request.workDate,
                    workType = request.workType,
                    quantityKg = request.quantityKg,
                    hours = request.hours,
                    rate = request.rate,
                    totalAmount = totalAmount,
                    notes = request.notes,
                    createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
                )
                mockRecords.add(0, newRecord)

                // Update payable
                val existing = mockPayables[request.workerId] ?: WorkerPayableResponse(
                    workerId = request.workerId,
                    workerName = workerName,
                    totalEarned = 0.0,
                    totalPaid = 0.0,
                    outstandingPayable = 0.0,
                    recordsCount = 0
                )
                val updatedEarned = existing.totalEarned + totalAmount
                val updatedOutstanding = existing.outstandingPayable + totalAmount
                mockPayables[request.workerId] = existing.copy(
                    totalEarned = updatedEarned,
                    outstandingPayable = updatedOutstanding,
                    recordsCount = existing.recordsCount + 1
                )

                return@withContext Result.success(newRecord)
            }

            try {
                val result = apiService.createWorkRecord(request)
                Result.success(result)
            } catch (e: HttpException) {
                Result.failure(e)
            } catch (e: IOException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun getWorkerPayable(workerId: String): Result<WorkerPayableResponse> =
        withContext(Dispatchers.IO) {
            if (BuildConfig.USE_MOCK) {
                val payable = mockPayables[workerId] ?: WorkerPayableResponse(
                    workerId = workerId,
                    workerName = "Worker $workerId",
                    totalEarned = 0.0,
                    totalPaid = 0.0,
                    outstandingPayable = 0.0,
                    recordsCount = 0
                )
                return@withContext Result.success(payable)
            }

            try {
                val result = apiService.getWorkerPayable(workerId)
                Result.success(result)
            } catch (e: HttpException) {
                Result.failure(e)
            } catch (e: IOException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun getAllWorkersPayable(): Result<List<WorkerPayableResponse>> =
        withContext(Dispatchers.IO) {
            if (BuildConfig.USE_MOCK) {
                return@withContext Result.success(mockPayables.values.toList())
            }

            try {
                val result = apiService.getAllWorkersPayable()
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
