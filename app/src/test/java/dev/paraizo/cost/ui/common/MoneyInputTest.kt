package dev.paraizo.cost.ui.common

import dev.paraizo.cost.domain.Money
import kotlin.test.Test
import kotlin.test.assertEquals

class MoneyInputTest {

    @Test
    fun parseCentavosDeStringDecimalRetornaCentavos() {
        assertEquals(1234L, parseCentavos("12,34"))
    }

    @Test
    fun parseCentavosDeStringVaziaRetornaZero() {
        assertEquals(0L, parseCentavos(""))
    }

    @Test
    fun parseCentavosDeZeroVirgulaDoisDigitosRetornaCentavos() {
        assertEquals(50L, parseCentavos("0,50"))
    }

    @Test
    fun parseCentavosDeInteiroSemDecimalRetornaCentavosComZeros() {
        assertEquals(1000L, parseCentavos("10"))
    }

    @Test
    fun formatReaisDeMoneyRetornaStringFormatada() {
        assertEquals("R$ 12,34", formatReais(Money(1234)))
    }

    @Test
    fun formatReaisDeZeroRetornaZeroFormatado() {
        assertEquals("R$ 0,00", formatReais(Money(0)))
    }

    @Test
    fun formatReaisDeValorGrandeRetornaFormatado() {
        assertEquals("R$ 1000,00", formatReais(Money(100000)))
    }
}
