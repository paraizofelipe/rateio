package dev.paraizo.cost.ui.settle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.paraizo.cost.domain.Money
import dev.paraizo.cost.domain.Pessoa
import dev.paraizo.cost.ui.common.InitialAvatar
import dev.paraizo.cost.ui.common.Pill
import dev.paraizo.cost.ui.common.formatReais
import dev.paraizo.cost.ui.theme.RateioTheme
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettleScreen(state: SettleUiState, onBack: () -> Unit = {}) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                title = { Text("Acerto de contas", color = RateioTheme.extras.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (state) {
                is SettleUiState.Loading ->
                    androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(48.dp).align(Alignment.Center))

                is SettleUiState.Blocked -> {
                    val msg = when (state.reason) {
                        BlockReason.SEM_PESSOAS -> "Nenhuma pessoa cadastrada no grupo."
                        BlockReason.RENDA_TOTAL_ZERO -> "A renda total do grupo é zero. Cadastre rendas para calcular o acerto."
                    }
                    EstadoCentral(icon = Icons.Rounded.Warning, iconTint = RateioTheme.extras.warnIcon, titulo = msg, tituloColor = RateioTheme.extras.textPrimary)
                }

                is SettleUiState.Error ->
                    Box(Modifier.fillMaxSize().padding(24.dp), Alignment.Center) {
                        Text(state.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }

                is SettleUiState.Ready ->
                    if (state.result.transferencias.isEmpty()) {
                        EstadoCentral(
                            icon = Icons.Rounded.CheckCircle,
                            iconTint = RateioTheme.extras.okIcon,
                            titulo = "Tudo certo!",
                            tituloColor = RateioTheme.extras.okText,
                            subtitulo = "Ninguém deve nada.",
                        )
                    } else {
                        SettleReadyContent(state = state)
                    }
            }
        }
    }
}

@Composable
private fun EstadoCentral(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    titulo: String,
    tituloColor: Color,
    subtitulo: String? = null,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(72.dp))
        Spacer(Modifier.height(16.dp))
        Text(titulo, color = tituloColor, fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        if (subtitulo != null) {
            Spacer(Modifier.height(4.dp))
            Text(subtitulo, color = RateioTheme.extras.textSecondary, fontSize = 14.sp)
        }
    }
}

@Composable
private fun SettleReadyContent(state: SettleUiState.Ready) {
    val extras = RateioTheme.extras
    val pessoasOrdenadas = state.pessoasById.entries.sortedBy { it.value.nome }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Row(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 8.dp)) {
                Pill(text = "Competência: ${state.competencia}")
            }
        }

        // ── Resumo por pessoa ──
        item { SecaoTitulo("Resumo por pessoa") }
        item {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(extras.card)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), MaterialTheme.shapes.large),
            ) {
                pessoasOrdenadas.forEachIndexed { index, (id, pessoa) ->
                    ResumoPessoa(
                        pessoa = pessoa,
                        devido = state.result.devidoPorPessoa[id] ?: Money.ZERO,
                        pago = state.result.pagoPorPessoa[id] ?: Money.ZERO,
                        saldo = state.result.saldoPorPessoa[id] ?: Money.ZERO,
                    )
                    if (index < pessoasOrdenadas.size - 1) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    }
                }
            }
        }

        // ── Transferências ──
        item {
            Spacer(Modifier.height(12.dp))
            SecaoTitulo("Transferências")
        }
        item {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(extras.card)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), MaterialTheme.shapes.large),
            ) {
                state.result.transferencias.forEachIndexed { index, t ->
                    val de = state.pessoasById[t.deId]?.nome ?: t.deId
                    val para = state.pessoasById[t.paraId]?.nome ?: t.paraId
                    LinhaTransferencia(de = de, para = para, valor = t.valor)
                    if (index < state.result.transferencias.size - 1) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    }
                }
            }
        }

        // ── Texto legível ──
        item {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                state.result.transferencias.forEach { t ->
                    val de = state.pessoasById[t.deId]?.nome ?: t.deId
                    val para = state.pessoasById[t.paraId]?.nome ?: t.paraId
                    Text(
                        buildAnnotatedString {
                            withStyle(androidx.compose.ui.text.SpanStyle(color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)) { append(de) }
                            append(" deve ")
                            withStyle(androidx.compose.ui.text.SpanStyle(color = extras.receberLabel, fontWeight = FontWeight.Bold)) { append(formatReais(t.valor)) }
                            append(" para ")
                            withStyle(androidx.compose.ui.text.SpanStyle(color = extras.receberValue, fontWeight = FontWeight.SemiBold)) { append(para) }
                        },
                        fontSize = 13.sp,
                        color = extras.textSecondary,
                        modifier = Modifier.padding(vertical = 3.dp),
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun SecaoTitulo(texto: String) {
    Text(
        texto.uppercase(),
        color = RateioTheme.extras.textSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 8.dp),
    )
}

@Composable
private fun ResumoPessoa(pessoa: Pessoa, devido: Money, pago: Money, saldo: Money) {
    val extras = RateioTheme.extras
    val recebe = saldo.cents >= 0L
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            InitialAvatar(name = pessoa.nome, size = 36.dp)
            Spacer(Modifier.size(12.dp))
            Text(pessoa.nome, color = extras.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricaBox(Modifier.weight(1f), "DEVIDO", formatReais(devido), extras.devidoBg, extras.devidoLabel, extras.devidoValue)
            MetricaBox(Modifier.weight(1f), "PAGO", formatReais(pago), extras.pagoBg, extras.pagoLabel, extras.pagoValue)
            if (recebe) {
                MetricaBox(Modifier.weight(1f), "A RECEBER", formatReais(Money(abs(saldo.cents))), extras.receberBg, extras.receberLabel, extras.receberValue)
            } else {
                MetricaBox(Modifier.weight(1f), "A PAGAR", formatReais(Money(abs(saldo.cents))), extras.pagarBg, extras.pagarLabel, extras.pagarValue)
            }
        }
    }
}

@Composable
private fun MetricaBox(modifier: Modifier, label: String, valor: String, bg: Color, labelColor: Color, valorColor: Color) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(label, color = labelColor, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
        Spacer(Modifier.height(2.dp))
        Text(valor, color = valorColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LinhaTransferencia(de: String, para: String, valor: Money) {
    val extras = RateioTheme.extras
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Pill(text = de, background = extras.pagarBg, contentColor = extras.pagarLabel)
        Spacer(Modifier.size(10.dp))
        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = extras.textMuted, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(10.dp))
        Pill(text = para, background = extras.receberBg, contentColor = extras.receberValue)
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(extras.brandGradient)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(formatReais(valor), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}
