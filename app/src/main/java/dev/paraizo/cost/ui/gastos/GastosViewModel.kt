package dev.paraizo.cost.ui.gastos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.paraizo.cost.data.GastoRepo
import dev.paraizo.cost.data.PessoaRepo
import dev.paraizo.cost.domain.Gasto
import dev.paraizo.cost.domain.Money
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
                val current = (_state.value as? GastosUiState.Ready)?.competencia ?: competenciaInicial
                _state.value = buildReady(current)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = GastosUiState.Error(e.message ?: "Erro ao criar gasto")
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
        return GastosUiState.Ready(gastos = gastos, pessoas = pessoas, competencia = competencia)
    }
}
