package dev.paraizo.cost.ui.pessoas

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.paraizo.cost.domain.Pessoa
import dev.paraizo.cost.ui.common.BrandFab
import dev.paraizo.cost.ui.common.EmptyState
import dev.paraizo.cost.ui.common.InitialAvatar
import dev.paraizo.cost.ui.common.ItemActionsMenu
import dev.paraizo.cost.ui.common.MoneyField
import dev.paraizo.cost.ui.common.formatReais
import dev.paraizo.cost.ui.common.parseCentavos
import dev.paraizo.cost.ui.theme.CostTheme
import java.math.RoundingMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PessoasScreen(
    state: PessoasUiState,
    onSalvar: (nome: String, rendaCentavos: Long) -> Unit,
    onAbrirGastos: () -> Unit = {},
    onBack: () -> Unit = {},
    onEditar: (id: String, nome: String, rendaCentavos: Long) -> Unit = { _, _, _ -> },
    exclusao: ExclusaoPessoa? = null,
    onPrepararExclusao: (Pessoa) -> Unit = {},
    onConfirmarExclusao: () -> Unit = {},
    onCancelarExclusao: () -> Unit = {},
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var pessoaEmEdicaoId by rememberSaveable { mutableStateOf<String?>(null) }
    var nomeInput by rememberSaveable { mutableStateOf("") }
    var rendaInput by rememberSaveable { mutableStateOf("") }
    val extras = CostTheme.extras

    fun abrirCriacao() {
        pessoaEmEdicaoId = null
        nomeInput = ""
        rendaInput = ""
        showDialog = true
    }

    fun abrirEdicao(pessoa: Pessoa) {
        pessoaEmEdicaoId = pessoa.id
        nomeInput = pessoa.nome
        rendaInput = centavosParaInput(pessoa.renda.cents)
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
                title = { Text("Pessoas", color = extras.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                actions = {
                    OutlinedButton(
                        onClick = onAbrirGastos,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Gastos", fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            BrandFab(onClick = { abrirCriacao() }, icon = Icons.Default.Add, contentDescription = "Adicionar pessoa")
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (state) {
                is PessoasUiState.Loading ->
                    CircularProgressIndicator(modifier = Modifier.size(48.dp).align(Alignment.Center))

                is PessoasUiState.Error ->
                    Box(Modifier.fillMaxSize().padding(24.dp), Alignment.Center) {
                        Text(state.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }

                is PessoasUiState.Ready ->
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (state.rendaTotalZero && state.rows.isNotEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            ) {
                                Text(
                                    "Cálculo de percentual indisponível: renda total é zero.",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                )
                            }
                        }
                        if (state.rows.isEmpty()) {
                            EmptyState(
                                icon = Icons.Rounded.Person,
                                title = "Nenhuma pessoa encontrada",
                                subtitle = "Toque em \"+\" para adicionar",
                            )
                        } else {
                            PessoasList(
                                rows = state.rows,
                                rendaTotalZero = state.rendaTotalZero,
                                onEditar = { abrirEdicao(it) },
                                onExcluir = onPrepararExclusao,
                            )
                        }
                    }
            }
        }
    }

    if (showDialog) {
        val editando = pessoaEmEdicaoId != null
        val fechar = { showDialog = false; pessoaEmEdicaoId = null; nomeInput = ""; rendaInput = "" }
        AlertDialog(
            onDismissRequest = fechar,
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text(if (editando) "Editar pessoa" else "Nova pessoa", fontWeight = FontWeight.SemiBold, color = extras.textPrimary) },
            text = {
                Column {
                    OutlinedTextField(
                        value = nomeInput,
                        onValueChange = { nomeInput = it },
                        label = { Text("Nome") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    MoneyField(
                        value = rendaInput,
                        onValueChange = { rendaInput = it },
                        label = "Renda (R$)",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = pessoaEmEdicaoId
                        if (id != null) onEditar(id, nomeInput, parseCentavos(rendaInput))
                        else onSalvar(nomeInput, parseCentavos(rendaInput))
                        fechar()
                    },
                    enabled = nomeInput.isNotBlank(),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) { Text("Salvar") }
            },
            dismissButton = {
                TextButton(onClick = fechar) { Text("Cancelar", color = MaterialTheme.colorScheme.primary) }
            }
        )
    }

    if (exclusao != null) {
        AlertDialog(
            onDismissRequest = onCancelarExclusao,
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text("Excluir pessoa", fontWeight = FontWeight.SemiBold, color = extras.textPrimary) },
            text = {
                val gastosTxt = if (exclusao.qtdGastos == 1) "1 gasto pago por ela será removido"
                else "${exclusao.qtdGastos} gastos pagos por ela serão removidos"
                Text(
                    "Excluir \"${exclusao.pessoa.nome}\"? $gastosTxt. Esta ação não pode ser desfeita."
                )
            },
            confirmButton = {
                Button(
                    onClick = onConfirmarExclusao,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = onCancelarExclusao) { Text("Cancelar", color = MaterialTheme.colorScheme.primary) }
            }
        )
    }
}

/** Converte centavos para o texto que o MoneyField espera (ex.: 500000 -> "5000.00"). */
private fun centavosParaInput(cents: Long): String {
    if (cents == 0L) return ""
    val reais = cents / 100
    val centavos = cents % 100
    return "%d.%02d".format(reais, centavos)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PessoasList(
    rows: List<PessoaRow>,
    rendaTotalZero: Boolean,
    onEditar: (Pessoa) -> Unit,
    onExcluir: (Pessoa) -> Unit,
) {
    val extras = CostTheme.extras
    var menuParaId by rememberSaveable { mutableStateOf<String?>(null) }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
        items(rows, key = { it.pessoa.id }) { row ->
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onEditar(row.pessoa) },
                            onLongClick = { menuParaId = row.pessoa.id },
                        )
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    InitialAvatar(name = row.pessoa.nome)
                    Spacer(Modifier.size(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(row.pessoa.nome, color = extras.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text(formatReais(row.pessoa.renda), color = extras.textSecondary, fontSize = 13.sp)
                    }
                    if (!rendaTotalZero) {
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 12.dp, vertical = 5.dp),
                        ) {
                            Text(
                                "${row.percentual.setScale(0, RoundingMode.HALF_UP)}%",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                            )
                        }
                    }
                }
                ItemActionsMenu(
                    expanded = menuParaId == row.pessoa.id,
                    onDismiss = { menuParaId = null },
                    onEditar = { menuParaId = null; onEditar(row.pessoa) },
                    onExcluir = { menuParaId = null; onExcluir(row.pessoa) },
                )
            }
            HorizontalDivider(modifier = Modifier.padding(start = 72.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}
