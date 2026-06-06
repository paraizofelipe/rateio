package dev.paraizo.cost.domain

data class Grupo(val id: String, val nome: String)

data class Pessoa(val id: String, val nome: String, val renda: Money, val groupId: String)

data class Gasto(
    val id: String,
    val descricao: String,
    val valor: Money,
    val pagadorId: String,
    val groupId: String,
    val competencia: String
)

data class Transferencia(val deId: String, val paraId: String, val valor: Money)

data class SettleResult(
    val devidoPorPessoa: Map<String, Money>,
    val pagoPorPessoa: Map<String, Money>,
    val saldoPorPessoa: Map<String, Money>,
    val transferencias: List<Transferencia>
)
