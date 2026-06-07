package dev.paraizo.cost.ui.grupos

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.paraizo.cost.domain.Grupo
import dev.paraizo.cost.ui.common.BrandFab
import dev.paraizo.cost.ui.common.EmptyState
import dev.paraizo.cost.ui.common.ItemActionsMenu
import dev.paraizo.cost.ui.theme.RateioTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GruposScreen(
    state: GruposUiState,
    onCriar: (String) -> Unit,
    onSelecionar: (String) -> Unit,
    onEditar: (id: String, nome: String) -> Unit = { _, _ -> },
    exclusao: ExclusaoGrupo? = null,
    onPrepararExclusao: (Grupo) -> Unit = {},
    onConfirmarExclusao: () -> Unit = {},
    onCancelarExclusao: () -> Unit = {},
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var grupoEmEdicaoId by rememberSaveable { mutableStateOf<String?>(null) }
    var nomeInput by rememberSaveable { mutableStateOf("") }
    val extras = RateioTheme.extras

    fun abrirCriacao() {
        grupoEmEdicaoId = null
        nomeInput = ""
        showDialog = true
    }

    fun abrirEdicao(grupo: Grupo) {
        grupoEmEdicaoId = grupo.id
        nomeInput = grupo.nome
        showDialog = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Grupos", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            BrandFab(onClick = { abrirCriacao() }, icon = Icons.Default.Add, contentDescription = "Criar grupo")
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (state) {
                is GruposUiState.Loading ->
                    CircularProgressIndicator(modifier = Modifier.size(48.dp).align(Alignment.Center))

                is GruposUiState.Error ->
                    Box(Modifier.fillMaxSize().padding(24.dp), Alignment.Center) {
                        Text(state.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }

                is GruposUiState.Ready ->
                    if (state.grupos.isEmpty()) {
                        EmptyState(
                            icon = Icons.Rounded.Groups,
                            title = "Nenhum grupo encontrado",
                            subtitle = "Toque em \"+\" para criar um grupo",
                        )
                    } else {
                        GruposList(
                            grupos = state.grupos,
                            onSelecionar = onSelecionar,
                            onEditar = { abrirEdicao(it) },
                            onExcluir = onPrepararExclusao,
                        )
                    }
            }
        }
    }

    if (showDialog) {
        val editando = grupoEmEdicaoId != null
        val fechar = { showDialog = false; grupoEmEdicaoId = null; nomeInput = "" }
        AlertDialog(
            onDismissRequest = fechar,
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text(if (editando) "Editar grupo" else "Novo grupo", fontWeight = FontWeight.SemiBold, color = extras.textPrimary) },
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
                Button(
                    onClick = {
                        val id = grupoEmEdicaoId
                        if (id != null) onEditar(id, nomeInput) else onCriar(nomeInput)
                        fechar()
                    },
                    enabled = nomeInput.isNotBlank(),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) { Text(if (editando) "Salvar" else "Criar") }
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
            title = { Text("Excluir grupo", fontWeight = FontWeight.SemiBold, color = extras.textPrimary) },
            text = {
                Text(
                    "Excluir \"${exclusao.grupo.nome}\"? Isso também removerá " +
                        "${exclusao.qtdPessoas} ${plural(exclusao.qtdPessoas, "pessoa", "pessoas")} e " +
                        "${exclusao.qtdGastos} ${plural(exclusao.qtdGastos, "gasto", "gastos")} " +
                        "deste grupo. Esta ação não pode ser desfeita."
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

private fun plural(n: Int, singular: String, plural: String) = if (n == 1) singular else plural

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GruposList(
    grupos: List<Grupo>,
    onSelecionar: (String) -> Unit,
    onEditar: (Grupo) -> Unit,
    onExcluir: (Grupo) -> Unit,
) {
    val extras = RateioTheme.extras
    var menuParaId by rememberSaveable { mutableStateOf<String?>(null) }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(grupos, key = { it.id }) { grupo ->
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onSelecionar(grupo.id) },
                            onLongClick = { menuParaId = grupo.id },
                        )
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.size(16.dp))
                    Text(
                        text = grupo.nome,
                        color = extras.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = extras.textMuted)
                }
                ItemActionsMenu(
                    expanded = menuParaId == grupo.id,
                    onDismiss = { menuParaId = null },
                    onEditar = { menuParaId = null; onEditar(grupo) },
                    onExcluir = { menuParaId = null; onExcluir(grupo) },
                )
            }
            HorizontalDivider(modifier = Modifier.padding(start = 72.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}
