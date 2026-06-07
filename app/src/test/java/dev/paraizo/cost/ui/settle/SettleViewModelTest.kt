package dev.paraizo.cost.ui.settle

import dev.paraizo.cost.data.GastoRepo
import dev.paraizo.cost.data.PessoaRepo
import dev.paraizo.cost.data.RendaMensalRepo
import dev.paraizo.cost.domain.Gasto
import dev.paraizo.cost.domain.Money
import dev.paraizo.cost.domain.Pessoa
import dev.paraizo.cost.domain.SettleResult
import dev.paraizo.cost.domain.Transferencia
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
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettleViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var pessoaRepo: FakePessoaRepo
    private lateinit var gastoRepo: FakeGastoRepo
    private lateinit var rendaRepo: FakeRendaMensalRepo
    private lateinit var viewModel: SettleViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        pessoaRepo = FakePessoaRepo()
        gastoRepo = FakeGastoRepo()
        rendaRepo = FakeRendaMensalRepo()
        viewModel = SettleViewModel(pessoaRepo, gastoRepo, rendaRepo, "g1", testDispatcher)
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun pessoasEGastosValidosResultamEmReady() = runTest(testDispatcher) {
        pessoaRepo.pessoas = listOf(
            Pessoa("p1", "Felipe", Money(600000L), "g1"),
            Pessoa("p2", "Elaine", Money(400000L), "g1")
        )
        gastoRepo.gastos = listOf(
            Gasto("g1", "Aluguel", Money(100000L), "p1", "g1", "2026-06")
        )

        viewModel.carregar("2026-06")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertIs<SettleUiState.Ready>(state)
        assertEquals(2, state.pessoasById.size)
        assertTrue(state.pessoasById.containsKey("p1"))
        assertTrue(state.pessoasById.containsKey("p2"))
        val somasSaldos = state.result.saldoPorPessoa.values.fold(Money.ZERO) { acc, v -> acc + v }
        assertEquals(Money.ZERO, somasSaldos)
    }

    @Test
    fun rendaTotalZeroResultaEmBlocked() = runTest(testDispatcher) {
        pessoaRepo.pessoas = listOf(
            Pessoa("p1", "Ana", Money(0L), "g1"),
            Pessoa("p2", "Bob", Money(0L), "g1")
        )
        gastoRepo.gastos = emptyList()

        viewModel.carregar("2026-06")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertIs<SettleUiState.Blocked>(state)
        assertEquals(BlockReason.RENDA_TOTAL_ZERO, state.reason)
    }

    @Test
    fun semPessoasResultaEmBlocked() = runTest(testDispatcher) {
        pessoaRepo.pessoas = emptyList()
        gastoRepo.gastos = emptyList()

        viewModel.carregar("2026-06")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertIs<SettleUiState.Blocked>(state)
        assertEquals(BlockReason.SEM_PESSOAS, state.reason)
    }

    @Test
    fun grupoDe1PessoaResultaEmReadySemTransferencias() = runTest(testDispatcher) {
        pessoaRepo.pessoas = listOf(
            Pessoa("p1", "Solo", Money(300000L), "g1")
        )
        gastoRepo.gastos = listOf(
            Gasto("g1", "Internet", Money(5000L), "p1", "g1", "2026-06")
        )

        viewModel.carregar("2026-06")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertIs<SettleUiState.Ready>(state)
        assertTrue(state.result.transferencias.isEmpty())
    }

    @Test
    fun erroDoRepoResultaEmError() = runTest(testDispatcher) {
        pessoaRepo.listError = RuntimeException("falha de rede")

        viewModel.carregar("2026-06")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertIs<SettleUiState.Error>(state)
        assertTrue(state.message.isNotEmpty())
    }

    @Test
    fun comSnapshotUsaRendaCongeladaEnaoARendaAtual() = runTest(testDispatcher) {
        // Renda ATUAL do Felipe foi alterada para 8000, mas o mês foi fotografado com 6000.
        pessoaRepo.pessoas = listOf(
            Pessoa("p1", "Felipe", Money(800000L), "g1"),
            Pessoa("p2", "Elaine", Money(400000L), "g1")
        )
        rendaRepo.snapshots["2026-06"] = mapOf("p1" to 600000L, "p2" to 400000L)
        gastoRepo.gastos = listOf(
            Gasto("e1", "Aluguel", Money(100000L), "p1", "g1", "2026-06")
        )

        viewModel.carregar("2026-06")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertIs<SettleUiState.Ready>(state)
        // 60% de 1000,00 = 600,00 (foto 6000/4000). Com a renda atual (8000) seria ~666,67.
        assertEquals(Money(60000L), state.result.devidoPorPessoa["p1"])
        // A renda exibida também reflete a foto, não a atual.
        assertEquals(Money(600000L), state.pessoasById["p1"]?.renda)
    }

    @Test
    fun semSnapshotUsaRendaAtual() = runTest(testDispatcher) {
        pessoaRepo.pessoas = listOf(
            Pessoa("p1", "Felipe", Money(600000L), "g1"),
            Pessoa("p2", "Elaine", Money(400000L), "g1")
        )
        gastoRepo.gastos = listOf(
            Gasto("e1", "Aluguel", Money(100000L), "p1", "g1", "2026-06")
        )

        viewModel.carregar("2026-06")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertIs<SettleUiState.Ready>(state)
        assertEquals(Money(60000L), state.result.devidoPorPessoa["p1"])
    }
}

private class FakePessoaRepo : PessoaRepo {
    var pessoas: List<Pessoa> = emptyList()
    var listError: Throwable? = null

    override suspend fun create(pessoa: Pessoa): Pessoa = pessoa
    override suspend fun listByGroup(groupId: String): List<Pessoa> {
        listError?.let { throw it }
        return pessoas
    }
    override suspend fun update(pessoa: Pessoa): Pessoa = pessoa
    override suspend fun delete(id: String) {}
}

private class FakeGastoRepo : GastoRepo {
    var gastos: List<Gasto> = emptyList()
    var listError: Throwable? = null

    override suspend fun create(gasto: Gasto): Gasto = gasto
    override suspend fun listByGroupAndCompetencia(groupId: String, competencia: String): List<Gasto> {
        listError?.let { throw it }
        return gastos.filter { it.competencia == competencia }
    }
    override suspend fun listByGroup(groupId: String): List<Gasto> {
        listError?.let { throw it }
        return gastos
    }
    override suspend fun update(gasto: Gasto): Gasto = gasto
    override suspend fun delete(id: String) {}
}

private class FakeRendaMensalRepo : RendaMensalRepo {
    val snapshots: MutableMap<String, Map<String, Long>> = mutableMapOf()

    override suspend fun rendasDe(groupId: String, competencia: String): Map<String, Long> =
        snapshots[competencia] ?: emptyMap()

    override suspend fun criarSnapshot(groupId: String, competencia: String, rendas: Map<String, Long>) {
        snapshots[competencia] = rendas
    }

    override suspend fun limparSnapshot(groupId: String, competencia: String) {
        snapshots.remove(competencia)
    }
}
