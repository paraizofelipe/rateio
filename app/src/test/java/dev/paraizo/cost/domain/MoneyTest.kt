package dev.paraizo.cost.domain

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class MoneyTest {

    @Test
    fun additionReturnsSumOfCents() {
        assertEquals(Money(150), Money(100) + Money(50))
    }

    @Test
    fun subtractionReturnsDifferenceOfCents() {
        assertEquals(Money(70), Money(100) - Money(30))
    }

    @Test
    fun zeroHasCentsEqualToZero() {
        assertEquals(0L, Money.ZERO.cents)
    }

    @Test
    fun toBigDecimalReturnsExactCentsAsBigDecimal() {
        assertEquals(BigDecimal("1234"), Money(1234).toBigDecimal())
    }
}
