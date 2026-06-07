package dev.paraizo.cost.ui.grupos

import dev.paraizo.cost.data.GrupoRepo
import dev.paraizo.cost.domain.Grupo
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GruposViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repo: FakeGrupoRepo
    private lateinit var viewModel: GruposViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repo = FakeGrupoRepo()
        viewModel = GruposViewModel(repo, testDispatcher)
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadComSucessoResultaEmReadyComGrupos() = runTest(testDispatcher) {
        repo.grupos = listOf(Grupo("1", "Casa"), Grupo("2", "Trabalho"))
        viewModel.load()
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.state.value
        assertTrue(state is GruposUiState.Ready)
        assertEquals(2, state.grupos.size)
    }

    @Test
    fun loadComListaVaziaResultaEmReadyVazio() = runTest(testDispatcher) {
        repo.grupos = emptyList()
        viewModel.load()
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.state.value
        assertTrue(state is GruposUiState.Ready)
        assertEquals(emptyList(), state.grupos)
    }

    @Test
    fun loadComErroDoRepoResultaEmError() = runTest(testDispatcher) {
        repo.listError = RuntimeException("Falha de rede")
        viewModel.load()
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.state.value
        assertTrue(state is GruposUiState.Error)
        assertTrue(state.message.isNotEmpty())
    }

    @Test
    fun criarComNomeValidoPersistERecarrega() = runTest(testDispatcher) {
        val novoGrupo = Grupo("3", "Viagem")
        repo.nextCreated = novoGrupo
        viewModel.criar("Viagem")
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(repo.createCalled)
        val state = viewModel.state.value
        assertTrue(state is GruposUiState.Ready)
        assertTrue(state.grupos.contains(novoGrupo))
    }

    @Test
    fun criarComNomeVazioNaoChama() = runTest(testDispatcher) {
        viewModel.criar("")
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(repo.createCalled)
    }

    @Test
    fun criarComApenasEspacosNaoChama() = runTest(testDispatcher) {
        viewModel.criar("   ")
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(repo.createCalled)
    }
}

private class FakeGrupoRepo : GrupoRepo {
    var grupos: List<Grupo> = emptyList()
    var listError: Throwable? = null
    var nextCreated: Grupo = Grupo("new", "Novo")
    var createCalled: Boolean = false

    override suspend fun create(grupo: Grupo): Grupo {
        createCalled = true
        grupos = grupos + nextCreated
        return nextCreated
    }

    override suspend fun list(): List<Grupo> {
        listError?.let { throw it }
        return grupos
    }
}
