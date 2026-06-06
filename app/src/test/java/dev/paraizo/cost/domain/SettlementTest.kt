package dev.paraizo.cost.domain

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettlementTest {

    private fun pessoa(id: String, rendaCents: Long, groupId: String = "g1") =
        Pessoa(id = id, nome = "Pessoa $id", renda = Money(rendaCents), groupId = groupId)

    private fun gasto(id: String, valorCents: Long, pagadorId: String, competencia: String = "2026-06", groupId: String = "g1") =
        Gasto(id = id, descricao = "Gasto $id", valor = Money(valorCents), pagadorId = pagadorId, groupId = groupId, competencia = competencia)

    @Test
    fun percentuaisSumToOneHundredPercent() {
        val pessoas = listOf(pessoa("a", 1000), pessoa("b", 3000))
        val result = percentuais(pessoas)
        val soma = result.values.fold(BigDecimal.ZERO) { acc, v -> acc + v }
        assertTrue((soma - BigDecimal("100")).abs() < BigDecimal("0.01"))
    }

    @Test
    fun percentuaisWithZeroTotalRendaReturnsAllZero() {
        val pessoas = listOf(pessoa("a", 0), pessoa("b", 0))
        val result = percentuais(pessoas)
        result.values.forEach { assertEquals(0, it.compareTo(BigDecimal.ZERO)) }
    }

    @Test
    fun ratearWithEqualRendaAndOddTotalGivesCentToLowestIndex() {
        val pessoas = listOf(pessoa("a", 100), pessoa("b", 100), pessoa("c", 100))
        val total = Money(10)
        val result = ratear(total, pessoas)
        val soma = result.values.fold(Money.ZERO) { acc, v -> acc + v }
        assertEquals(total, soma)
        assertEquals(4L, result["a"]!!.cents)
        assertEquals(3L, result["b"]!!.cents)
        assertEquals(3L, result["c"]!!.cents)
    }

    @Test
    fun ratearWithZeroTotalReturnsAllZero() {
        val pessoas = listOf(pessoa("a", 1000), pessoa("b", 2000))
        val result = ratear(Money(0), pessoas)
        result.values.forEach { assertEquals(Money.ZERO, it) }
        assertEquals(Money.ZERO, result.values.fold(Money.ZERO) { acc, v -> acc + v })
    }

    @Test
    fun ratearWithZeroTotalRendaReturnsAllZero() {
        val pessoas = listOf(pessoa("a", 0), pessoa("b", 0))
        val result = ratear(Money(500), pessoas)
        result.values.forEach { assertEquals(Money.ZERO, it) }
    }

    @Test
    fun ratearDistributesProportionallyAndSumsExact() {
        val pessoas = listOf(pessoa("a", 1000), pessoa("b", 3000))
        val result = ratear(Money(1000), pessoas)
        assertEquals(Money(250), result["a"])
        assertEquals(Money(750), result["b"])
        assertEquals(Money(1000), result.values.fold(Money.ZERO) { acc, v -> acc + v })
    }

    @Test
    fun ratearWithSinglePersonReturnsTotalForThatPerson() {
        val pessoas = listOf(pessoa("a", 5000))
        val result = ratear(Money(500), pessoas)
        assertEquals(Money(500), result["a"])
    }

    @Test
    fun settleUpWithOnePersonHasNoTransfers() {
        val p = pessoa("a", 5000)
        val g = gasto("g1", 300, pagadorId = "a")
        val result = settleUp(listOf(p), listOf(g), "2026-06")
        assertEquals(0, result.transferencias.size)
        assertEquals(Money(300), result.devidoPorPessoa["a"])
        assertEquals(Money(300), result.pagoPorPessoa["a"])
    }

    @Test
    fun settleUpPersonWithNoGastoIsDebtor() {
        val pA = pessoa("a", 1000)
        val pB = pessoa("b", 1000)
        val g = gasto("g1", 200, pagadorId = "a")
        val result = settleUp(listOf(pA, pB), listOf(g), "2026-06")
        assertTrue(result.transferencias.isNotEmpty())
        val transferToA = result.transferencias.filter { it.paraId == "a" }
        assertTrue(transferToA.isNotEmpty())
        assertTrue(transferToA.all { it.deId == "b" })
    }

    @Test
    fun settleUpTransferenciasZeroOutBalances() {
        val pessoas = listOf(pessoa("a", 1000), pessoa("b", 2000), pessoa("c", 3000))
        val gastos = listOf(
            gasto("g1", 6000, pagadorId = "a"),
            gasto("g2", 3000, pagadorId = "b"),
        )
        val result = settleUp(pessoas, gastos, "2026-06")

        val saldoSoma = result.saldoPorPessoa.values.fold(Money.ZERO) { acc, v -> acc + v }
        assertEquals(Money.ZERO, saldoSoma)

        val netPorPessoa = mutableMapOf("a" to 0L, "b" to 0L, "c" to 0L)
        result.transferencias.forEach { t ->
            netPorPessoa[t.paraId] = (netPorPessoa[t.paraId] ?: 0L) + t.valor.cents
            netPorPessoa[t.deId] = (netPorPessoa[t.deId] ?: 0L) - t.valor.cents
        }
        pessoas.forEach { p ->
            val saldoEsperado = result.saldoPorPessoa[p.id]!!.cents
            assertEquals(saldoEsperado, netPorPessoa[p.id] ?: 0L,
                "Transferências não zeraram o saldo de ${p.id}")
        }
    }

    @Test
    fun settleUpIgnoresGastosFromOtherCompetencia() {
        val pA = pessoa("a", 1000)
        val pB = pessoa("b", 1000)
        val gastos = listOf(
            gasto("g1", 200, pagadorId = "a", competencia = "2026-05"),
            gasto("g2", 400, pagadorId = "b", competencia = "2026-06"),
        )
        val result = settleUp(listOf(pA, pB), gastos, "2026-06")
        assertEquals(Money(400), result.pagoPorPessoa.values.fold(Money.ZERO) { acc, v -> acc + v })
        assertEquals(Money.ZERO, result.pagoPorPessoa["a"] ?: Money.ZERO)
        assertEquals(Money(400), result.pagoPorPessoa["b"])
    }
}
