package dev.paraizo.cost.domain

import java.math.BigDecimal
import java.math.RoundingMode

fun percentuais(pessoas: List<Pessoa>): Map<String, BigDecimal> {
    val rendaTotal = pessoas.fold(BigDecimal.ZERO) { acc, p -> acc + p.renda.toBigDecimal() }
    if (rendaTotal.compareTo(BigDecimal.ZERO) == 0) {
        return pessoas.associate { it.id to BigDecimal.ZERO }
    }
    val cem = BigDecimal("100")
    return pessoas.associate { p ->
        p.id to (p.renda.toBigDecimal() * cem).divide(rendaTotal, 10, RoundingMode.HALF_UP)
    }
}

fun ratear(total: Money, pessoas: List<Pessoa>): Map<String, Money> {
    if (pessoas.isEmpty()) return emptyMap()

    val rendaTotal = pessoas.fold(BigDecimal.ZERO) { acc, p -> acc + p.renda.toBigDecimal() }
    if (rendaTotal.compareTo(BigDecimal.ZERO) == 0 || total.cents == 0L) {
        return pessoas.associate { it.id to Money.ZERO }
    }

    val totalBd = total.toBigDecimal()

    data class Parcela(val id: String, val base: Long, val resto: BigDecimal, val originalIndex: Int)

    val parcelas = pessoas.mapIndexed { index, pessoa ->
        val quota = (pessoa.renda.toBigDecimal() * totalBd).divide(rendaTotal, 20, RoundingMode.FLOOR)
        val base = quota.toLong()
        val resto = quota - BigDecimal(base)
        Parcela(pessoa.id, base, resto, index)
    }

    val somaBase = parcelas.sumOf { it.base }
    var restante = total.cents - somaBase

    val sorted = parcelas.sortedWith(
        compareByDescending<Parcela> { it.resto }.thenBy { it.originalIndex }
    )

    val resultado = parcelas.associate { it.id to it.base }.toMutableMap()
    for (p in sorted) {
        if (restante <= 0L) break
        resultado[p.id] = resultado[p.id]!! + 1L
        restante--
    }
    check(restante == 0L) { "ratear: invariante soma==total violada; restante=$restante" }

    return resultado.mapValues { (_, cents) -> Money(cents) }
}

fun settleUp(pessoas: List<Pessoa>, gastos: List<Gasto>, competencia: String): SettleResult {
    val gastosDoMes = gastos.filter { it.competencia == competencia }
    val totalGasto = gastosDoMes.fold(Money.ZERO) { acc, g -> acc + g.valor }

    val devido = ratear(totalGasto, pessoas)

    val pago = pessoas.associate { p ->
        p.id to gastosDoMes
            .filter { it.pagadorId == p.id }
            .fold(Money.ZERO) { acc, g -> acc + g.valor }
    }

    val saldo = pessoas.associate { p ->
        p.id to Money((pago[p.id]?.cents ?: 0L) - (devido[p.id]?.cents ?: 0L))
    }

    val credores = saldo.entries
        .filter { it.value.cents > 0L }
        .sortedByDescending { it.value.cents }
        .map { it.key to it.value.cents }
        .toMutableList()

    val devedores = saldo.entries
        .filter { it.value.cents < 0L }
        .sortedBy { it.value.cents }
        .map { it.key to -it.value.cents }
        .toMutableList()

    val transferencias = mutableListOf<Transferencia>()

    var ci = 0
    var di = 0
    var credorSaldo = if (credores.isNotEmpty()) credores[ci].second else 0L
    var devedorSaldo = if (devedores.isNotEmpty()) devedores[di].second else 0L

    while (ci < credores.size && di < devedores.size) {
        val valor = minOf(credorSaldo, devedorSaldo)
        if (valor > 0L) {
            transferencias.add(Transferencia(devedores[di].first, credores[ci].first, Money(valor)))
        }
        credorSaldo -= valor
        devedorSaldo -= valor
        if (credorSaldo == 0L) {
            ci++
            credorSaldo = if (ci < credores.size) credores[ci].second else 0L
        }
        if (devedorSaldo == 0L) {
            di++
            devedorSaldo = if (di < devedores.size) devedores[di].second else 0L
        }
    }

    return SettleResult(
        devidoPorPessoa = devido,
        pagoPorPessoa = pago,
        saldoPorPessoa = saldo,
        transferencias = transferencias
    )
}
