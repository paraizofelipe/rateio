package dev.paraizo.cost.ui.grupos

import dev.paraizo.cost.domain.Grupo

sealed interface GruposUiState {
    data object Loading : GruposUiState
    data class Ready(val grupos: List<Grupo>) : GruposUiState
    data class Error(val message: String) : GruposUiState
}
