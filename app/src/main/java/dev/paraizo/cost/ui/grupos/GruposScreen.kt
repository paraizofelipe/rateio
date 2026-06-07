package dev.paraizo.cost.ui.grupos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import dev.paraizo.cost.domain.Grupo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GruposScreen(
    state: GruposUiState,
    onCriar: (String) -> Unit,
    onSelecionar: (String) -> Unit
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var nomeInput by rememberSaveable { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Grupos") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Criar grupo")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (state) {
                is GruposUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.Center)
                    )
                }

                is GruposUiState.Error -> {
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

                is GruposUiState.Ready -> {
                    if (state.grupos.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Nenhum grupo encontrado",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        GruposList(grupos = state.grupos, onSelecionar = onSelecionar)
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
            },
            title = { Text("Novo grupo") },
            text = {
                OutlinedTextField(
                    value = nomeInput,
                    onValueChange = { nomeInput = it },
                    label = { Text("Nome") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onCriar(nomeInput)
                        showDialog = false
                        nomeInput = ""
                    }
                ) {
                    Text("Criar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        nomeInput = ""
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun GruposList(grupos: List<Grupo>, onSelecionar: (String) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(grupos, key = { it.id }) { grupo ->
            Text(
                text = grupo.nome,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelecionar(grupo.id) }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            )
            HorizontalDivider()
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}
