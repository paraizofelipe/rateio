package dev.paraizo.cost.ui.settle

import dev.paraizo.cost.domain.Pessoa
import dev.paraizo.cost.domain.SettleResult

enum class BlockReason { RENDA_TOTAL_ZERO, SEM_PESSOAS }

sealed interface SettleUiState {
    data object Loading : SettleUiState
    data class Ready(
        val result: SettleResult,
        val pessoasById: Map<String, Pessoa>,
        val competencia: String
    ) : SettleUiState
    data class Blocked(val reason: BlockReason) : SettleUiState
    data class Error(val message: String) : SettleUiState
}
