package com.smartledger.app.feature.parties

import com.google.gson.Gson
import com.smartledger.app.core.util.UiState
import com.smartledger.app.feature.parties.data.api.PartiesApiService
import com.smartledger.app.feature.parties.data.dto.CreatePartyRequest
import com.smartledger.app.feature.parties.data.dto.PartyResponse
import com.smartledger.app.feature.parties.data.dto.UpdatePartyRequest
import com.smartledger.app.feature.parties.data.repository.PartiesRepositoryImpl
import com.smartledger.app.feature.parties.domain.PartiesRepository
import com.smartledger.app.feature.parties.domain.model.PartyType
import com.smartledger.app.feature.parties.presentation.viewmodel.CreatePartyViewModel
import com.smartledger.app.feature.parties.presentation.viewmodel.PartyDetailViewModel
import com.smartledger.app.feature.parties.presentation.viewmodel.PartyListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

/**
 * Fake PartiesApiService for repository tests.
 */
class FakePartiesApiService : PartiesApiService {
    var shouldFail: Boolean = false
    var failureCode: Int = 400
    var failureMessage: String = "Bad request"

    val sampleParties = mutableListOf(
        PartyResponse(
            id = "c1",
            partyCode = "CUST-001",
            name = "Ramesh Silks",
            partyType = "CUSTOMER",
            phone = "9845012345",
            address = "Bangalore",
            status = "ACTIVE"
        ),
        PartyResponse(
            id = "s1",
            partyCode = "SUPP-001",
            name = "Karnataka Silk Filatures",
            partyType = "SUPPLIER",
            phone = null,
            address = "Ramanagara",
            status = "ACTIVE"
        )
    )

    override suspend fun getParties(type: String?, search: String?): Response<List<PartyResponse>> {
        if (shouldFail) {
            val errorJson = """{"status":$failureCode,"message":"$failureMessage"}"""
            return Response.error(failureCode, errorJson.toResponseBody("application/json".toMediaTypeOrNull()))
        }
        var list = sampleParties.toList()
        if (type != null) {
            list = list.filter { it.partyType.equals(type, ignoreCase = true) }
        }
        if (!search.isNullOrBlank()) {
            list = list.filter { it.name.contains(search, ignoreCase = true) || it.partyCode.contains(search, ignoreCase = true) }
        }
        return Response.success(list)
    }

    override suspend fun createParty(request: CreatePartyRequest): Response<PartyResponse> {
        if (shouldFail) {
            val errorJson = """{"status":$failureCode,"message":"$failureMessage"}"""
            return Response.error(failureCode, errorJson.toResponseBody("application/json".toMediaTypeOrNull()))
        }
        val created = PartyResponse(
            id = "new-${sampleParties.size + 1}",
            partyCode = "NEW-001",
            name = request.name,
            partyType = request.partyType,
            phone = request.phone,
            address = request.address,
            status = "ACTIVE"
        )
        sampleParties.add(created)
        return Response.success(created)
    }

    override suspend fun getPartyById(id: String): Response<PartyResponse> {
        val found = sampleParties.firstOrNull { it.id == id || it.partyCode == id }
        return if (found != null && !shouldFail) {
            Response.success(found)
        } else {
            Response.error(404, "Not found".toResponseBody("text/plain".toMediaTypeOrNull()))
        }
    }

    override suspend fun updateParty(id: String, request: UpdatePartyRequest): Response<PartyResponse> {
        val idx = sampleParties.indexOfFirst { it.id == id }
        return if (idx != -1 && !shouldFail) {
            val existing = sampleParties[idx]
            val updated = existing.copy(
                name = request.name ?: existing.name,
                phone = request.phone ?: existing.phone,
                status = request.status ?: existing.status
            )
            sampleParties[idx] = updated
            Response.success(updated)
        } else {
            Response.error(404, "Not found".toResponseBody("text/plain".toMediaTypeOrNull()))
        }
    }
}

/**
 * Fake PartiesRepository for ViewModel tests.
 */
class FakePartiesRepository : PartiesRepository {
    var shouldFail: Boolean = false
    var failureMessage: String = "Network error"

    val parties = mutableListOf(
        PartyResponse("c1", "CUST-001", "Ramesh Silks", "CUSTOMER", "9845012345", "Bangalore", "ACTIVE"),
        PartyResponse("s1", "SUPP-001", "Karnataka Silk", "SUPPLIER", null, "Ramanagara", "ACTIVE")
    )

    override suspend fun getParties(type: PartyType?, search: String?): Result<List<PartyResponse>> {
        return if (shouldFail) {
            Result.failure(Exception(failureMessage))
        } else {
            var list = parties.toList()
            if (type != null) list = list.filter { it.partyType == type.name }
            if (!search.isNullOrBlank()) list = list.filter { it.name.contains(search, true) }
            Result.success(list)
        }
    }

    override suspend fun getPartyById(id: String): Result<PartyResponse> {
        val found = parties.firstOrNull { it.id == id }
        return if (found != null && !shouldFail) {
            Result.success(found)
        } else {
            Result.failure(Exception("Party not found"))
        }
    }

    override suspend fun createParty(request: CreatePartyRequest): Result<PartyResponse> {
        return if (shouldFail) {
            Result.failure(Exception(failureMessage))
        } else {
            val created = PartyResponse(
                id = "id-${parties.size + 1}",
                partyCode = "CUST-999",
                name = request.name,
                partyType = request.partyType,
                phone = request.phone,
                address = request.address,
                status = "ACTIVE"
            )
            parties.add(created)
            Result.success(created)
        }
    }

    override suspend fun updateParty(id: String, request: UpdatePartyRequest): Result<PartyResponse> {
        val idx = parties.indexOfFirst { it.id == id }
        return if (idx != -1 && !shouldFail) {
            val updated = parties[idx].copy(status = request.status ?: parties[idx].status)
            parties[idx] = updated
            Result.success(updated)
        } else {
            Result.failure(Exception("Not found"))
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class PartiesUnitTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Repository Tests ──────────────────────────────────────────────────

    @Test
    fun repository_getParties_filtersByType() = runTest {
        val fakeApi = FakePartiesApiService()
        val repo = PartiesRepositoryImpl(fakeApi, Gson())

        val result = repo.getParties(type = PartyType.CUSTOMER)

        assertTrue(result.isSuccess)
        val list = result.getOrNull()!!
        assertEquals(1, list.size)
        assertEquals("CUST-001", list[0].partyCode)
        assertEquals("CUSTOMER", list[0].partyType)
    }

    @Test
    fun repository_createParty_successWithOptionalPhone() = runTest {
        val fakeApi = FakePartiesApiService()
        val repo = PartiesRepositoryImpl(fakeApi, Gson())

        val request = CreatePartyRequest(
            name = "Sri Balaji Twisters",
            partyType = "MANUFACTURER",
            phone = null, // Optional
            address = "Doddaballapura"
        )
        val result = repo.createParty(request)

        assertTrue(result.isSuccess)
        val created = result.getOrNull()!!
        assertEquals("Sri Balaji Twisters", created.name)
        assertNull(created.phone)
        assertEquals("ACTIVE", created.status)
    }

    @Test
    fun repository_createParty_blankName_fails() = runTest {
        val fakeApi = FakePartiesApiService()
        val repo = PartiesRepositoryImpl(fakeApi, Gson())

        val request = CreatePartyRequest(
            name = "   ",
            partyType = "CUSTOMER"
        )
        val result = repo.createParty(request)

        assertTrue(result.isFailure)
        assertEquals("Party name is required", result.exceptionOrNull()?.message)
    }

    @Test
    fun repository_updateParty_statusToggle() = runTest {
        val fakeApi = FakePartiesApiService()
        val repo = PartiesRepositoryImpl(fakeApi, Gson())

        val result = repo.updateParty("c1", UpdatePartyRequest(status = "INACTIVE"))

        assertTrue(result.isSuccess)
        val updated = result.getOrNull()!!
        assertEquals("INACTIVE", updated.status)
        assertFalse(updated.isActive)
    }

    // ── ViewModel Tests ────────────────────────────────────────────────────

    @Test
    fun partyListViewModel_typeSelection_updatesList() = runTest {
        val fakeRepo = FakePartiesRepository()
        val viewModel = PartyListViewModel(fakeRepo)

        advanceUntilIdle()

        // Default tab is CUSTOMER
        assertEquals(PartyType.CUSTOMER, viewModel.selectedType.value)
        val customerState = viewModel.uiState.value as UiState.Success
        assertEquals(1, customerState.data.size)
        assertEquals("Ramesh Silks", customerState.data[0].name)

        // Switch to SUPPLIER
        viewModel.selectType(PartyType.SUPPLIER)
        advanceUntilIdle()

        assertEquals(PartyType.SUPPLIER, viewModel.selectedType.value)
        val supplierState = viewModel.uiState.value as UiState.Success
        assertEquals(1, supplierState.data.size)
        assertEquals("Karnataka Silk", supplierState.data[0].name)
    }

    @Test
    fun partyListViewModel_searchDebounce() = runTest {
        val fakeRepo = FakePartiesRepository()
        val viewModel = PartyListViewModel(fakeRepo)
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("Ramesh")
        advanceTimeBy(350)
        advanceUntilIdle()

        val state = viewModel.uiState.value as UiState.Success
        assertEquals(1, state.data.size)
        assertEquals("Ramesh Silks", state.data[0].name)
    }

    @Test
    fun createPartyViewModel_emptyName_showsError() {
        val fakeRepo = FakePartiesRepository()
        val viewModel = CreatePartyViewModel(fakeRepo)

        viewModel.onPhoneChanged("9845012345")
        viewModel.createParty()

        assertEquals("Party name is required", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.isSuccess)
    }

    @Test
    fun createPartyViewModel_successWithOptionalPhone() = runTest {
        val fakeRepo = FakePartiesRepository()
        val viewModel = CreatePartyViewModel(fakeRepo)

        viewModel.onNameChanged("Cauvery Silk Sizing")
        viewModel.onTypeChanged(PartyType.MANUFACTURER)
        // Phone intentionally left empty
        viewModel.createParty()

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isSuccess)
        assertNull(state.errorMessage)
    }

    @Test
    fun partyDetailViewModel_loadAndToggleStatus() = runTest {
        val fakeRepo = FakePartiesRepository()
        val viewModel = PartyDetailViewModel(fakeRepo)

        viewModel.loadParty("c1")
        advanceUntilIdle()

        val state = viewModel.uiState.value as UiState.Success
        assertEquals("CUST-001", state.data.partyCode)
        assertTrue(state.data.isActive)

        // Toggle to INACTIVE
        viewModel.toggleStatus("c1")
        advanceUntilIdle()

        val updatedState = viewModel.uiState.value as UiState.Success
        assertEquals("INACTIVE", updatedState.data.status)
        assertFalse(updatedState.data.isActive)
    }
}
