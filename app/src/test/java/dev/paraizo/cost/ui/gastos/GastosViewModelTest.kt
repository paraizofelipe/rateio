package dev.paraizo.cost.ui.gastos

import dev.paraizo.cost.data.GastoRepo
import dev.paraizo.cost.data.PessoaRepo
import dev.paraizo.cost.data.RendaMensalRepo
import dev.paraizo.cost.domain.Gasto
import dev.paraizo.cost.domain.Money
import dev.paraizo.cost.domain.Pessoa
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import dev.paraizo.cost.domain.NivelOrcamento
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GastosViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var gastoRepo: FakeGastoRepo
    private lateinit var pessoaRepo: FakePessoaRepo
    private lateinit var rendaRepo: FakeRendaMensalRepo
    private lateinit var viewModel: GastosViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        gastoRepo = FakeGastoRepo()
        pessoaRepo = FakePessoaRepo()
        rendaRepo = FakeRendaMensalRepo()
        viewModel = GastosViewModel(gastoRepo, pessoaRepo, rendaRepo, "g1", testDispatcher)
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun criarGastoValidoChamaCriarERecarrega() = runTest(testDispatcher) {
        val gastoEsperado = Gasto("id1", "Aluguel", Money(150000L), "p1", "g1", "2026-06")
        gastoRepo.nextCreated = gastoEsperado
        viewModel.load()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.criar("Aluguel", 150000L, "p1", "2026-06")
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(gastoRepo.createCalled)
        val state = viewModel.state.value
        assertTrue(state is GastosUiState.Ready)
        assertTrue(state.gastos.any { it.descricao == "Aluguel" })
    }

    @Test
    fun criarComPagadorNuloNaoPersiste() = runTest(testDispatcher) {
        viewModel.criar("Aluguel", 150000L, null, "2026-06")
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(gastoRepo.createCalled)
    }

    @Test
    fun criarComPagadorVazioNaoPersiste() = runTest(testDispatcher) {
        viewModel.criar("Aluguel", 150000L, "", "2026-06")
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(gastoRepo.createCalled)
    }

    @Test
    fun criarComDescricaoVaziaNaoPersiste() = runTest(testDispatcher) {
        viewModel.criar("", 150000L, "p1", "2026-06")
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(gastoRepo.createCalled)
    }

    @Test
    fun selecionarCompetenciaFiltraGastosPorCompetencia() = runTest(testDispatcher) {
        gastoRepo.gastosPorCompetencia = mapOf(
            "2026-05" to listOf(Gasto("a", "Água", Money(5000), "p1", "g1", "2026-05")),
            "2026-06" to listOf(Gasto("b", "Luz", Money(8000), "p1", "g1", "2026-06"))
        )
        viewModel.selecionarCompetencia("2026-06")
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.state.value
        assertTrue(state is GastosUiState.Ready)
        assertEquals(1, state.gastos.size)
        assertEquals("Luz", state.gastos.first().descricao)
        assertEquals("2026-06", state.competencia)
    }

    @Test
    fun carregarComErroDoGastoRepoResultaEmError() = runTest(testDispatcher) {
        gastoRepo.listError = RuntimeException("Falha de rede")
        viewModel.load()
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.state.value
        assertTrue(state is GastosUiState.Error)
        assertTrue(state.message.isNotEmpty())
    }

    @Test
    fun selecionarCompetenciaComErroResultaEmError() = runTest(testDispatcher) {
        gastoRepo.listError = RuntimeException("timeout")
        viewModel.selecionarCompetencia("2026-05")
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value is GastosUiState.Error)
    }

    @Test
    fun pessoasDoGrupoCarregadasNoEstado() = runTest(testDispatcher) {
        pessoaRepo.pessoas = listOf(
            Pessoa("p1", "Ana", Money(5000), "g1"),
            Pessoa("p2", "Bob", Money(3000), "g1")
        )
        viewModel.load()
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.state.value
        assertTrue(state is GastosUiState.Ready)
        assertEquals(2, state.pessoas.size)
    }

    @Test
    fun editarGastoValidoChamaUpdateERecarrega() = runTest(testDispatcher) {
        gastoRepo.gastosPorCompetencia = mapOf(
            "2026-06" to listOf(Gasto("b", "Luz", Money(8000), "p1", "g1", "2026-06"))
        )
        viewModel.selecionarCompetencia("2026-06")
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.editar("b", "Luz e força", 9000L, "p1", "2026-06")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("Luz e força", gastoRepo.updated?.descricao)
        assertEquals("b", gastoRepo.updated?.id)
    }

    @Test
    fun editarComIdVazioNaoChama() = runTest(testDispatcher) {
        viewModel.editar("", "Luz", 9000L, "p1", "2026-06")
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(gastoRepo.updateCalled)
    }

    @Test
    fun removerGastoChamaDeleteERecarrega() = runTest(testDispatcher) {
        gastoRepo.gastosPorCompetencia = mapOf(
            "2026-06" to listOf(Gasto("b", "Luz", Money(8000), "p1", "g1", "2026-06"))
        )
        viewModel.selecionarCompetencia("2026-06")
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.remover("b")
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(gastoRepo.deletados.contains("b"))
    }

    @Test
    fun criarPrimeiroGastoDoMesFotografaRendas() = runTest(testDispatcher) {
        pessoaRepo.pessoas = listOf(
            Pessoa("p1", "Ana", Money(6000), "g1"),
            Pessoa("p2", "Bob", Money(4000), "g1")
        )
        viewModel.criar("Aluguel", 150000L, "p1", "2026-06")
        testDispatcher.scheduler.advanceUntilIdle()
        val snapshot = rendaRepo.snapshots["2026-06"]
        assertEquals(mapOf("p1" to 6000L, "p2" to 4000L), snapshot)
    }

    @Test
    fun criarSegundoGastoNaoRefotografa() = runTest(testDispatcher) {
        pessoaRepo.pessoas = listOf(Pessoa("p1", "Ana", Money(6000), "g1"))
        rendaRepo.snapshots = mutableMapOf("2026-06" to mapOf("p1" to 5000L))
        viewModel.criar("Aluguel", 150000L, "p1", "2026-06")
        testDispatcher.scheduler.advanceUntilIdle()
        // Mantém a foto original (5000), não sobrescreve com a renda atual (6000)
        assertEquals(mapOf("p1" to 5000L), rendaRepo.snapshots["2026-06"])
    }

    @Test
    fun sincronizarRendasRefotografaComRendasAtuais() = runTest(testDispatcher) {
        pessoaRepo.pessoas = listOf(Pessoa("p1", "Ana", Money(8000), "g1"))
        rendaRepo.snapshots = mutableMapOf("2026-06" to mapOf("p1" to 5000L))
        viewModel.sincronizarRendas("2026-06")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(mapOf("p1" to 8000L), rendaRepo.snapshots["2026-06"])
    }

    @Test
    fun orcamentoUsaSnapshotQuandoExiste() = runTest(testDispatcher) {
        // snapshot congelado: renda total 1000; gasto 600 = 60% -> ATENCAO
        rendaRepo.snapshots = mutableMapOf("2026-06" to mapOf("p1" to 600L, "p2" to 400L))
        gastoRepo.gastosPorCompetencia = mapOf(
            "2026-06" to listOf(Gasto("b", "Luz", Money(600), "p1", "g1", "2026-06"))
        )
        viewModel.selecionarCompetencia("2026-06")
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.state.value
        assertTrue(state is GastosUiState.Ready)
        assertEquals(Money(1000), state.orcamento.rendaTotal)
        assertEquals(Money(600), state.orcamento.totalGasto)
        assertEquals(Money(400), state.orcamento.restante)
        assertEquals(NivelOrcamento.ATENCAO, state.orcamento.nivel)
    }

    @Test
    fun orcamentoUsaRendaAtualQuandoSemSnapshot() = runTest(testDispatcher) {
        pessoaRepo.pessoas = listOf(Pessoa("p1", "Ana", Money(1000), "g1"))
        gastoRepo.gastosPorCompetencia = mapOf(
            "2026-06" to listOf(Gasto("b", "Luz", Money(300), "p1", "g1", "2026-06"))
        )
        viewModel.selecionarCompetencia("2026-06")
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.state.value
        assertTrue(state is GastosUiState.Ready)
        assertEquals(Money(1000), state.orcamento.rendaTotal)
        assertEquals(NivelOrcamento.SAUDAVEL, state.orcamento.nivel)
    }
}

private class FakeGastoRepo : GastoRepo {
    var gastosPorCompetencia: Map<String, List<Gasto>> = emptyMap()
    var listError: Throwable? = null
    var createCalled: Boolean = false
    var nextCreated: Gasto = Gasto("new", "Novo", Money(1000), "p1", "g1", "2026-06")
    var updated: Gasto? = null
    var updateCalled: Boolean = false
    val deletados = mutableListOf<String>()

    override suspend fun create(gasto: Gasto): Gasto {
        createCalled = true
        val competencia = gasto.competencia
        gastosPorCompetencia = gastosPorCompetencia.toMutableMap().also { map ->
            map[competencia] = (map[competencia] ?: emptyList()) + nextCreated
        }
        return nextCreated
    }

    override suspend fun listByGroupAndCompetencia(groupId: String, competencia: String): List<Gasto> {
        listError?.let { throw it }
        return gastosPorCompetencia[competencia] ?: emptyList()
    }

    override suspend fun listByGroup(groupId: String): List<Gasto> {
        listError?.let { throw it }
        return gastosPorCompetencia.values.flatten()
    }

    override suspend fun update(gasto: Gasto): Gasto {
        updateCalled = true
        updated = gasto
        return gasto
    }

    override suspend fun delete(id: String) {
        deletados += id
        gastosPorCompetencia = gastosPorCompetencia.mapValues { (_, list) -> list.filterNot { it.id == id } }
    }
}

private class FakePessoaRepo : PessoaRepo {
    var pessoas: List<Pessoa> = emptyList()

    override suspend fun create(pessoa: Pessoa): Pessoa = pessoa
    override suspend fun listByGroup(groupId: String): List<Pessoa> = pessoas
    override suspend fun update(pessoa: Pessoa): Pessoa = pessoa
    override suspend fun delete(id: String) {}
}

private class FakeRendaMensalRepo : RendaMensalRepo {
    var snapshots: MutableMap<String, Map<String, Long>> = mutableMapOf()

    override suspend fun rendasDe(groupId: String, competencia: String): Map<String, Long> =
        snapshots[competencia] ?: emptyMap()

    override suspend fun criarSnapshot(groupId: String, competencia: String, rendas: Map<String, Long>) {
        snapshots[competencia] = rendas
    }

    override suspend fun limparSnapshot(groupId: String, competencia: String) {
        snapshots.remove(competencia)
    }
}
