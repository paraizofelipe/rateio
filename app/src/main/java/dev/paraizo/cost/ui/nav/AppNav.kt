package dev.paraizo.cost.ui.nav

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.paraizo.cost.ui.auth.AuthState
import dev.paraizo.cost.ui.auth.AuthViewModel
import dev.paraizo.cost.ui.auth.LoginScreen

@Composable
fun AppNav(authViewModel: AuthViewModel) {
    val state by authViewModel.state.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    LaunchedEffect(state) {
        when (state) {
            is AuthState.LoggedIn -> navController.navigate(Routes.GRUPOS) {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                launchSingleTop = true
            }
            is AuthState.LoggedOut -> {
                if (navController.currentDestination?.route != Routes.LOGIN) {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
            else -> Unit
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestinationFor(state)
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                state = state,
                onLogin = { email, senha -> authViewModel.login(email, senha) }
            )
        }
        composable(Routes.GRUPOS) {
            Text("Grupos")
        }
        composable(Routes.PESSOAS) {
            Text("Pessoas")
        }
        composable(Routes.GASTOS) {
            Text("Gastos")
        }
        composable(Routes.SETTLE) {
            Text("Settle")
        }
    }
}
