package dev.paraizo.cost.data

import dev.paraizo.cost.data.dto.gastoFromDocument
import dev.paraizo.cost.data.dto.gastoToData
import dev.paraizo.cost.data.dto.grupoFromDocument
import dev.paraizo.cost.data.dto.grupoToData
import dev.paraizo.cost.data.dto.pessoaFromDocument
import dev.paraizo.cost.data.dto.pessoaToData
import dev.paraizo.cost.domain.Gasto
import dev.paraizo.cost.domain.Grupo
import dev.paraizo.cost.domain.Money
import dev.paraizo.cost.domain.Pessoa
import kotlin.test.Test
import kotlin.test.assertEquals

class MapperTest {

    @Test
    fun gastoRoundTripPreservesCentavos() {
        val gasto = Gasto(
            id = "g1",
            descricao = "Aluguel",
            valor = Money(12345),
            pagadorId = "p1",
            groupId = "gr1",
            competencia = "2026-05"
        )
        val data = gastoToData(gasto)
        val restored = gastoFromDocument("g1", data)
        assertEquals(12345L, restored.valor.cents)
    }

    @Test
    fun gastoRoundTripWithZeroPreservesCents() {
        val gasto = Gasto(
            id = "g2",
            descricao = "Grátis",
            valor = Money(0),
            pagadorId = "p1",
            groupId = "gr1",
            competencia = "2026-05"
        )
        val data = gastoToData(gasto)
        val restored = gastoFromDocument("g2", data)
        assertEquals(0L, restored.valor.cents)
    }

    @Test
    fun gastoRoundTripPreservesCompetencia() {
        val gasto = Gasto(
            id = "g3",
            descricao = "Internet",
            valor = Money(9900),
            pagadorId = "p2",
            groupId = "gr1",
            competencia = "2025-12"
        )
        val data = gastoToData(gasto)
        val restored = gastoFromDocument("g3", data)
        assertEquals("2025-12", restored.competencia)
    }

    @Test
    fun gastoFromDocumentWithIntValueConvertsToLong() {
        val data: Map<String, Any> = mapOf(
            "descricao" to "Mercado",
            "valorCentavos" to 5000,
            "pagadorId" to "p1",
            "groupId" to "gr1",
            "competencia" to "2026-06"
        )
        val gasto = gastoFromDocument("g4", data)
        assertEquals(5000L, gasto.valor.cents)
    }

    @Test
    fun pessoaRoundTripPreservesRendaCentavos() {
        val pessoa = Pessoa(
            id = "p1",
            nome = "Felipe",
            renda = Money(500000),
            groupId = "gr1"
        )
        val data = pessoaToData(pessoa)
        val restored = pessoaFromDocument("p1", data)
        assertEquals(500000L, restored.renda.cents)
    }

    @Test
    fun pessoaFromDocumentWithIntRendaConvertsToLong() {
        val data: Map<String, Any> = mapOf(
            "nome" to "Ana",
            "rendaCentavos" to 300000,
            "groupId" to "gr1"
        )
        val pessoa = pessoaFromDocument("p2", data)
        assertEquals(300000L, pessoa.renda.cents)
    }

    @Test
    fun grupoRoundTripPreservesNome() {
        val grupo = Grupo(id = "gr1", nome = "Casa")
        val data = grupoToData(grupo)
        val restored = grupoFromDocument("gr1", data)
        assertEquals("Casa", restored.nome)
    }
}
