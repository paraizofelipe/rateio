package dev.paraizo.cost.ui.nav

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.paraizo.cost.data.AppwriteClient
import dev.paraizo.cost.data.GrupoRepository
import dev.paraizo.cost.data.PessoaRepository
import dev.paraizo.cost.ui.auth.AuthState
import dev.paraizo.cost.ui.auth.AuthViewModel
import dev.paraizo.cost.ui.auth.LoginScreen
import dev.paraizo.cost.ui.grupos.GruposScreen
import dev.paraizo.cost.ui.grupos.GruposViewModel
import dev.paraizo.cost.ui.pessoas.PessoasScreen
import dev.paraizo.cost.ui.pessoas.PessoasViewModel

@Composable
fun AppNav(authViewModel: AuthViewModel, appwriteClient: AppwriteClient) {
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
            val gruposViewModel: GruposViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        GruposViewModel(GrupoRepository(appwriteClient))
                    }
                }
            )
            val gruposState by gruposViewModel.state.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
                gruposViewModel.load()
            }

            GruposScreen(
                state = gruposState,
                onCriar = { nome -> gruposViewModel.criar(nome) },
                onSelecionar = { groupId -> navController.navigate(Routes.pessoas(groupId)) }
            )
        }
        composable(Routes.PESSOAS) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
            val pessoasViewModel: PessoasViewModel = viewModel(
                key = groupId,
                factory = viewModelFactory {
                    initializer {
                        PessoasViewModel(PessoaRepository(appwriteClient), groupId)
                    }
                }
            )
            val pessoasState by pessoasViewModel.state.collectAsStateWithLifecycle()

            LaunchedEffect(groupId) {
                pessoasViewModel.load()
            }

            PessoasScreen(
                state = pessoasState,
                onSalvar = { nome, rendaCentavos -> pessoasViewModel.salvar(nome, rendaCentavos) }
            )
        }
        composable(Routes.GASTOS) {
            Text("Gastos")
        }
        composable(Routes.SETTLE) {
            Text("Settle")
        }
    }
}
