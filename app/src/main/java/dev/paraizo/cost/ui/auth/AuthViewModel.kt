package dev.paraizo.cost.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val gateway: AuthGateway,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state: StateFlow<AuthState> = _state

    fun checkSession() {
        viewModelScope.launch(dispatcher) {
            _state.value = AuthState.Loading
            try {
                _state.value = if (gateway.isLoggedIn()) AuthState.LoggedIn else AuthState.LoggedOut
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = AuthState.Error(e.message ?: "Erro ao verificar sessão")
            }
        }
    }

    fun login(email: String, senha: String) {
        viewModelScope.launch(dispatcher) {
            _state.value = AuthState.Loading
            try {
                gateway.login(email, senha)
                _state.value = AuthState.LoggedIn
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = AuthState.Error(e.message ?: "Erro ao fazer login")
            }
        }
    }

    fun logout() {
        viewModelScope.launch(dispatcher) {
            _state.value = AuthState.Loading
            try {
                gateway.logout()
                _state.value = AuthState.LoggedOut
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Não transiciona para LoggedOut se o servidor rejeitou: o estado local
                // deve refletir o real (a sessão pode continuar ativa no Appwrite).
                _state.value = AuthState.Error(e.message ?: "Erro ao sair")
            }
        }
    }
}
