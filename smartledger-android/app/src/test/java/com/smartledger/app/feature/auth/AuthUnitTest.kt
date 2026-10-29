package com.smartledger.app.feature.auth

import com.google.gson.Gson
import com.smartledger.app.core.auth.TokenManager
import com.smartledger.app.feature.auth.data.api.AuthApiService
import com.smartledger.app.feature.auth.data.dto.LoginRequest
import com.smartledger.app.feature.auth.data.dto.LoginResponse
import com.smartledger.app.feature.auth.data.dto.RefreshTokenRequest
import com.smartledger.app.feature.auth.data.repository.AuthRepositoryImpl
import com.smartledger.app.feature.auth.domain.AuthRepository
import com.smartledger.app.feature.auth.presentation.viewmodel.LoginViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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
import retrofit2.Call
import retrofit2.Response

/**
 * In-memory test double for TokenManager.
 */
class FakeTokenManager : TokenManager {
    private var accessToken: String? = null
    private var refreshToken: String? = null

    override fun saveTokens(access: String, refresh: String) {
        accessToken = access
        refreshToken = refresh
    }

    override fun getAccessToken(): String? = accessToken

    override fun getRefreshToken(): String? = refreshToken

    override fun clearTokens() {
        accessToken = null
        refreshToken = null
    }

    override fun isLoggedIn(): Boolean = accessToken != null
}

/**
 * Test double for AuthApiService.
 */
class FakeAuthApiService : AuthApiService {
    var shouldFailLogin: Boolean = false
    var failureCode: Int = 401
    var failureMessage: String = "Invalid username or password"

    override suspend fun login(request: LoginRequest): Response<LoginResponse> {
        return if (shouldFailLogin) {
            val errorJson = """{"status":$failureCode,"message":"$failureMessage"}"""
            Response.error(
                failureCode,
                errorJson.toResponseBody("application/json".toMediaTypeOrNull())
            )
        } else {
            Response.success(
                LoginResponse(
                    accessToken = "test_access_jwt",
                    refreshToken = "test_refresh_jwt",
                    expiresIn = 900L
                )
            )
        }
    }

    override suspend fun refresh(request: RefreshTokenRequest): Response<LoginResponse> {
        return if (shouldFailLogin) {
            Response.error(
                401,
                "Unauthorized".toResponseBody("text/plain".toMediaTypeOrNull())
            )
        } else {
            Response.success(
                LoginResponse(
                    accessToken = "refreshed_access_jwt",
                    refreshToken = "refreshed_refresh_jwt",
                    expiresIn = 900L
                )
            )
        }
    }

    override fun refreshSync(request: RefreshTokenRequest): Call<LoginResponse> {
        throw UnsupportedOperationException("Not needed in unit test")
    }
}

/**
 * Test double for AuthRepository.
 */
class FakeAuthRepository : AuthRepository {
    var shouldFail: Boolean = false
    var failureMessage: String = "Invalid credentials"
    var loggedIn: Boolean = false
    var lastUsername: String? = null

    override suspend fun login(username: String, password: String): Result<LoginResponse> {
        lastUsername = username
        return if (shouldFail) {
            Result.failure(Exception(failureMessage))
        } else {
            loggedIn = true
            Result.success(
                LoginResponse(
                    accessToken = "fake_access_token",
                    refreshToken = "fake_refresh_token",
                    expiresIn = 900L
                )
            )
        }
    }

    override suspend fun refreshToken(): Result<LoginResponse> {
        return if (shouldFail) {
            Result.failure(Exception("Refresh failed"))
        } else {
            Result.success(
                LoginResponse(
                    accessToken = "new_fake_access_token",
                    refreshToken = "new_fake_refresh_token",
                    expiresIn = 900L
                )
            )
        }
    }

    override fun isLoggedIn(): Boolean = loggedIn

    override fun getAccessToken(): String? = if (loggedIn) "fake_access_token" else null

    override fun logout() {
        loggedIn = false
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AuthUnitTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── AuthRepositoryImpl Tests ──────────────────────────────────────────

    @Test
    fun repository_login_success_storesTokens() = runTest {
        val fakeApi = FakeAuthApiService()
        val fakeStorage = FakeTokenManager()
        val repository = AuthRepositoryImpl(fakeApi, fakeStorage, Gson())

        val result = repository.login("owner", "secret")

        assertTrue(result.isSuccess)
        assertEquals("test_access_jwt", result.getOrNull()?.accessToken)
        assertEquals("test_access_jwt", fakeStorage.getAccessToken())
        assertEquals("test_refresh_jwt", fakeStorage.getRefreshToken())
        assertTrue(repository.isLoggedIn())
    }

    @Test
    fun repository_login_emptyCredentials_fails() = runTest {
        val fakeApi = FakeAuthApiService()
        val fakeStorage = FakeTokenManager()
        val repository = AuthRepositoryImpl(fakeApi, fakeStorage, Gson())

        val resultEmptyUser = repository.login("", "secret")
        assertTrue(resultEmptyUser.isFailure)

        val resultEmptyPass = repository.login("owner", "  ")
        assertTrue(resultEmptyPass.isFailure)
        assertFalse(fakeStorage.isLoggedIn())
    }

    @Test
    fun repository_login_serverError_returnsFailure() = runTest {
        val fakeApi = FakeAuthApiService().apply {
            shouldFailLogin = true
            failureCode = 401
            failureMessage = "Invalid credentials from server"
        }
        val fakeStorage = FakeTokenManager()
        val repository = AuthRepositoryImpl(fakeApi, fakeStorage, Gson())

        val result = repository.login("owner", "wrongpassword")

        assertTrue(result.isFailure)
        assertEquals("Invalid credentials from server", result.exceptionOrNull()?.message)
        assertFalse(fakeStorage.isLoggedIn())
    }

    @Test
    fun repository_refreshToken_success() = runTest {
        val fakeApi = FakeAuthApiService()
        val fakeStorage = FakeTokenManager().apply {
            saveTokens("old_access", "old_refresh")
        }
        val repository = AuthRepositoryImpl(fakeApi, fakeStorage, Gson())

        val result = repository.refreshToken()

        assertTrue(result.isSuccess)
        assertEquals("refreshed_access_jwt", fakeStorage.getAccessToken())
    }

    @Test
    fun repository_refreshToken_noStoredToken_fails() = runTest {
        val fakeApi = FakeAuthApiService()
        val fakeStorage = FakeTokenManager() // no tokens
        val repository = AuthRepositoryImpl(fakeApi, fakeStorage, Gson())

        val result = repository.refreshToken()

        assertTrue(result.isFailure)
    }

    @Test
    fun repository_logout_clearsTokens() {
        val fakeApi = FakeAuthApiService()
        val fakeStorage = FakeTokenManager().apply {
            saveTokens("access", "refresh")
        }
        val repository = AuthRepositoryImpl(fakeApi, fakeStorage, Gson())

        assertTrue(repository.isLoggedIn())
        repository.logout()
        assertFalse(repository.isLoggedIn())
        assertNull(repository.getAccessToken())
    }

    // ── LoginViewModel Tests ──────────────────────────────────────────────

    @Test
    fun viewModel_initialState_isEmpty() {
        val fakeRepo = FakeAuthRepository()
        val viewModel = LoginViewModel(fakeRepo)

        val state = viewModel.uiState.value
        assertEquals("", state.username)
        assertEquals("", state.password)
        assertFalse(state.isLoading)
        assertFalse(state.isSuccess)
        assertFalse(state.isPasswordVisible)
        assertNull(state.errorMessage)
    }

    @Test
    fun viewModel_inputChange_updatesState() {
        val fakeRepo = FakeAuthRepository()
        val viewModel = LoginViewModel(fakeRepo)

        viewModel.onUsernameChanged("admin")
        viewModel.onPasswordChanged("123456")
        viewModel.togglePasswordVisibility()

        val state = viewModel.uiState.value
        assertEquals("admin", state.username)
        assertEquals("123456", state.password)
        assertTrue(state.isPasswordVisible)
    }

    @Test
    fun viewModel_emptyUsername_showsError() {
        val fakeRepo = FakeAuthRepository()
        val viewModel = LoginViewModel(fakeRepo)

        viewModel.onPasswordChanged("secret")
        viewModel.login()

        assertEquals("Please enter your username", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun viewModel_emptyPassword_showsError() {
        val fakeRepo = FakeAuthRepository()
        val viewModel = LoginViewModel(fakeRepo)

        viewModel.onUsernameChanged("owner")
        viewModel.login()

        assertEquals("Please enter your password", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun viewModel_loginSuccess_updatesState() = runTest {
        val fakeRepo = FakeAuthRepository()
        val viewModel = LoginViewModel(fakeRepo)

        viewModel.onUsernameChanged("owner")
        viewModel.onPasswordChanged("secret")
        viewModel.login()

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isSuccess)
        assertNull(state.errorMessage)
        assertEquals("owner", fakeRepo.lastUsername)
    }

    @Test
    fun viewModel_loginFailure_setsErrorMessage() = runTest {
        val fakeRepo = FakeAuthRepository().apply {
            shouldFail = true
            failureMessage = "Invalid username or password"
        }
        val viewModel = LoginViewModel(fakeRepo)

        viewModel.onUsernameChanged("owner")
        viewModel.onPasswordChanged("wrong")
        viewModel.login()

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isSuccess)
        assertEquals("Invalid username or password", state.errorMessage)
    }
}
