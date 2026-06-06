package dev.paraizo.cost.ui.auth

sealed interface AuthState {
    data object Loading : AuthState
    data object LoggedOut : AuthState
    data object LoggedIn : AuthState
    data class Error(val message: String) : AuthState
}
