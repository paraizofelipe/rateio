package dev.paraizo.cost.ui.grupos

import dev.paraizo.cost.data.GastoRepo
import dev.paraizo.cost.data.GrupoRepo
import dev.paraizo.cost.data.PessoaRepo
import dev.paraizo.cost.domain.Gasto
import dev.paraizo.cost.domain.Grupo
import dev.paraizo.cost.domain.Money
import dev.paraizo.cost.domain.Pessoa
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GruposViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repo: FakeGrupoRepo
    private lateinit var pessoaRepo: FakePessoaRepo
    private lateinit var gastoRepo: FakeGastoRepo
    private lateinit var viewModel: GruposViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repo = FakeGrupoRepo()
        pessoaRepo = FakePessoaRepo()
        gastoRepo = FakeGastoRepo()
        viewModel = GruposViewModel(repo, pessoaRepo, gastoRepo, testDispatcher)
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

    @Test
    fun editarComNomeValidoAtualizaERecarrega() = runTest(testDispatcher) {
        repo.grupos = listOf(Grupo("1", "Casa"))
        viewModel.editar("1", "Casa Nova")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(Grupo("1", "Casa Nova"), repo.updated)
        val state = viewModel.state.value
        assertTrue(state is GruposUiState.Ready)
        assertTrue(state.grupos.any { it.nome == "Casa Nova" })
    }

    @Test
    fun editarComNomeVazioNaoChama() = runTest(testDispatcher) {
        viewModel.editar("1", "  ")
        testDispatcher.scheduler.advanceUntilIdle()
        assertNull(repo.updated)
    }

    @Test
    fun prepararExclusaoContaPessoasEGastos() = runTest(testDispatcher) {
        pessoaRepo.pessoas = listOf(Pessoa("p1", "Ana", Money(100), "g1"), Pessoa("p2", "Bob", Money(0), "g1"))
        gastoRepo.gastos = listOf(Gasto("e1", "Luz", Money(500), "p1", "g1", "2026-06"))
        viewModel.prepararExclusao(Grupo("g1", "Casa"))
        testDispatcher.scheduler.advanceUntilIdle()
        val exclusao = viewModel.exclusao.value
        assertTrue(exclusao != null)
        assertEquals(2, exclusao.qtdPessoas)
        assertEquals(1, exclusao.qtdGastos)
    }

    @Test
    fun confirmarExclusaoRemoveGastosPessoasEGrupo() = runTest(testDispatcher) {
        repo.grupos = listOf(Grupo("g1", "Casa"))
        pessoaRepo.pessoas = listOf(Pessoa("p1", "Ana", Money(100), "g1"))
        gastoRepo.gastos = listOf(Gasto("e1", "Luz", Money(500), "p1", "g1", "2026-06"))
        viewModel.prepararExclusao(Grupo("g1", "Casa"))
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.confirmarExclusao()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(gastoRepo.deletados.contains("e1"))
        assertTrue(pessoaRepo.deletados.contains("p1"))
        assertTrue(repo.deletados.contains("g1"))
        assertNull(viewModel.exclusao.value)
    }

    @Test
    fun cancelarExclusaoLimpaEstado() = runTest(testDispatcher) {
        viewModel.prepararExclusao(Grupo("g1", "Casa"))
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.cancelarExclusao()
        assertNull(viewModel.exclusao.value)
    }
}

private class FakeGrupoRepo : GrupoRepo {
    var grupos: List<Grupo> = emptyList()
    var listError: Throwable? = null
    var nextCreated: Grupo = Grupo("new", "Novo")
    var createCalled: Boolean = false
    var updated: Grupo? = null
    val deletados = mutableListOf<String>()

    override suspend fun create(grupo: Grupo): Grupo {
        createCalled = true
        grupos = grupos + nextCreated
        return nextCreated
    }

    override suspend fun list(): List<Grupo> {
        listError?.let { throw it }
        return grupos
    }

    override suspend fun update(grupo: Grupo): Grupo {
        updated = grupo
        grupos = grupos.map { if (it.id == grupo.id) grupo else it }
        return grupo
    }

    override suspend fun delete(id: String) {
        deletados += id
        grupos = grupos.filterNot { it.id == id }
    }
}

private class FakePessoaRepo : PessoaRepo {
    var pessoas: List<Pessoa> = emptyList()
    val deletados = mutableListOf<String>()

    override suspend fun create(pessoa: Pessoa): Pessoa = pessoa
    override suspend fun listByGroup(groupId: String): List<Pessoa> = pessoas
    override suspend fun update(pessoa: Pessoa): Pessoa = pessoa
    override suspend fun delete(id: String) {
        deletados += id
        pessoas = pessoas.filterNot { it.id == id }
    }
}

private class FakeGastoRepo : GastoRepo {
    var gastos: List<Gasto> = emptyList()
    val deletados = mutableListOf<String>()

    override suspend fun create(gasto: Gasto): Gasto = gasto
    override suspend fun listByGroupAndCompetencia(groupId: String, competencia: String): List<Gasto> =
        gastos.filter { it.competencia == competencia }
    override suspend fun listByGroup(groupId: String): List<Gasto> = gastos
    override suspend fun update(gasto: Gasto): Gasto = gasto
    override suspend fun delete(id: String) {
        deletados += id
        gastos = gastos.filterNot { it.id == id }
    }
}
