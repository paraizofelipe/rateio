package dev.paraizo.cost.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class OrcamentoTest {

    @Test
    fun semGastoBarraCheiaVerde() {
        val o = calcularOrcamento(Money(1000), Money.ZERO)
        assertEquals(Money(1000), o.restante)
        assertEquals(Money.ZERO, o.excedente)
        assertEquals(1.0f, o.fracaoRestante, 0.001f)
        assertEquals(NivelOrcamento.SAUDAVEL, o.nivel)
    }

    @Test
    fun gasto59PorCentoSaudavel() {
        val o = calcularOrcamento(Money(1000), Money(590))
        assertEquals(NivelOrcamento.SAUDAVEL, o.nivel)
        assertEquals(Money(410), o.restante)
        assertEquals(0.41f, o.fracaoRestante, 0.001f)
    }

    @Test
    fun gasto60PorCentoAtencao() {
        val o = calcularOrcamento(Money(1000), Money(600))
        assertEquals(NivelOrcamento.ATENCAO, o.nivel)
    }

    @Test
    fun gasto85PorCentoAtencao() {
        val o = calcularOrcamento(Money(1000), Money(850))
        assertEquals(NivelOrcamento.ATENCAO, o.nivel)
    }

    @Test
    fun gasto86PorCentoCritico() {
        val o = calcularOrcamento(Money(1000), Money(851))
        assertEquals(NivelOrcamento.CRITICO, o.nivel)
    }

    @Test
    fun estouroZeraBarraECalculaExcedente() {
        val o = calcularOrcamento(Money(1000), Money(1200))
        assertEquals(Money.ZERO, o.restante)
        assertEquals(Money(200), o.excedente)
        assertEquals(0.0f, o.fracaoRestante, 0.001f)
        assertEquals(NivelOrcamento.CRITICO, o.nivel)
    }

    @Test
    fun rendaZeroResultaSemRenda() {
        val o = calcularOrcamento(Money.ZERO, Money(500))
        assertEquals(NivelOrcamento.SEM_RENDA, o.nivel)
        assertEquals(0.0f, o.fracaoRestante, 0.001f)
        assertEquals(Money.ZERO, o.restante)
        assertEquals(Money.ZERO, o.excedente)
    }

    @Test
    fun valoresIntermediarios() {
        val o = calcularOrcamento(Money(1000), Money(300))
        assertEquals(Money(700), o.restante)
        assertEquals(0.7f, o.fracaoRestante, 0.001f)
        assertEquals(NivelOrcamento.SAUDAVEL, o.nivel)
    }
}
