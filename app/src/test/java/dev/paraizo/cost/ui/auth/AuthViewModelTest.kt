package dev.paraizo.cost.ui.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var gateway: FakeAuthGateway
    private lateinit var viewModel: AuthViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        gateway = FakeAuthGateway()
        viewModel = AuthViewModel(gateway, testDispatcher)
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loginWithSuccessfulGatewayResultsInLoggedIn() = runTest(testDispatcher) {
        gateway.loggedIn = true
        viewModel.login("user@example.com", "secret")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(AuthState.LoggedIn, viewModel.state.value)
    }

    @Test
    fun loginWithGatewayThrowingResultsInErrorWithNonEmptyMessage() = runTest(testDispatcher) {
        gateway.loginError = RuntimeException("Credenciais inválidas")
        viewModel.login("user@example.com", "wrong")
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.state.value
        assertTrue(state is AuthState.Error)
        assertTrue(state.message.isNotEmpty())
    }

    @Test
    fun checkSessionWithActiveSessionResultsInLoggedIn() = runTest(testDispatcher) {
        gateway.loggedIn = true
        viewModel.checkSession()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(AuthState.LoggedIn, viewModel.state.value)
    }

    @Test
    fun checkSessionWithNoSessionResultsInLoggedOut() = runTest(testDispatcher) {
        gateway.loggedIn = false
        viewModel.checkSession()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(AuthState.LoggedOut, viewModel.state.value)
    }

    @Test
    fun logoutResultsInLoggedOut() = runTest(testDispatcher) {
        gateway.loggedIn = true
        viewModel.checkSession()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.logout()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(AuthState.LoggedOut, viewModel.state.value)
    }
}

private class FakeAuthGateway : AuthGateway {
    var loggedIn: Boolean = false
    var loginError: Throwable? = null

    override suspend fun isLoggedIn(): Boolean = loggedIn

    override suspend fun login(email: String, senha: String) {
        loginError?.let { throw it }
        loggedIn = true
    }

    override suspend fun logout() {
        loggedIn = false
    }
}
