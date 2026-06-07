package dev.paraizo.cost.ui.pessoas

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.paraizo.cost.ui.common.MoneyField
import dev.paraizo.cost.ui.common.formatReais
import dev.paraizo.cost.ui.common.parseCentavos
import java.math.RoundingMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PessoasScreen(
    state: PessoasUiState,
    onSalvar: (nome: String, rendaCentavos: Long) -> Unit
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var nomeInput by rememberSaveable { mutableStateOf("") }
    var rendaInput by rememberSaveable { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Pessoas") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar pessoa")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (state) {
                is PessoasUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.Center)
                    )
                }

                is PessoasUiState.Error -> {
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

                is PessoasUiState.Ready -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (state.rendaTotalZero && state.rows.isNotEmpty()) {
                            Text(
                                text = "Cálculo de percentual indisponível: renda total é zero.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        if (state.rows.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Nenhuma pessoa encontrada",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        } else {
                            PessoasList(rows = state.rows, rendaTotalZero = state.rendaTotalZero)
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
                nomeInput = ""
                rendaInput = ""
            },
            title = { Text("Nova pessoa") },
            text = {
                Column {
                    OutlinedTextField(
                        value = nomeInput,
                        onValueChange = { nomeInput = it },
                        label = { Text("Nome") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    MoneyField(
                        value = rendaInput,
                        onValueChange = { rendaInput = it },
                        label = "Renda (R$)",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSalvar(nomeInput, parseCentavos(rendaInput))
                        showDialog = false
                        nomeInput = ""
                        rendaInput = ""
                    }
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        nomeInput = ""
                        rendaInput = ""
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun PessoasList(rows: List<PessoaRow>, rendaTotalZero: Boolean) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(rows, key = { it.pessoa.id }) { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = row.pessoa.nome,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = formatReais(row.pessoa.renda),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!rendaTotalZero) {
                    Text(
                        text = "${row.percentual.setScale(0, RoundingMode.HALF_UP)}%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            HorizontalDivider()
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}
