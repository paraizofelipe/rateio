package dev.paraizo.cost.ui.gastos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.paraizo.cost.data.GastoRepo
import dev.paraizo.cost.data.PessoaRepo
import dev.paraizo.cost.data.RendaMensalRepo
import dev.paraizo.cost.domain.Gasto
import dev.paraizo.cost.domain.Money
import dev.paraizo.cost.domain.calcularOrcamento
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GastosViewModel(
    private val gastoRepo: GastoRepo,
    private val pessoaRepo: PessoaRepo,
    private val rendaRepo: RendaMensalRepo,
    private val groupId: String,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val competenciaInicial: String =
        YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))

    private val _state = MutableStateFlow<GastosUiState>(GastosUiState.Loading)
    val state: StateFlow<GastosUiState> = _state

    fun load() {
        val competencia = (_state.value as? GastosUiState.Ready)?.competencia ?: competenciaInicial
        carregar(competencia)
    }

    fun selecionarCompetencia(competencia: String) {
        carregar(competencia)
    }

    fun criar(descricao: String, valorCentavos: Long, pagadorId: String?, competencia: String) {
        if (descricao.isBlank()) return
        if (valorCentavos < 0) return
        if (pagadorId.isNullOrBlank()) return
        viewModelScope.launch(dispatcher) {
            try {
                val gasto = Gasto(
                    id = "",
                    descricao = descricao.trim(),
                    valor = Money(valorCentavos),
                    pagadorId = pagadorId,
                    groupId = groupId,
                    competencia = competencia
                )
                gastoRepo.create(gasto)
                // Fotografa a renda das pessoas no primeiro gasto da competência,
                // congelando o rateio deste mês (alterar a renda depois não o afeta).
                val pessoas = pessoaRepo.listByGroup(groupId)
                if (rendaRepo.rendasDe(groupId, competencia).isEmpty()) {
                    rendaRepo.criarSnapshot(groupId, competencia, pessoas.associate { it.id to it.renda.cents })
                }
                val current = (_state.value as? GastosUiState.Ready)?.competencia ?: competenciaInicial
                _state.value = buildReady(current)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = GastosUiState.Error(e.message ?: "Erro ao criar gasto")
            }
        }
    }

    /** Re-fotografa as rendas da competência a partir das rendas atuais (corrige um mês já congelado). */
    fun sincronizarRendas(competencia: String) {
        viewModelScope.launch(dispatcher) {
            try {
                val pessoas = pessoaRepo.listByGroup(groupId)
                rendaRepo.limparSnapshot(groupId, competencia)
                rendaRepo.criarSnapshot(groupId, competencia, pessoas.associate { it.id to it.renda.cents })
                _state.value = buildReady(competencia)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = GastosUiState.Error(e.message ?: "Erro ao sincronizar rendas")
            }
        }
    }

    fun editar(id: String, descricao: String, valorCentavos: Long, pagadorId: String?, competencia: String) {
        if (id.isBlank()) return
        if (descricao.isBlank()) return
        if (valorCentavos < 0) return
        if (pagadorId.isNullOrBlank()) return
        viewModelScope.launch(dispatcher) {
            try {
                gastoRepo.update(
                    Gasto(
                        id = id,
                        descricao = descricao.trim(),
                        valor = Money(valorCentavos),
                        pagadorId = pagadorId,
                        groupId = groupId,
                        competencia = competencia
                    )
                )
                val current = (_state.value as? GastosUiState.Ready)?.competencia ?: competenciaInicial
                _state.value = buildReady(current)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = GastosUiState.Error(e.message ?: "Erro ao editar gasto")
            }
        }
    }

    fun remover(id: String) {
        if (id.isBlank()) return
        viewModelScope.launch(dispatcher) {
            try {
                gastoRepo.delete(id)
                val current = (_state.value as? GastosUiState.Ready)?.competencia ?: competenciaInicial
                _state.value = buildReady(current)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = GastosUiState.Error(e.message ?: "Erro ao excluir gasto")
            }
        }
    }

    private fun carregar(competencia: String) {
        viewModelScope.launch(dispatcher) {
            _state.value = GastosUiState.Loading
            try {
                _state.value = buildReady(competencia)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = GastosUiState.Error(e.message ?: "Erro ao carregar gastos")
            }
        }
    }

    private suspend fun buildReady(competencia: String): GastosUiState.Ready {
        val gastos = gastoRepo.listByGroupAndCompetencia(groupId, competencia)
        val pessoas = pessoaRepo.listByGroup(groupId)
        val totalGasto = gastos.fold(Money.ZERO) { acc, g -> acc + g.valor }
        // Base da renda: snapshot congelado da competência; se ainda não há, soma das rendas atuais.
        val snapshot = rendaRepo.rendasDe(groupId, competencia)
        val rendaTotalCents = if (snapshot.isNotEmpty()) {
            snapshot.values.sum()
        } else {
            pessoas.fold(0L) { acc, p -> acc + p.renda.cents }
        }
        val orcamento = calcularOrcamento(Money(rendaTotalCents), totalGasto)
        return GastosUiState.Ready(
            gastos = gastos,
            pessoas = pessoas,
            competencia = competencia,
            orcamento = orcamento,
        )
    }
}
