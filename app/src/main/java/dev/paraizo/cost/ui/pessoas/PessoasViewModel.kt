package dev.paraizo.cost.ui.pessoas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.paraizo.cost.data.GastoRepo
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

/** Confirmação pendente de exclusão de uma pessoa, com a contagem de gastos que serão removidos em cascata. */
data class ExclusaoPessoa(val pessoa: Pessoa, val qtdGastos: Int)

class PessoasViewModel(
    private val repo: PessoaRepo,
    private val gastoRepo: GastoRepo,
    private val groupId: String,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _state = MutableStateFlow<PessoasUiState>(PessoasUiState.Loading)
    val state: StateFlow<PessoasUiState> = _state

    private val _exclusao = MutableStateFlow<ExclusaoPessoa?>(null)
    val exclusao: StateFlow<ExclusaoPessoa?> = _exclusao

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

    fun editar(id: String, nome: String, rendaCentavos: Long) {
        val nomeTrimmed = nome.trim()
        if (id.isBlank() || nomeTrimmed.isEmpty()) return
        if (rendaCentavos < 0) return
        viewModelScope.launch(dispatcher) {
            try {
                repo.update(Pessoa(id = id, nome = nomeTrimmed, renda = Money(rendaCentavos), groupId = groupId))
                _state.value = buildReady()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = PessoasUiState.Error(e.message ?: "Erro ao editar pessoa")
            }
        }
    }

    /** Conta quantos gastos (de qualquer competência) são pagos pela pessoa e abre a confirmação. */
    fun prepararExclusao(pessoa: Pessoa) {
        viewModelScope.launch(dispatcher) {
            try {
                val qtd = gastoRepo.listByGroup(groupId).count { it.pagadorId == pessoa.id }
                _exclusao.value = ExclusaoPessoa(pessoa, qtd)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = PessoasUiState.Error(e.message ?: "Erro ao preparar exclusão")
            }
        }
    }

    fun cancelarExclusao() {
        _exclusao.value = null
    }

    /** Remove em cascata: os gastos pagos pela pessoa e por fim a própria pessoa. */
    fun confirmarExclusao() {
        val alvo = _exclusao.value ?: return
        _exclusao.value = null
        viewModelScope.launch(dispatcher) {
            try {
                gastoRepo.listByGroup(groupId)
                    .filter { it.pagadorId == alvo.pessoa.id }
                    .forEach { gastoRepo.delete(it.id) }
                repo.delete(alvo.pessoa.id)
                _state.value = buildReady()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = PessoasUiState.Error(e.message ?: "Erro ao excluir pessoa")
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
