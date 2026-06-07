package dev.paraizo.cost.ui.pessoas

import dev.paraizo.cost.data.GastoRepo
import dev.paraizo.cost.data.PessoaRepo
import dev.paraizo.cost.domain.Gasto
import dev.paraizo.cost.domain.Money
import dev.paraizo.cost.domain.Pessoa
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import java.math.BigDecimal
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PessoasViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repo: FakePessoaRepo
    private lateinit var gastoRepo: FakeGastoRepo
    private lateinit var viewModel: PessoasViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repo = FakePessoaRepo()
        gastoRepo = FakeGastoRepo()
        viewModel = PessoasViewModel(repo, gastoRepo, "g1", testDispatcher)
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun carregarComTresPessoasRetornaRowsComPercentuaisQueSomam100() = runTest(testDispatcher) {
        repo.pessoas = listOf(
            Pessoa("1", "Ana", Money(6000), "g1"),
            Pessoa("2", "Bob", Money(3000), "g1"),
            Pessoa("3", "Cia", Money(1000), "g1")
        )
        viewModel.load()
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.state.value
        assertTrue(state is PessoasUiState.Ready)
        val soma = state.rows.fold(BigDecimal.ZERO) { acc, row -> acc + row.percentual }
        assertEquals(0, soma.compareTo(BigDecimal("100")))
        assertFalse(state.rendaTotalZero)
    }

    @Test
    fun carregarComRendaTotalZeroMarcaRendaTotalZeroTrue() = runTest(testDispatcher) {
        repo.pessoas = listOf(
            Pessoa("1", "Ana", Money(0), "g1"),
            Pessoa("2", "Bob", Money(0), "g1")
        )
        viewModel.load()
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.state.value
        assertTrue(state is PessoasUiState.Ready)
        assertTrue(state.rendaTotalZero)
        assertTrue(state.rows.all { it.percentual.compareTo(BigDecimal.ZERO) == 0 })
    }

    @Test
    fun salvarPessoaValidaChamaCriarERecarrega() = runTest(testDispatcher) {
        repo.nextCreated = Pessoa("new", "Ana", Money(5000), "g1")
        viewModel.salvar("Ana", 5000L)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(repo.createCalled)
        val state = viewModel.state.value
        assertTrue(state is PessoasUiState.Ready)
        assertTrue(state.rows.any { it.pessoa.nome == "Ana" })
    }

    @Test
    fun salvarComNomeVazioNaoPersiste() = runTest(testDispatcher) {
        viewModel.salvar("", 1000L)
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(repo.createCalled)
    }

    @Test
    fun salvarComRendaNegativaNaoPersiste() = runTest(testDispatcher) {
        viewModel.salvar("Bob", -1L)
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(repo.createCalled)
    }

    @Test
    fun carregarComErroDoRepoResultaEmError() = runTest(testDispatcher) {
        repo.listError = RuntimeException("Falha de rede")
        viewModel.load()
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.state.value
        assertTrue(state is PessoasUiState.Error)
        assertTrue(state.message.isNotEmpty())
    }

    @Test
    fun editarPessoaValidaAtualizaERecarrega() = runTest(testDispatcher) {
        repo.pessoas = listOf(Pessoa("1", "Ana", Money(5000), "g1"))
        viewModel.editar("1", "Ana Maria", 7000L)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(Pessoa("1", "Ana Maria", Money(7000), "g1"), repo.updated)
        val state = viewModel.state.value
        assertTrue(state is PessoasUiState.Ready)
        assertTrue(state.rows.any { it.pessoa.nome == "Ana Maria" })
    }

    @Test
    fun editarComNomeVazioNaoChama() = runTest(testDispatcher) {
        viewModel.editar("1", "  ", 1000L)
        testDispatcher.scheduler.advanceUntilIdle()
        assertNull(repo.updated)
    }

    @Test
    fun prepararExclusaoContaGastosDaPessoa() = runTest(testDispatcher) {
        gastoRepo.gastos = listOf(
            Gasto("e1", "Luz", Money(500), "p1", "g1", "2026-06"),
            Gasto("e2", "Água", Money(300), "p1", "g1", "2026-05"),
            Gasto("e3", "Net", Money(120), "p2", "g1", "2026-06")
        )
        viewModel.prepararExclusao(Pessoa("p1", "Ana", Money(5000), "g1"))
        testDispatcher.scheduler.advanceUntilIdle()
        val exclusao = viewModel.exclusao.value
        assertTrue(exclusao != null)
        assertEquals(2, exclusao.qtdGastos)
    }

    @Test
    fun confirmarExclusaoRemoveGastosDaPessoaEAPessoa() = runTest(testDispatcher) {
        repo.pessoas = listOf(Pessoa("p1", "Ana", Money(5000), "g1"))
        gastoRepo.gastos = listOf(
            Gasto("e1", "Luz", Money(500), "p1", "g1", "2026-06"),
            Gasto("e3", "Net", Money(120), "p2", "g1", "2026-06")
        )
        viewModel.prepararExclusao(Pessoa("p1", "Ana", Money(5000), "g1"))
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.confirmarExclusao()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(gastoRepo.deletados.contains("e1"))
        assertFalse(gastoRepo.deletados.contains("e3"))
        assertTrue(repo.deletados.contains("p1"))
        assertNull(viewModel.exclusao.value)
    }
}

private class FakePessoaRepo : PessoaRepo {
    var pessoas: List<Pessoa> = emptyList()
    var listError: Throwable? = null
    var nextCreated: Pessoa = Pessoa("new", "Nova", Money(1000), "g1")
    var createCalled: Boolean = false
    var updated: Pessoa? = null
    val deletados = mutableListOf<String>()

    override suspend fun create(pessoa: Pessoa): Pessoa {
        createCalled = true
        pessoas = pessoas + nextCreated
        return nextCreated
    }

    override suspend fun listByGroup(groupId: String): List<Pessoa> {
        listError?.let { throw it }
        return pessoas
    }

    override suspend fun update(pessoa: Pessoa): Pessoa {
        updated = pessoa
        pessoas = pessoas.map { if (it.id == pessoa.id) pessoa else it }
        return pessoa
    }

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
