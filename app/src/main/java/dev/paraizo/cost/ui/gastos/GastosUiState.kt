package dev.paraizo.cost.ui.gastos

import dev.paraizo.cost.domain.Gasto
import dev.paraizo.cost.domain.OrcamentoMensal
import dev.paraizo.cost.domain.Pessoa

sealed interface GastosUiState {
    data object Loading : GastosUiState
    data class Ready(
        val gastos: List<Gasto>,
        val pessoas: List<Pessoa>,
        val competencia: String,
        val orcamento: OrcamentoMensal,
    ) : GastosUiState
    data class Error(val message: String) : GastosUiState
}
