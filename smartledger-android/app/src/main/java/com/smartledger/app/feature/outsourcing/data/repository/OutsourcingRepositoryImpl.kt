package com.smartledger.app.feature.outsourcing.data.repository

import com.google.gson.Gson
import com.smartledger.app.BuildConfig
import com.smartledger.app.core.util.ApiError
import com.smartledger.app.core.util.ApiErrorCode
import com.smartledger.app.feature.outsourcing.data.api.OutsourcingApiService
import com.smartledger.app.feature.outsourcing.data.dto.CreateOutsourcingJobRequest
import com.smartledger.app.feature.outsourcing.data.dto.IssueMaterialRequest
import com.smartledger.app.feature.outsourcing.data.dto.OutsourcingJobResponse
import com.smartledger.app.feature.outsourcing.data.dto.ReceiveOutsourcingRequest
import com.smartledger.app.feature.outsourcing.domain.OutsourcingRepository
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
import kotlin.math.abs

@Singleton
class OutsourcingRepositoryImpl @Inject constructor(
    private val apiService: OutsourcingApiService,
    private val gson: Gson
) : OutsourcingRepository {

    // Aligned with Dashboard KPIs
    private var availableRawSilkKg: Double = 324.500
    private var externalWipKg: Double = 82.500

    private val mockJobs = mutableListOf(
        OutsourcingJobResponse(
            id = "job-1",
            jobNumber = "OUT-2026-0001",
            manufacturerId = "mfr-1",
            manufacturerName = "Sri Murugan Twisting Mills",
            orderId = "order-1",
            orderNumber = "ORD-2026-0001",
            productType = "SILK_WARP",
            status = "COMPLETED",
            plannedWeightKg = 60.0,
            issuedWeightKg = 60.0,
            receivedWeightKg = 58.2,
            wastageWeightKg = 1.8,
            issueDate = "2026-10-05",
            expectedReturnDate = "2026-10-12",
            completionDate = "2026-10-11",
            ratePerKg = 180.0,
            totalCost = 10800.0,
            notes = "Completed ahead of schedule. Standard quality.",
            createdAt = "2026-10-05T09:00:00Z"
        ),
        OutsourcingJobResponse(
            id = "job-2",
            jobNumber = "OUT-2026-0002",
            manufacturerId = "mfr-2",
            manufacturerName = "Balaji Weaving & Warping",
            orderId = "order-2",
            orderNumber = "ORD-2026-0002",
            productType = "KORA_WARP",
            status = "MATERIAL_ISSUED",
            plannedWeightKg = 82.5,
            issuedWeightKg = 82.5,
            receivedWeightKg = null,
            wastageWeightKg = null,
            issueDate = "2026-10-14",
            expectedReturnDate = "2026-10-24",
            completionDate = null,
            ratePerKg = 210.0,
            totalCost = null,
            notes = "High-twist kora job. In process at manufacturer.",
            createdAt = "2026-10-14T11:30:00Z"
        ),
        OutsourcingJobResponse(
            id = "job-3",
            jobNumber = "OUT-2026-0003",
            manufacturerId = "mfr-1",
            manufacturerName = "Sri Murugan Twisting Mills",
            orderId = null,
            orderNumber = null,
            productType = "VUDA_WARP",
            status = "CREATED",
            plannedWeightKg = 45.0,
            issuedWeightKg = null,
            receivedWeightKg = null,
            wastageWeightKg = null,
            issueDate = null,
            expectedReturnDate = "2026-10-28",
            completionDate = null,
            ratePerKg = 195.0,
            totalCost = null,
            notes = "Scheduled dispatch for next week.",
            createdAt = "2026-10-19T15:00:00Z"
        )
    )

    override suspend fun getAvailableRawSilkKg(): Double = availableRawSilkKg

    override suspend fun getExternalWipKg(): Double = externalWipKg

    override suspend fun getOutsourcingJobs(status: String?): Result<List<OutsourcingJobResponse>> =
        withContext(Dispatchers.IO) {
            if (BuildConfig.USE_MOCK) {
                val filtered = if (status.isNullOrBlank() || status.equals("ALL", ignoreCase = true)) {
                    mockJobs.toList()
                } else {
                    mockJobs.filter { it.status.equals(status, ignoreCase = true) }
                }
                return@withContext Result.success(filtered)
            }

            try {
                val result = apiService.getOutsourcingJobs(status)
                Result.success(result)
            } catch (e: HttpException) {
                Result.failure(e)
            } catch (e: IOException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun getOutsourcingJobById(id: String): Result<OutsourcingJobResponse> =
        withContext(Dispatchers.IO) {
            if (BuildConfig.USE_MOCK) {
                val job = mockJobs.find { it.id == id }
                return@withContext if (job != null) {
                    Result.success(job)
                } else {
                    Result.failure(NoSuchElementException("Job with id $id not found"))
                }
            }

            try {
                val result = apiService.getOutsourcingJobById(id)
                Result.success(result)
            } catch (e: HttpException) {
                Result.failure(e)
            } catch (e: IOException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun createOutsourcingJob(request: CreateOutsourcingJobRequest): Result<OutsourcingJobResponse> =
        withContext(Dispatchers.IO) {
            if (request.plannedWeightKg <= 0) {
                return@withContext Result.failure(
                    IllegalArgumentException("Planned weight must be greater than 0 kg")
                )
            }

            if (BuildConfig.USE_MOCK) {
                val nextNum = mockJobs.size + 1
                val jobNumber = "OUT-2026-%04d".format(nextNum)
                val mfrName = if (request.manufacturerId == "mfr-1") {
                    "Sri Murugan Twisting Mills"
                } else {
                    "Balaji Weaving & Warping"
                }

                val newJob = OutsourcingJobResponse(
                    id = UUID.randomUUID().toString(),
                    jobNumber = jobNumber,
                    manufacturerId = request.manufacturerId,
                    manufacturerName = mfrName,
                    orderId = request.orderId,
                    orderNumber = request.orderId?.let { "ORD-2026-0001" },
                    productType = request.productType,
                    status = "CREATED",
                    plannedWeightKg = request.plannedWeightKg,
                    expectedReturnDate = request.expectedReturnDate,
                    ratePerKg = request.ratePerKg,
                    notes = request.notes,
                    createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
                )

                mockJobs.add(0, newJob)
                return@withContext Result.success(newJob)
            }

            try {
                val result = apiService.createOutsourcingJob(request)
                Result.success(result)
            } catch (e: HttpException) {
                Result.failure(e)
            } catch (e: IOException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun issueMaterial(id: String, request: IssueMaterialRequest): Result<OutsourcingJobResponse> =
        withContext(Dispatchers.IO) {
            if (request.issuedWeightKg <= 0) {
                return@withContext Result.failure(
                    IllegalArgumentException("Issued weight must be greater than 0 kg")
                )
            }

            if (request.issuedWeightKg > availableRawSilkKg) {
                val err = ApiError(
                    status = 409,
                    code = ApiErrorCode.INSUFFICIENT_INVENTORY,
                    message = "Only %.3f kg of raw silk is available, but %.3f kg required.".format(
                        availableRawSilkKg, request.issuedWeightKg
                    )
                )
                return@withContext Result.failure(IllegalStateException(err.message))
            }

            if (BuildConfig.USE_MOCK) {
                val index = mockJobs.indexOfFirst { it.id == id }
                if (index == -1) {
                    return@withContext Result.failure(NoSuchElementException("Job not found"))
                }

                val current = mockJobs[index]
                if (current.status != "CREATED") {
                    return@withContext Result.failure(
                        IllegalStateException("Cannot issue material: Job is in ${current.status} status")
                    )
                }

                // Inventory updates: raw stock drops, external WIP increases
                availableRawSilkKg -= request.issuedWeightKg
                externalWipKg += request.issuedWeightKg

                val updated = current.copy(
                    status = "MATERIAL_ISSUED",
                    issuedWeightKg = request.issuedWeightKg,
                    issueDate = request.issueDate,
                    notes = if (request.notes.isNullOrBlank()) current.notes else "${current.notes ?: ""}\n${request.notes}"
                )
                mockJobs[index] = updated
                return@withContext Result.success(updated)
            }

            try {
                val result = apiService.issueMaterial(id, request)
                Result.success(result)
            } catch (e: HttpException) {
                Result.failure(e)
            } catch (e: IOException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun receiveOutput(id: String, request: ReceiveOutsourcingRequest): Result<OutsourcingJobResponse> =
        withContext(Dispatchers.IO) {
            if (request.receivedWeightKg <= 0) {
                return@withContext Result.failure(
                    IllegalArgumentException("Received weight must be greater than 0 kg")
                )
            }

            if (BuildConfig.USE_MOCK) {
                val index = mockJobs.indexOfFirst { it.id == id }
                if (index == -1) {
                    return@withContext Result.failure(NoSuchElementException("Job not found"))
                }

                val current = mockJobs[index]
                if (current.status != "MATERIAL_ISSUED" && current.status != "PARTIALLY_RECEIVED") {
                    return@withContext Result.failure(
                        IllegalStateException("Cannot receive output: Job is in ${current.status} status")
                    )
                }

                val issued = current.issuedWeightKg ?: current.plannedWeightKg
                val totalProcessed = request.receivedWeightKg + request.wastageWeightKg
                val difference = abs(issued - totalProcessed)

                // Tolerance check (within 10% or 2kg)
                if (totalProcessed > issued * 1.15) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Received + Wastage (%.3f kg) significantly exceeds issued material (%.3f kg)".format(
                            totalProcessed, issued
                        ))
                    )
                }

                // Inventory: External WIP decreases
                externalWipKg = (externalWipKg - issued).coerceAtLeast(0.0)

                val cost = current.ratePerKg?.let { it * request.receivedWeightKg }

                val updated = current.copy(
                    status = "COMPLETED",
                    receivedWeightKg = request.receivedWeightKg,
                    wastageWeightKg = request.wastageWeightKg,
                    completionDate = request.receiptDate,
                    totalCost = cost,
                    notes = if (request.notes.isNullOrBlank()) current.notes else "${current.notes ?: ""}\n${request.notes}"
                )
                mockJobs[index] = updated
                return@withContext Result.success(updated)
            }

            try {
                val result = apiService.receiveOutput(id, request)
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
