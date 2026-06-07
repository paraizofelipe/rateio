package dev.paraizo.cost.ui.settle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.paraizo.cost.data.GastoRepo
import dev.paraizo.cost.data.PessoaRepo
import dev.paraizo.cost.data.RendaMensalRepo
import dev.paraizo.cost.domain.Money
import dev.paraizo.cost.domain.settleUp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettleViewModel(
    private val pessoaRepo: PessoaRepo,
    private val gastoRepo: GastoRepo,
    private val rendaRepo: RendaMensalRepo,
    private val groupId: String,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _state = MutableStateFlow<SettleUiState>(SettleUiState.Loading)
    val state: StateFlow<SettleUiState> = _state

    fun carregar(competencia: String) {
        viewModelScope.launch(dispatcher) {
            _state.value = SettleUiState.Loading
            try {
                val pessoasAtuais = pessoaRepo.listByGroup(groupId)
                if (pessoasAtuais.isEmpty()) {
                    _state.value = SettleUiState.Blocked(BlockReason.SEM_PESSOAS)
                    return@launch
                }
                // Se a competência tem renda "fotografada", usa-a (rateio congelado);
                // senão, usa a renda atual das pessoas (mês ainda sem lançamentos).
                val snapshot = rendaRepo.rendasDe(groupId, competencia)
                val pessoas = if (snapshot.isNotEmpty()) {
                    pessoasAtuais.map { it.copy(renda = Money(snapshot[it.id] ?: it.renda.cents)) }
                } else {
                    pessoasAtuais
                }
                val rendaTotal = pessoas.sumOf { it.renda.cents }
                if (rendaTotal == 0L) {
                    _state.value = SettleUiState.Blocked(BlockReason.RENDA_TOTAL_ZERO)
                    return@launch
                }
                val gastos = gastoRepo.listByGroupAndCompetencia(groupId, competencia)
                val result = settleUp(pessoas, gastos, competencia)
                _state.value = SettleUiState.Ready(
                    result = result,
                    pessoasById = pessoas.associateBy { it.id },
                    competencia = competencia
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = SettleUiState.Error(e.message ?: "Erro desconhecido")
            }
        }
    }
}
