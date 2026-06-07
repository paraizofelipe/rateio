package dev.paraizo.cost.ui.common

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import dev.paraizo.cost.domain.Money
import java.math.BigDecimal

fun parseCentavos(input: String): Long {
    if (input.isBlank()) return 0L
    val normalized = input.replace(",", ".")
    val parsed = normalized.toBigDecimalOrNull() ?: return 0L
    return (parsed * BigDecimal("100")).toLong()
}

fun formatReais(money: Money): String {
    val reais = money.cents / 100
    val centavos = kotlin.math.abs(money.cents % 100)
    return "R$ %d,%02d".format(reais, centavos)
}

@Composable
fun MoneyField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = modifier
    )
}
