package dev.paraizo.cost.domain

import java.math.BigDecimal

@JvmInline
value class Money(val cents: Long) {
    operator fun plus(other: Money): Money = Money(cents + other.cents)
    operator fun minus(other: Money): Money = Money(cents - other.cents)
    fun toBigDecimal(): BigDecimal = BigDecimal(cents)

    companion object {
        val ZERO: Money = Money(0L)
    }
}
