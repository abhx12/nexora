package com.smartledger.app.feature.outsourcing

import androidx.lifecycle.SavedStateHandle
import com.google.gson.Gson
import com.smartledger.app.core.util.UiState
import com.smartledger.app.feature.outsourcing.data.api.OutsourcingApiService
import com.smartledger.app.feature.outsourcing.data.dto.CreateOutsourcingJobRequest
import com.smartledger.app.feature.outsourcing.data.dto.IssueMaterialRequest
import com.smartledger.app.feature.outsourcing.data.dto.OutsourcingJobResponse
import com.smartledger.app.feature.outsourcing.data.dto.ReceiveOutsourcingRequest
import com.smartledger.app.feature.outsourcing.data.repository.OutsourcingRepositoryImpl
import com.smartledger.app.feature.outsourcing.domain.OutsourcingRepository
import com.smartledger.app.feature.outsourcing.domain.model.OutsourcingStatus
import com.smartledger.app.feature.outsourcing.presentation.viewmodel.CreateOutsourcingJobViewModel
import com.smartledger.app.feature.outsourcing.presentation.viewmodel.IssueMaterialViewModel
import com.smartledger.app.feature.outsourcing.presentation.viewmodel.OutsourcingDetailViewModel
import com.smartledger.app.feature.outsourcing.presentation.viewmodel.OutsourcingListViewModel
import com.smartledger.app.feature.outsourcing.presentation.viewmodel.ReceiveOutsourcingViewModel
import com.smartledger.app.feature.parties.data.dto.CreatePartyRequest
import com.smartledger.app.feature.parties.data.dto.PartyResponse
import com.smartledger.app.feature.parties.data.dto.UpdatePartyRequest
import com.smartledger.app.feature.parties.domain.PartiesRepository
import com.smartledger.app.feature.parties.domain.model.PartyType
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
import java.util.UUID

class FakePartiesRepository : PartiesRepository {
    override suspend fun getParties(type: PartyType?, search: String?): Result<List<PartyResponse>> {
        return Result.success(
            listOf(
                PartyResponse(
                    id = "mfr-1",
                    partyCode = "MFR-001",
                    name = "Sri Murugan Twisting Mills",
                    partyType = "MANUFACTURER",
                    phone = "9842100001",
                    address = "Erode, TN",
                    status = "ACTIVE"
                )
            )
        )
    }

    override suspend fun getPartyById(id: String): Result<PartyResponse> =
        Result.success(PartyResponse(id = id, partyCode = "P-1", name = "Test Party", partyType = "MANUFACTURER", phone = null, address = null, status = "ACTIVE"))

    override suspend fun createParty(request: CreatePartyRequest): Result<PartyResponse> =
        Result.success(PartyResponse("p-new", "P-NEW", request.name, request.partyType, null, null, "ACTIVE"))

    override suspend fun updateParty(id: String, request: UpdatePartyRequest): Result<PartyResponse> =
        Result.success(PartyResponse(id, "P-UPD", request.name ?: "Updated", "MANUFACTURER", null, null, "ACTIVE"))
}

class FakeOutsourcingApiService : OutsourcingApiService {
    val jobs = mutableListOf<OutsourcingJobResponse>()

    override suspend fun getOutsourcingJobs(status: String?): List<OutsourcingJobResponse> =
        if (status.isNullOrBlank()) jobs else jobs.filter { it.status.equals(status, ignoreCase = true) }

    override suspend fun getOutsourcingJobById(id: String): OutsourcingJobResponse =
        jobs.find { it.id == id } ?: throw NoSuchElementException("Job not found")

    override suspend fun createOutsourcingJob(request: CreateOutsourcingJobRequest): OutsourcingJobResponse {
        val job = OutsourcingJobResponse(
            id = UUID.randomUUID().toString(),
            jobNumber = "OUT-2026-9999",
            manufacturerId = request.manufacturerId,
            productType = request.productType,
            status = "CREATED",
            plannedWeightKg = request.plannedWeightKg
        )
        jobs.add(0, job)
        return job
    }

    override suspend fun issueMaterial(id: String, request: IssueMaterialRequest): OutsourcingJobResponse {
        val index = jobs.indexOfFirst { it.id == id }
        val updated = jobs[index].copy(status = "MATERIAL_ISSUED", issuedWeightKg = request.issuedWeightKg)
        jobs[index] = updated
        return updated
    }

    override suspend fun receiveOutput(id: String, request: ReceiveOutsourcingRequest): OutsourcingJobResponse {
        val index = jobs.indexOfFirst { it.id == id }
        val updated = jobs[index].copy(status = "COMPLETED", receivedWeightKg = request.receivedWeightKg)
        jobs[index] = updated
        return updated
    }
}

class FakeOutsourcingRepository : OutsourcingRepository {
    var availableSilk: Double = 324.5
    var externalWip: Double = 82.5

    val jobs = mutableListOf(
        OutsourcingJobResponse(
            id = "job-1",
            jobNumber = "OUT-2026-0001",
            manufacturerId = "mfr-1",
            manufacturerName = "Sri Murugan Twisting Mills",
            orderId = "order-1",
            productType = "SILK_WARP",
            status = "COMPLETED",
            plannedWeightKg = 60.0
        ),
        OutsourcingJobResponse(
            id = "job-2",
            jobNumber = "OUT-2026-0002",
            manufacturerId = "mfr-2",
            manufacturerName = "Balaji Weaving & Warping",
            productType = "KORA_WARP",
            status = "MATERIAL_ISSUED",
            plannedWeightKg = 82.5
        )
    )

    override suspend fun getOutsourcingJobs(status: String?): Result<List<OutsourcingJobResponse>> {
        val filtered = if (status.isNullOrBlank() || status == "ALL") jobs else jobs.filter { it.status == status }
        return Result.success(filtered)
    }

    override suspend fun getOutsourcingJobById(id: String): Result<OutsourcingJobResponse> {
        val j = jobs.find { it.id == id } ?: return Result.failure(Exception("Not found"))
        return Result.success(j)
    }

    override suspend fun createOutsourcingJob(request: CreateOutsourcingJobRequest): Result<OutsourcingJobResponse> {
        val newJob = OutsourcingJobResponse(
            id = "job-new",
            jobNumber = "OUT-2026-9999",
            manufacturerId = request.manufacturerId,
            productType = request.productType,
            status = "CREATED",
            plannedWeightKg = request.plannedWeightKg
        )
        jobs.add(0, newJob)
        return Result.success(newJob)
    }

    override suspend fun issueMaterial(id: String, request: IssueMaterialRequest): Result<OutsourcingJobResponse> {
        val idx = jobs.indexOfFirst { it.id == id }
        val updated = jobs[idx].copy(status = "MATERIAL_ISSUED", issuedWeightKg = request.issuedWeightKg)
        jobs[idx] = updated
        return Result.success(updated)
    }

    override suspend fun receiveOutput(id: String, request: ReceiveOutsourcingRequest): Result<OutsourcingJobResponse> {
        val idx = jobs.indexOfFirst { it.id == id }
        val updated = jobs[idx].copy(status = "COMPLETED", receivedWeightKg = request.receivedWeightKg)
        jobs[idx] = updated
        return Result.success(updated)
    }

    override suspend fun getAvailableRawSilkKg(): Double = availableSilk
    override suspend fun getExternalWipKg(): Double = externalWip
}

@OptIn(ExperimentalCoroutinesApi::class)
class OutsourcingUnitTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var apiService: FakeOutsourcingApiService
    private lateinit var repository: OutsourcingRepositoryImpl

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        apiService = FakeOutsourcingApiService()
        repository = OutsourcingRepositoryImpl(apiService, Gson())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testOutsourcingStatusMapping() {
        assertEquals(OutsourcingStatus.CREATED, OutsourcingStatus.fromString("CREATED"))
        assertEquals(OutsourcingStatus.MATERIAL_ISSUED, OutsourcingStatus.fromString("MATERIAL_ISSUED"))
        assertEquals(OutsourcingStatus.PARTIALLY_RECEIVED, OutsourcingStatus.fromString("PARTIALLY_RECEIVED"))
        assertEquals(OutsourcingStatus.COMPLETED, OutsourcingStatus.fromString("COMPLETED"))
        assertEquals(OutsourcingStatus.CREATED, OutsourcingStatus.fromString("UNKNOWN"))
    }

    @Test
    fun testGetOutsourcingJobsMock() = runTest {
        val result = repository.getOutsourcingJobs()
        assertTrue(result.isSuccess)
        val list = result.getOrThrow()
        assertTrue(list.isNotEmpty())
        assertTrue(list.any { it.status == "COMPLETED" })
        assertTrue(list.any { it.status == "MATERIAL_ISSUED" })
    }

    @Test
    fun testFilterOutsourcingJobs() = runTest {
        val result = repository.getOutsourcingJobs("COMPLETED")
        assertTrue(result.isSuccess)
        val list = result.getOrThrow()
        assertTrue(list.all { it.status == "COMPLETED" })
    }

    @Test
    fun testCreateOutsourcingJob() = runTest {
        val request = CreateOutsourcingJobRequest(
            manufacturerId = "mfr-1",
            productType = "SILK_WARP",
            plannedWeightKg = 40.0,
            ratePerKg = 180.0
        )
        val result = repository.createOutsourcingJob(request)
        assertTrue(result.isSuccess)
        val created = result.getOrThrow()
        assertEquals("CREATED", created.status)
        assertEquals(40.0, created.plannedWeightKg, 0.001)
    }

    @Test
    fun testCreateOutsourcingJob_InvalidWeightFails() = runTest {
        val request = CreateOutsourcingJobRequest(
            manufacturerId = "mfr-1",
            productType = "SILK_WARP",
            plannedWeightKg = 0.0
        )
        val result = repository.createOutsourcingJob(request)
        assertTrue(result.isFailure)
    }

    @Test
    fun testIssueMaterial_Success() = runTest {
        val request = IssueMaterialRequest(
            issueDate = "2026-10-21",
            issuedWeightKg = 45.0,
            notes = "Lot #99 dispatched"
        )
        val result = repository.issueMaterial("job-3", request)
        assertTrue(result.isSuccess)
        val updated = result.getOrThrow()
        assertEquals("MATERIAL_ISSUED", updated.status)
        assertEquals(45.0, updated.issuedWeightKg ?: 0.0, 0.001)
    }

    @Test
    fun testIssueMaterial_InsufficientInventoryFails() = runTest {
        val request = IssueMaterialRequest(
            issueDate = "2026-10-21",
            issuedWeightKg = 9999.0
        )
        val result = repository.issueMaterial("job-3", request)
        assertTrue(result.isFailure)
    }

    @Test
    fun testReceiveOutput_Success() = runTest {
        val request = ReceiveOutsourcingRequest(
            receiptDate = "2026-10-21",
            receivedWeightKg = 80.5,
            wastageWeightKg = 2.0,
            notes = "Good quality warp received"
        )
        val result = repository.receiveOutput("job-2", request)
        assertTrue(result.isSuccess)
        val completed = result.getOrThrow()
        assertEquals("COMPLETED", completed.status)
        assertEquals(80.5, completed.receivedWeightKg ?: 0.0, 0.001)
    }

    @Test
    fun testOutsourcingListViewModel() = runTest {
        val fakeRepo = FakeOutsourcingRepository()
        val viewModel = OutsourcingListViewModel(fakeRepo)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        val jobs = (state as UiState.Success).data
        assertTrue(jobs.isNotEmpty())
    }

    @Test
    fun testOutsourcingDetailViewModel() = runTest {
        val fakeRepo = FakeOutsourcingRepository()
        val handle = SavedStateHandle(mapOf("jobId" to "job-1"))
        val viewModel = OutsourcingDetailViewModel(fakeRepo, handle)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        val job = (state as UiState.Success).data
        assertEquals("job-1", job.id)
    }

    @Test
    fun testCreateOutsourcingJobViewModel() = runTest {
        val fakeRepo = FakeOutsourcingRepository()
        val partiesRepo = FakePartiesRepository()
        val viewModel = CreateOutsourcingJobViewModel(fakeRepo, partiesRepo)
        advanceUntilIdle()

        viewModel.createJob(
            manufacturerId = "mfr-1",
            orderId = null,
            productType = "SILK_WARP",
            plannedWeightKg = 50.0,
            ratePerKg = 190.0,
            expectedReturnDate = "2026-10-30",
            notes = "Test"
        )
        advanceUntilIdle()

        val state = viewModel.submitState.value
        assertTrue(state is UiState.Success)
    }

    @Test
    fun testIssueMaterialViewModel() = runTest {
        val fakeRepo = FakeOutsourcingRepository()
        val handle = SavedStateHandle(mapOf("jobId" to "job-2"))
        val viewModel = IssueMaterialViewModel(fakeRepo, handle)
        advanceUntilIdle()

        viewModel.issueMaterial("2026-10-21", 50.0, "Notes")
        advanceUntilIdle()

        val state = viewModel.submitState.value
        assertTrue(state is UiState.Success)
    }

    @Test
    fun testReceiveOutsourcingViewModel() = runTest {
        val fakeRepo = FakeOutsourcingRepository()
        val handle = SavedStateHandle(mapOf("jobId" to "job-2"))
        val viewModel = ReceiveOutsourcingViewModel(fakeRepo, handle)
        advanceUntilIdle()

        viewModel.receiveOutput("2026-10-21", 48.0, 2.0, "Notes")
        advanceUntilIdle()

        val state = viewModel.submitState.value
        assertTrue(state is UiState.Success)
    }
}
