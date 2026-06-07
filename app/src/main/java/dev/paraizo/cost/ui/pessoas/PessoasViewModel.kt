package dev.paraizo.cost.ui.pessoas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.paraizo.cost.data.PessoaRepo
import dev.paraizo.cost.domain.Money
import dev.paraizo.cost.domain.Pessoa
import dev.paraizo.cost.domain.percentuais
import java.math.BigDecimal
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PessoasViewModel(
    private val repo: PessoaRepo,
    private val groupId: String,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _state = MutableStateFlow<PessoasUiState>(PessoasUiState.Loading)
    val state: StateFlow<PessoasUiState> = _state

    fun load() {
        viewModelScope.launch(dispatcher) {
            _state.value = PessoasUiState.Loading
            try {
                _state.value = buildReady()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = PessoasUiState.Error(e.message ?: "Erro ao carregar pessoas")
            }
        }
    }

    fun salvar(nome: String, rendaCentavos: Long) {
        val nomeTrimmed = nome.trim()
        if (nomeTrimmed.isEmpty()) return
        if (rendaCentavos < 0) return
        viewModelScope.launch(dispatcher) {
            try {
                repo.create(Pessoa(id = "", nome = nomeTrimmed, renda = Money(rendaCentavos), groupId = groupId))
                _state.value = buildReady()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = PessoasUiState.Error(e.message ?: "Erro ao salvar pessoa")
            }
        }
    }

    private suspend fun buildReady(): PessoasUiState.Ready {
        val pessoas = repo.listByGroup(groupId)
        val perc = percentuais(pessoas)
        val rendaTotalZero = pessoas.all { it.renda.cents == 0L }
        val rows = pessoas.map { p -> PessoaRow(p, perc[p.id] ?: BigDecimal.ZERO) }
        return PessoasUiState.Ready(rows, rendaTotalZero)
    }
}
