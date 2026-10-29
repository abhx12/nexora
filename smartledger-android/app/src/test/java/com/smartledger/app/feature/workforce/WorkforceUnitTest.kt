package com.smartledger.app.feature.workforce

import com.google.gson.Gson
import com.smartledger.app.core.util.UiState
import com.smartledger.app.feature.parties.data.dto.CreatePartyRequest
import com.smartledger.app.feature.parties.data.dto.PartyResponse
import com.smartledger.app.feature.parties.data.dto.UpdatePartyRequest
import com.smartledger.app.feature.parties.domain.PartiesRepository
import com.smartledger.app.feature.parties.domain.model.PartyType
import com.smartledger.app.feature.workforce.data.api.WorkforceApiService
import com.smartledger.app.feature.workforce.data.dto.CreateWorkRecordRequest
import com.smartledger.app.feature.workforce.data.dto.WorkRecordResponse
import com.smartledger.app.feature.workforce.data.dto.WorkerPayableResponse
import com.smartledger.app.feature.workforce.data.repository.WorkforceRepositoryImpl
import com.smartledger.app.feature.workforce.domain.WorkforceRepository
import com.smartledger.app.feature.workforce.presentation.viewmodel.CreateWorkRecordViewModel
import com.smartledger.app.feature.workforce.presentation.viewmodel.WorkRecordListViewModel
import com.smartledger.app.feature.workforce.presentation.viewmodel.WorkerPayableViewModel
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

class FakeWorkforceApiService : WorkforceApiService {
    val records = mutableListOf<WorkRecordResponse>()
    val payables = mutableListOf<WorkerPayableResponse>()

    override suspend fun getWorkRecords(workerId: String?): List<WorkRecordResponse> =
        if (workerId.isNullOrBlank()) records else records.filter { it.workerId == workerId }

    override suspend fun createWorkRecord(request: CreateWorkRecordRequest): WorkRecordResponse {
        val total = if (request.workType == "WARPING") (request.quantityKg ?: 0.0) * request.rate else (request.hours ?: 0.0) * request.rate
        val rec = WorkRecordResponse(
            id = UUID.randomUUID().toString(),
            recordNumber = "WRK-2026-9999",
            workerId = request.workerId,
            workerName = "Worker ${request.workerId}",
            workDate = request.workDate,
            workType = request.workType,
            quantityKg = request.quantityKg,
            hours = request.hours,
            rate = request.rate,
            totalAmount = total
        )
        records.add(0, rec)
        return rec
    }

    override suspend fun getWorkerPayable(workerId: String): WorkerPayableResponse =
        payables.find { it.workerId == workerId } ?: WorkerPayableResponse(workerId, "Worker", 0.0, 0.0, 0.0, 0)

    override suspend fun getAllWorkersPayable(): List<WorkerPayableResponse> = payables
}

class FakeWorkerPartiesRepository : PartiesRepository {
    override suspend fun getParties(type: PartyType?, search: String?): Result<List<PartyResponse>> {
        return Result.success(
            listOf(
                PartyResponse(id = "worker-1", partyCode = "WRK-001", name = "Mani Kumar", partyType = "WORKER", phone = "9842111111", address = "Salem", status = "ACTIVE")
            )
        )
    }

    override suspend fun getPartyById(id: String): Result<PartyResponse> =
        Result.success(PartyResponse(id = id, partyCode = "WRK-001", name = "Mani Kumar", partyType = "WORKER", phone = null, address = null, status = "ACTIVE"))

    override suspend fun createParty(request: CreatePartyRequest): Result<PartyResponse> =
        Result.success(PartyResponse("w-new", "W-NEW", request.name, request.partyType, null, null, "ACTIVE"))

    override suspend fun updateParty(id: String, request: UpdatePartyRequest): Result<PartyResponse> =
        Result.success(PartyResponse(id, "W-UPD", request.name ?: "", "WORKER", null, null, "ACTIVE"))
}

class FakeWorkforceRepository : WorkforceRepository {
    val records = mutableListOf(
        WorkRecordResponse("r-1", "WRK-2026-0001", "worker-1", "Mani Kumar", "2026-10-18", "WARPING", 45.5, null, 120.0, 5460.0),
        WorkRecordResponse("r-2", "WRK-2026-0002", "worker-2", "Selvam R.", "2026-10-18", "ROLLING", null, 8.0, 85.0, 680.0)
    )

    val payables = listOf(
        WorkerPayableResponse("worker-1", "Mani Kumar", 10020.0, 6000.0, 4020.0, 2),
        WorkerPayableResponse("worker-2", "Selvam R.", 5440.0, 4000.0, 1440.0, 1)
    )

    override suspend fun getWorkRecords(workerId: String?): Result<List<WorkRecordResponse>> {
        val filtered = if (workerId.isNullOrBlank()) records else records.filter { it.workerId == workerId }
        return Result.success(filtered)
    }

    override suspend fun createWorkRecord(request: CreateWorkRecordRequest): Result<WorkRecordResponse> {
        val total = if (request.workType == "WARPING") (request.quantityKg ?: 0.0) * request.rate else (request.hours ?: 0.0) * request.rate
        val rec = WorkRecordResponse("r-new", "WRK-2026-9999", request.workerId, "Mani Kumar", request.workDate, request.workType, request.quantityKg, request.hours, request.rate, total)
        records.add(0, rec)
        return Result.success(rec)
    }

    override suspend fun getWorkerPayable(workerId: String): Result<WorkerPayableResponse> {
        return Result.success(payables.first { it.workerId == workerId })
    }

    override suspend fun getAllWorkersPayable(): Result<List<WorkerPayableResponse>> {
        return Result.success(payables)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class WorkforceUnitTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var apiService: FakeWorkforceApiService
    private lateinit var repository: WorkforceRepositoryImpl

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        apiService = FakeWorkforceApiService()
        repository = WorkforceRepositoryImpl(apiService, Gson())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testGetWorkRecordsMock() = runTest {
        val result = repository.getWorkRecords()
        assertTrue(result.isSuccess)
        val list = result.getOrThrow()
        assertTrue(list.isNotEmpty())
        assertTrue(list.any { it.workType == "WARPING" })
        assertTrue(list.any { it.workType == "ROLLING" })
    }

    @Test
    fun testCreateWarpingRecordCalculatesTotalCorrectly() = runTest {
        val request = CreateWorkRecordRequest(
            workerId = "worker-1",
            workDate = "2026-10-21",
            workType = "WARPING",
            quantityKg = 50.0,
            hours = null,
            rate = 120.0
        )
        val result = repository.createWorkRecord(request)
        assertTrue(result.isSuccess)
        val record = result.getOrThrow()
        assertEquals(6000.0, record.totalAmount, 0.001)
        assertEquals("WARPING", record.workType)
    }

    @Test
    fun testCreateRollingRecordCalculatesTotalCorrectly() = runTest {
        val request = CreateWorkRecordRequest(
            workerId = "worker-2",
            workDate = "2026-10-21",
            workType = "ROLLING",
            quantityKg = null,
            hours = 8.0,
            rate = 90.0
        )
        val result = repository.createWorkRecord(request)
        assertTrue(result.isSuccess)
        val record = result.getOrThrow()
        assertEquals(720.0, record.totalAmount, 0.001)
        assertEquals("ROLLING", record.workType)
    }

    @Test
    fun testCreateWorkRecord_InvalidInputFails() = runTest {
        val invalidWarping = CreateWorkRecordRequest(
            workerId = "worker-1",
            workDate = "2026-10-21",
            workType = "WARPING",
            quantityKg = 0.0,
            rate = 120.0
        )
        val result = repository.createWorkRecord(invalidWarping)
        assertTrue(result.isFailure)
    }

    @Test
    fun testGetWorkerPayable() = runTest {
        val result = repository.getWorkerPayable("worker-1")
        assertTrue(result.isSuccess)
        val payable = result.getOrThrow()
        assertTrue(payable.totalEarned > 0)
        assertTrue(payable.outstandingPayable > 0)
    }

    @Test
    fun testWorkRecordListViewModel() = runTest {
        val fakeRepo = FakeWorkforceRepository()
        val viewModel = WorkRecordListViewModel(fakeRepo)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        val records = (state as UiState.Success).data
        assertTrue(records.isNotEmpty())
    }

    @Test
    fun testCreateWorkRecordViewModel() = runTest {
        val fakeRepo = FakeWorkforceRepository()
        val partiesRepo = FakeWorkerPartiesRepository()
        val viewModel = CreateWorkRecordViewModel(fakeRepo, partiesRepo)
        advanceUntilIdle()

        viewModel.submitWorkRecord(
            workerId = "worker-1",
            workDate = "2026-10-21",
            workType = "WARPING",
            quantityKg = 40.0,
            hours = null,
            rate = 120.0,
            notes = "Test warping entry"
        )
        advanceUntilIdle()

        val state = viewModel.submitState.value
        assertTrue(state is UiState.Success)
    }

    @Test
    fun testWorkerPayableViewModel() = runTest {
        val fakeRepo = FakeWorkforceRepository()
        val viewModel = WorkerPayableViewModel(fakeRepo)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        val payables = (state as UiState.Success).data
        assertTrue(payables.isNotEmpty())
    }
}
