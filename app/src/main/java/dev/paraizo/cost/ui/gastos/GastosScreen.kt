package dev.paraizo.cost.ui.gastos

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.Balance
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.paraizo.cost.domain.Gasto
import dev.paraizo.cost.domain.Money
import dev.paraizo.cost.domain.Pessoa
import dev.paraizo.cost.ui.common.BrandFab
import dev.paraizo.cost.ui.common.EmptyState
import dev.paraizo.cost.ui.common.ItemActionsMenu
import dev.paraizo.cost.ui.common.MoneyField
import dev.paraizo.cost.ui.common.formatReais
import dev.paraizo.cost.ui.common.parseCentavos
import dev.paraizo.cost.ui.theme.CostTheme
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GastosScreen(
    state: GastosUiState,
    onSelecionarCompetencia: (String) -> Unit,
    onCriar: (descricao: String, valorCentavos: Long, pagadorId: String, competencia: String) -> Unit,
    onVerSettle: (competencia: String) -> Unit = {},
    onBack: () -> Unit = {},
    onEditar: (id: String, descricao: String, valorCentavos: Long, pagadorId: String, competencia: String) -> Unit = { _, _, _, _, _ -> },
    onRemover: (id: String) -> Unit = {},
    onSincronizarRendas: (competencia: String) -> Unit = {},
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var gastoEmEdicaoId by rememberSaveable { mutableStateOf<String?>(null) }
    var descricaoInput by rememberSaveable { mutableStateOf("") }
    var valorInput by rememberSaveable { mutableStateOf("") }
    var pagadorIdSelecionado by rememberSaveable { mutableStateOf("") }
    var gastoParaExcluir by remember { mutableStateOf<Gasto?>(null) }
    var topMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var showSyncConfirm by rememberSaveable { mutableStateOf(false) }
    val extras = CostTheme.extras

    val competencia = (state as? GastosUiState.Ready)?.competencia ?: ""
    val pessoas = (state as? GastosUiState.Ready)?.pessoas ?: emptyList()

    fun limparDialog() {
        showDialog = false
        gastoEmEdicaoId = null
        descricaoInput = ""
        valorInput = ""
        pagadorIdSelecionado = ""
    }

    fun abrirCriacao() {
        gastoEmEdicaoId = null
        descricaoInput = ""
        valorInput = ""
        pagadorIdSelecionado = ""
        showDialog = true
    }

    fun abrirEdicao(gasto: Gasto) {
        gastoEmEdicaoId = gasto.id
        descricaoInput = gasto.descricao
        valorInput = centavosParaInput(gasto.valor.cents)
        pagadorIdSelecionado = gasto.pagadorId
        showDialog = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                title = { Text("Gastos", color = extras.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                actions = {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        IconButton(onClick = { onVerSettle(competencia) }, enabled = state is GastosUiState.Ready) {
                            Icon(Icons.Rounded.Balance, contentDescription = "Acerto de contas", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Box {
                        IconButton(onClick = { topMenuExpanded = true }, enabled = state is GastosUiState.Ready) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "Mais opções", tint = MaterialTheme.colorScheme.primary)
                        }
                        DropdownMenu(expanded = topMenuExpanded, onDismissRequest = { topMenuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("Sincronizar rendas do mês") },
                                leadingIcon = { Icon(Icons.Rounded.Sync, contentDescription = null) },
                                onClick = { topMenuExpanded = false; showSyncConfirm = true },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            if (state is GastosUiState.Ready) {
                BrandFab(onClick = { abrirCriacao() }, icon = Icons.Default.Add, contentDescription = "Adicionar gasto")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (state is GastosUiState.Ready) {
                CompetenciaSeletor(
                    competenciaAtual = state.competencia,
                    onSelecionarCompetencia = onSelecionarCompetencia
                )
                val total = state.gastos.fold(Money.ZERO) { acc, g -> acc + g.valor }
                if (state.gastos.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Total do mês", color = extras.textSecondary, fontSize = 13.sp)
                        Text(formatReais(total), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
            Box(modifier = Modifier.fillMaxSize()) {
                when (state) {
                    is GastosUiState.Loading ->
                        CircularProgressIndicator(modifier = Modifier.size(48.dp).align(Alignment.Center))

                    is GastosUiState.Error ->
                        Box(Modifier.fillMaxSize().padding(24.dp), Alignment.Center) {
                            Text(state.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                        }

                    is GastosUiState.Ready ->
                        if (state.gastos.isEmpty()) {
                            EmptyState(
                                icon = Icons.Rounded.ShoppingCart,
                                title = "Nenhum gasto encontrado",
                                subtitle = "Toque em \"+\" para adicionar",
                            )
                        } else {
                            GastosList(
                                gastos = state.gastos,
                                pessoas = state.pessoas,
                                onEditar = { abrirEdicao(it) },
                                onExcluir = { gastoParaExcluir = it },
                            )
                        }
                }
            }
        }
    }

    if (showDialog) {
        val editando = gastoEmEdicaoId != null
        GastoDialog(
            editando = editando,
            pessoas = pessoas,
            descricaoInput = descricaoInput,
            onDescricaoChange = { descricaoInput = it },
            valorInput = valorInput,
            onValorChange = { valorInput = it },
            pagadorIdSelecionado = pagadorIdSelecionado,
            onPagadorSelecionado = { pagadorIdSelecionado = it },
            onConfirmar = {
                if (pagadorIdSelecionado.isNotBlank() && descricaoInput.isNotBlank()) {
                    val id = gastoEmEdicaoId
                    if (id != null) onEditar(id, descricaoInput, parseCentavos(valorInput), pagadorIdSelecionado, competencia)
                    else onCriar(descricaoInput, parseCentavos(valorInput), pagadorIdSelecionado, competencia)
                    limparDialog()
                }
            },
            onDismiss = { limparDialog() }
        )
    }

    if (showSyncConfirm) {
        AlertDialog(
            onDismissRequest = { showSyncConfirm = false },
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text("Sincronizar rendas do mês", fontWeight = FontWeight.SemiBold, color = extras.textPrimary) },
            text = {
                Text(
                    "Atualiza a renda congelada de $competencia com as rendas atuais de cada pessoa. " +
                        "Use isto se corrigiu uma renda e quer que este mês passe a usá-la. Outros meses não são afetados."
                )
            },
            confirmButton = {
                Button(
                    onClick = { onSincronizarRendas(competencia); showSyncConfirm = false },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) { Text("Sincronizar") }
            },
            dismissButton = {
                TextButton(onClick = { showSyncConfirm = false }) { Text("Cancelar", color = MaterialTheme.colorScheme.primary) }
            }
        )
    }

    gastoParaExcluir?.let { alvo ->
        AlertDialog(
            onDismissRequest = { gastoParaExcluir = null },
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text("Excluir gasto", fontWeight = FontWeight.SemiBold, color = extras.textPrimary) },
            text = { Text("Excluir \"${alvo.descricao}\" (${formatReais(alvo.valor)})? Esta ação não pode ser desfeita.") },
            confirmButton = {
                Button(
                    onClick = { onRemover(alvo.id); gastoParaExcluir = null },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = { gastoParaExcluir = null }) { Text("Cancelar", color = MaterialTheme.colorScheme.primary) }
            }
        )
    }
}

/** Converte centavos para o texto que o MoneyField espera (ex.: 150000 -> "1500.00"). */
private fun centavosParaInput(cents: Long): String {
    if (cents == 0L) return ""
    val reais = cents / 100
    val centavos = cents % 100
    return "%d.%02d".format(reais, centavos)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompetenciaSeletor(
    competenciaAtual: String,
    onSelecionarCompetencia: (String) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val meses = remember { gerarUltimos12Meses() }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        OutlinedTextField(
            value = competenciaAtual,
            onValueChange = {},
            readOnly = true,
            label = { Text("Competência") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            meses.forEach { mes ->
                DropdownMenuItem(
                    text = { Text(mes) },
                    onClick = { onSelecionarCompetencia(mes); expanded = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GastoDialog(
    editando: Boolean,
    pessoas: List<Pessoa>,
    descricaoInput: String,
    onDescricaoChange: (String) -> Unit,
    valorInput: String,
    onValorChange: (String) -> Unit,
    pagadorIdSelecionado: String,
    onPagadorSelecionado: (String) -> Unit,
    onConfirmar: () -> Unit,
    onDismiss: () -> Unit
) {
    var pagadorExpanded by rememberSaveable { mutableStateOf(false) }
    val pagadorNome = pessoas.find { it.id == pagadorIdSelecionado }?.nome ?: ""
    val habilitado = descricaoInput.isNotBlank() && pagadorIdSelecionado.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text(if (editando) "Editar gasto" else "Novo gasto", fontWeight = FontWeight.SemiBold, color = CostTheme.extras.textPrimary) },
        text = {
            Column {
                OutlinedTextField(
                    value = descricaoInput,
                    onValueChange = onDescricaoChange,
                    label = { Text("Descrição") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                MoneyField(
                    value = valorInput,
                    onValueChange = onValorChange,
                    label = "Valor (R$)",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                ExposedDropdownMenuBox(
                    expanded = pagadorExpanded,
                    onExpandedChange = { pagadorExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = pagadorNome,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Pagador") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = pagadorExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = pagadorExpanded, onDismissRequest = { pagadorExpanded = false }) {
                        pessoas.forEach { pessoa ->
                            DropdownMenuItem(
                                text = { Text(pessoa.nome) },
                                onClick = { onPagadorSelecionado(pessoa.id); pagadorExpanded = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmar,
                enabled = habilitado,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = MaterialTheme.colorScheme.primary) }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GastosList(
    gastos: List<Gasto>,
    pessoas: List<Pessoa>,
    onEditar: (Gasto) -> Unit,
    onExcluir: (Gasto) -> Unit,
) {
    val extras = CostTheme.extras
    val pessoasPorId = pessoas.associateBy { it.id }
    var menuParaId by rememberSaveable { mutableStateOf<String?>(null) }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(gastos, key = { it.id }) { gasto ->
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onEditar(gasto) },
                            onLongClick = { menuParaId = gasto.id },
                        )
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.size(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(gasto.descricao, color = extras.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("Pago por ${pessoasPorId[gasto.pagadorId]?.nome ?: "—"}", color = extras.textSecondary, fontSize = 12.sp)
                    }
                    Text(formatReais(gasto.valor), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                ItemActionsMenu(
                    expanded = menuParaId == gasto.id,
                    onDismiss = { menuParaId = null },
                    onEditar = { menuParaId = null; onEditar(gasto) },
                    onExcluir = { menuParaId = null; onExcluir(gasto) },
                )
            }
            HorizontalDivider(modifier = Modifier.padding(start = 72.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

private fun gerarUltimos12Meses(): List<String> {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM")
    val atual = YearMonth.now()
    return (0 until 12).map { atual.minusMonths(it.toLong()).format(formatter) }
}
