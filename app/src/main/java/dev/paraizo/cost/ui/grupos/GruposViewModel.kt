package dev.paraizo.cost.ui.grupos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.paraizo.cost.data.GastoRepo
import dev.paraizo.cost.data.GrupoRepo
import dev.paraizo.cost.data.PessoaRepo
import dev.paraizo.cost.domain.Grupo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Confirmação pendente de exclusão de um grupo, com a contagem de dados que serão removidos em cascata. */
data class ExclusaoGrupo(val grupo: Grupo, val qtdPessoas: Int, val qtdGastos: Int)

class GruposViewModel(
    private val repo: GrupoRepo,
    private val pessoaRepo: PessoaRepo,
    private val gastoRepo: GastoRepo,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _state = MutableStateFlow<GruposUiState>(GruposUiState.Loading)
    val state: StateFlow<GruposUiState> = _state

    private val _exclusao = MutableStateFlow<ExclusaoGrupo?>(null)
    val exclusao: StateFlow<ExclusaoGrupo?> = _exclusao

    fun load() {
        viewModelScope.launch(dispatcher) {
            _state.value = GruposUiState.Loading
            try {
                _state.value = GruposUiState.Ready(repo.list())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = GruposUiState.Error(e.message ?: "Erro ao carregar grupos")
            }
        }
    }

    fun criar(nome: String) {
        val nomeTrimmed = nome.trim()
        if (nomeTrimmed.isEmpty()) return
        viewModelScope.launch(dispatcher) {
            try {
                repo.create(Grupo(id = "", nome = nomeTrimmed))
                _state.value = GruposUiState.Ready(repo.list())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = GruposUiState.Error(e.message ?: "Erro ao criar grupo")
            }
        }
    }

    fun editar(id: String, nome: String) {
        val nomeTrimmed = nome.trim()
        if (id.isBlank() || nomeTrimmed.isEmpty()) return
        viewModelScope.launch(dispatcher) {
            try {
                repo.update(Grupo(id = id, nome = nomeTrimmed))
                _state.value = GruposUiState.Ready(repo.list())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = GruposUiState.Error(e.message ?: "Erro ao editar grupo")
            }
        }
    }

    /** Busca quantas pessoas/gastos serão apagados e abre a confirmação de exclusão. */
    fun prepararExclusao(grupo: Grupo) {
        viewModelScope.launch(dispatcher) {
            try {
                val pessoas = pessoaRepo.listByGroup(grupo.id).size
                val gastos = gastoRepo.listByGroup(grupo.id).size
                _exclusao.value = ExclusaoGrupo(grupo, pessoas, gastos)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = GruposUiState.Error(e.message ?: "Erro ao preparar exclusão")
            }
        }
    }

    fun cancelarExclusao() {
        _exclusao.value = null
    }

    /** Remove em cascata: gastos do grupo, pessoas do grupo e por fim o próprio grupo. */
    fun confirmarExclusao() {
        val alvo = _exclusao.value ?: return
        _exclusao.value = null
        viewModelScope.launch(dispatcher) {
            try {
                gastoRepo.listByGroup(alvo.grupo.id).forEach { gastoRepo.delete(it.id) }
                pessoaRepo.listByGroup(alvo.grupo.id).forEach { pessoaRepo.delete(it.id) }
                repo.delete(alvo.grupo.id)
                _state.value = GruposUiState.Ready(repo.list())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = GruposUiState.Error(e.message ?: "Erro ao excluir grupo")
            }
        }
    }
}
