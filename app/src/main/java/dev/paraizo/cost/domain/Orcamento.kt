package dev.paraizo.cost.domain

/** Termômetro de consumo da renda no mês. Verde/amarela/vermelha por % gasto; cinza sem renda. */
enum class NivelOrcamento { SAUDAVEL, ATENCAO, CRITICO, SEM_RENDA }

data class OrcamentoMensal(
    val rendaTotal: Money,
    val totalGasto: Money,
    val restante: Money,        // max(0, renda - gasto)
    val excedente: Money,       // max(0, gasto - renda)
    val fracaoRestante: Float,  // 0f..1f -> largura da barra
    val nivel: NivelOrcamento,
)

/**
 * Calcula o estado do orçamento mensal. A barra representa a renda RESTANTE;
 * a cor segue o % JÁ GASTO: <60% saudável, 60%–85% atenção, >85% crítico.
 */
fun calcularOrcamento(rendaTotal: Money, totalGasto: Money): OrcamentoMensal {
    if (rendaTotal.cents <= 0L) {
        return OrcamentoMensal(
            rendaTotal = rendaTotal,
            totalGasto = totalGasto,
            restante = Money.ZERO,
            excedente = Money.ZERO,
            fracaoRestante = 0f,
            nivel = NivelOrcamento.SEM_RENDA,
        )
    }
    val restanteCents = (rendaTotal.cents - totalGasto.cents).coerceAtLeast(0L)
    val excedenteCents = (totalGasto.cents - rendaTotal.cents).coerceAtLeast(0L)
    val fracao = (restanteCents.toDouble() / rendaTotal.cents.toDouble()).toFloat().coerceIn(0f, 1f)
    val percentualGasto = totalGasto.cents.toDouble() / rendaTotal.cents.toDouble()
    val nivel = when {
        percentualGasto < 0.60 -> NivelOrcamento.SAUDAVEL
        percentualGasto <= 0.85 -> NivelOrcamento.ATENCAO
        else -> NivelOrcamento.CRITICO
    }
    return OrcamentoMensal(
        rendaTotal = rendaTotal,
        totalGasto = totalGasto,
        restante = Money(restanteCents),
        excedente = Money(excedenteCents),
        fracaoRestante = fracao,
        nivel = nivel,
    )
}
