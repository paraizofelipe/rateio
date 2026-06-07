package dev.paraizo.cost.ui.grupos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.paraizo.cost.data.GrupoRepo
import dev.paraizo.cost.domain.Grupo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GruposViewModel(
    private val repo: GrupoRepo,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _state = MutableStateFlow<GruposUiState>(GruposUiState.Loading)
    val state: StateFlow<GruposUiState> = _state

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
}
