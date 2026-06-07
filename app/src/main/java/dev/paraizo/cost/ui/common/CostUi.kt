package dev.paraizo.cost.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.paraizo.cost.ui.theme.CostTheme

/** Cor pastel determinística a partir do nome — espelha o hsl(...) do protótipo. */
fun avatarColor(seed: String): Color {
    val code = seed.firstOrNull()?.code ?: 0
    val hue = ((code * 37) % 360).toFloat()
    return Color.hsl(hue, 0.55f, 0.88f)
}

/** Avatar circular com a inicial do nome. */
@Composable
fun InitialAvatar(name: String, size: Dp = 42.dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(avatarColor(name)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.firstOrNull()?.uppercase() ?: "?",
            color = Color(0xFF37474F),
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.38f).sp,
        )
    }
}

/** "Pill" arredondada de fundo translúcido teal. */
@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    background: Color = CostTheme.extras.pillBg,
    contentColor: Color = CostTheme.extras.pillText,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(background)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text(text = text, color = contentColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

/** FAB quadrado-arredondado com gradiente da marca. */
@Composable
fun BrandFab(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CostTheme.extras.brandGradient)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = Color.White)
    }
}

/** Botão "pill" preenchido com gradiente da marca. */
@Composable
fun BrandButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = MaterialTheme.colorScheme.outline,
        ),
        contentPadding = PaddingValues(),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .then(if (enabled) Modifier.background(CostTheme.extras.brandGradient) else Modifier)
                .fillMaxWidth()
                .padding(vertical = 13.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Menu de contexto (Editar/Excluir) exibido ao manter um item pressionado. */
@Composable
fun ItemActionsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onEditar: () -> Unit,
    onExcluir: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text("Editar") },
            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
            onClick = onEditar,
        )
        DropdownMenuItem(
            text = { Text("Excluir", color = MaterialTheme.colorScheme.error) },
            leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            onClick = onExcluir,
        )
    }
}

/** Estado vazio: ícone grande + título + subtítulo. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = CostTheme.extras.emptyIcon,
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(title, color = CostTheme.extras.textSecondary, fontSize = 15.sp)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, color = CostTheme.extras.textMuted, fontSize = 13.sp)
    }
}
