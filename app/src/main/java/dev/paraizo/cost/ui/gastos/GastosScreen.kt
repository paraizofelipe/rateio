package dev.paraizo.cost.ui.gastos

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.paraizo.cost.domain.Gasto
import dev.paraizo.cost.domain.Pessoa
import dev.paraizo.cost.ui.common.MoneyField
import dev.paraizo.cost.ui.common.formatReais
import dev.paraizo.cost.ui.common.parseCentavos
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GastosScreen(
    state: GastosUiState,
    onSelecionarCompetencia: (String) -> Unit,
    onCriar: (descricao: String, valorCentavos: Long, pagadorId: String, competencia: String) -> Unit,
    onVerSettle: (competencia: String) -> Unit = {}
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var descricaoInput by rememberSaveable { mutableStateOf("") }
    var valorInput by rememberSaveable { mutableStateOf("") }
    var pagadorIdSelecionado by rememberSaveable { mutableStateOf("") }

    val competencia = (state as? GastosUiState.Ready)?.competencia ?: ""
    val pessoas = (state as? GastosUiState.Ready)?.pessoas ?: emptyList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gastos") },
                actions = {
                    IconButton(
                        onClick = { onVerSettle(competencia) },
                        enabled = state is GastosUiState.Ready
                    ) {
                        Icon(Icons.Default.List, contentDescription = "Acerto de contas")
                    }
                }
            )
        },
        floatingActionButton = {
            if (state is GastosUiState.Ready) {
                FloatingActionButton(onClick = { showDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Adicionar gasto")
                }
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
            }
            Box(modifier = Modifier.fillMaxSize()) {
                when (state) {
                    is GastosUiState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(48.dp)
                                .align(Alignment.Center)
                        )
                    }
                    is GastosUiState.Error -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    is GastosUiState.Ready -> {
                        if (state.gastos.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Nenhum gasto encontrado",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        } else {
                            GastosList(gastos = state.gastos, pessoas = state.pessoas)
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        GastoDialog(
            pessoas = pessoas,
            descricaoInput = descricaoInput,
            onDescricaoChange = { descricaoInput = it },
            valorInput = valorInput,
            onValorChange = { valorInput = it },
            pagadorIdSelecionado = pagadorIdSelecionado,
            onPagadorSelecionado = { pagadorIdSelecionado = it },
            onConfirmar = {
                if (pagadorIdSelecionado.isNotBlank()) {
                    onCriar(descricaoInput, parseCentavos(valorInput), pagadorIdSelecionado, competencia)
                    showDialog = false
                    descricaoInput = ""
                    valorInput = ""
                    pagadorIdSelecionado = ""
                }
            },
            onDismiss = {
                showDialog = false
                descricaoInput = ""
                valorInput = ""
                pagadorIdSelecionado = ""
            }
        )
    }
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
            .padding(horizontal = 16.dp, vertical = 8.dp)
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
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            meses.forEach { mes ->
                DropdownMenuItem(
                    text = { Text(mes) },
                    onClick = {
                        onSelecionarCompetencia(mes)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GastoDialog(
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo gasto") },
        text = {
            Column {
                OutlinedTextField(
                    value = descricaoInput,
                    onValueChange = onDescricaoChange,
                    label = { Text("Descrição") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                MoneyField(
                    value = valorInput,
                    onValueChange = onValorChange,
                    label = "Valor (R$)",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
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
                    ExposedDropdownMenu(
                        expanded = pagadorExpanded,
                        onDismissRequest = { pagadorExpanded = false }
                    ) {
                        pessoas.forEach { pessoa ->
                            DropdownMenuItem(
                                text = { Text(pessoa.nome) },
                                onClick = {
                                    onPagadorSelecionado(pessoa.id)
                                    pagadorExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirmar) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun GastosList(gastos: List<Gasto>, pessoas: List<Pessoa>) {
    val pessoasPorId = pessoas.associateBy { it.id }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(gastos, key = { it.id }) { gasto ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = gasto.descricao,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = pessoasPorId[gasto.pagadorId]?.nome ?: gasto.pagadorId,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = formatReais(gasto.valor),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            HorizontalDivider()
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

private fun gerarUltimos12Meses(): List<String> {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM")
    val atual = YearMonth.now()
    return (0 until 12).map { atual.minusMonths(it.toLong()).format(formatter) }
}
