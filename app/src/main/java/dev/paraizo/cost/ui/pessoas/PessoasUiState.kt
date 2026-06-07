package dev.paraizo.cost.ui.pessoas

import dev.paraizo.cost.domain.Pessoa
import java.math.BigDecimal

data class PessoaRow(val pessoa: Pessoa, val percentual: BigDecimal)

sealed interface PessoasUiState {
    data object Loading : PessoasUiState
    data class Ready(val rows: List<PessoaRow>, val rendaTotalZero: Boolean) : PessoasUiState
    data class Error(val message: String) : PessoasUiState
}
