package dev.paraizo.cost.ui.gastos

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.paraizo.cost.domain.NivelOrcamento
import dev.paraizo.cost.domain.OrcamentoMensal
import dev.paraizo.cost.ui.common.formatReais
import dev.paraizo.cost.ui.theme.RateioTheme
import kotlin.math.roundToInt

private val CorSaudavel = Color(0xFF18B26B)
private val CorAtencao = Color(0xFFF59E0B)
private val CorCritico = Color(0xFFEF4444)
private val CorSemRenda = Color(0xFFCBD5E1)

@Composable
fun OrcamentoBar(orcamento: OrcamentoMensal, modifier: Modifier = Modifier) {
    val extras = RateioTheme.extras

    val cor = when (orcamento.nivel) {
        NivelOrcamento.SAUDAVEL -> CorSaudavel
        NivelOrcamento.ATENCAO -> CorAtencao
        NivelOrcamento.CRITICO -> CorCritico
        NivelOrcamento.SEM_RENDA -> CorSemRenda
    }

    val fracao by animateFloatAsState(targetValue = orcamento.fracaoRestante, label = "orcamentoFracao")

    val rotulo = when {
        orcamento.nivel == NivelOrcamento.SEM_RENDA -> "Sem renda cadastrada no mês"
        orcamento.excedente.cents > 0L -> "Estourou ${formatReais(orcamento.excedente)}"
        else -> {
            val pct = (orcamento.fracaoRestante * 100).roundToInt()
            "Restam ${formatReais(orcamento.restante)} de ${formatReais(orcamento.rendaTotal)} · $pct%"
        }
    }

    Surface(color = MaterialTheme.colorScheme.background, modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(color = extras.textMuted.copy(alpha = 0.2f))
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(rotulo, color = extras.textSecondary, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .semantics { contentDescription = rotulo },
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = fracao.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(5.dp))
                            .background(cor),
                    )
                }
            }
        }
    }
}
