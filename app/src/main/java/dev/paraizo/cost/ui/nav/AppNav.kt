package dev.paraizo.cost.ui.nav

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
import dev.paraizo.cost.data.GastoRepository
import dev.paraizo.cost.data.GrupoRepository
import dev.paraizo.cost.data.PessoaRepository
import dev.paraizo.cost.ui.auth.AuthState
import dev.paraizo.cost.ui.auth.AuthViewModel
import dev.paraizo.cost.ui.auth.LoginScreen
import dev.paraizo.cost.ui.gastos.GastosScreen
import dev.paraizo.cost.ui.gastos.GastosViewModel
import dev.paraizo.cost.ui.grupos.GruposScreen
import dev.paraizo.cost.ui.grupos.GruposViewModel
import dev.paraizo.cost.ui.pessoas.PessoasScreen
import dev.paraizo.cost.ui.pessoas.PessoasViewModel
import dev.paraizo.cost.ui.settle.SettleScreen
import dev.paraizo.cost.ui.settle.SettleViewModel

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
        composable(Routes.GASTOS) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
            val gastosViewModel: GastosViewModel = viewModel(
                key = groupId,
                factory = viewModelFactory {
                    initializer {
                        GastosViewModel(
                            GastoRepository(appwriteClient),
                            PessoaRepository(appwriteClient),
                            groupId
                        )
                    }
                }
            )
            val gastosState by gastosViewModel.state.collectAsStateWithLifecycle()

            LaunchedEffect(groupId) {
                gastosViewModel.load()
            }

            GastosScreen(
                state = gastosState,
                onSelecionarCompetencia = { gastosViewModel.selecionarCompetencia(it) },
                onCriar = { descricao, valorCentavos, pagadorId, competencia ->
                    gastosViewModel.criar(descricao, valorCentavos, pagadorId, competencia)
                },
                onVerSettle = { competencia ->
                    navController.navigate(Routes.settle(groupId, competencia))
                }
            )
        }
        composable(Routes.SETTLE) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
            val competencia = backStackEntry.arguments?.getString("competencia") ?: return@composable
            val settleViewModel: SettleViewModel = viewModel(
                key = groupId,
                factory = viewModelFactory {
                    initializer {
                        SettleViewModel(
                            PessoaRepository(appwriteClient),
                            GastoRepository(appwriteClient),
                            groupId
                        )
                    }
                }
            )
            val settleState by settleViewModel.state.collectAsStateWithLifecycle()

            LaunchedEffect(competencia) {
                settleViewModel.carregar(competencia)
            }

            SettleScreen(state = settleState)
        }
    }
}
