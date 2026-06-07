package dev.paraizo.cost.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = TealContainer,
    onPrimaryContainer = TealDeep,
    secondary = TealDark,
    onSecondary = Color.White,
    background = SurfaceLight,
    onBackground = Ink,
    surface = SurfaceLight,
    onSurface = Ink,
    surfaceVariant = PaperLight,
    onSurfaceVariant = SlateSecondary,
    error = ErrorRed,
    onError = Color.White,
    outline = GreyMuted,
)

private val DarkColors = darkColorScheme(
    primary = TealA,
    onPrimary = Color(0xFF00251F),
    primaryContainer = TealAStrong,
    onPrimaryContainer = Color.White,
    secondary = TealA,
    onSecondary = Color(0xFF00251F),
    background = SurfaceDark,
    onBackground = InkOnDark,
    surface = SurfaceDark,
    onSurface = InkOnDark,
    surfaceVariant = PaperDark,
    onSurfaceVariant = SlateMuted,
    error = Color(0xFFEF9A9A),
    onError = Color(0xFF3A0A0A),
    outline = Slate,
)

private val RateioShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * Cores fora do [androidx.compose.material3.ColorScheme] usadas pelo design do
 * protótipo (cards do acerto, pills, gradiente da marca). Variam entre claro/escuro.
 */
@Immutable
data class RateioExtras(
    val gradientStart: Color,
    val gradientEnd: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val emptyIcon: Color,
    val card: Color,
    val pillBg: Color,
    val pillText: Color,
    val devidoBg: Color, val devidoLabel: Color, val devidoValue: Color,
    val pagoBg: Color, val pagoLabel: Color, val pagoValue: Color,
    val receberBg: Color, val receberLabel: Color, val receberValue: Color,
    val pagarBg: Color, val pagarLabel: Color, val pagarValue: Color,
    val warnIcon: Color, val okIcon: Color, val okText: Color,
) {
    val brandGradient: Brush get() = Brush.linearGradient(listOf(gradientStart, gradientEnd))
}

private val LightExtras = RateioExtras(
    gradientStart = Teal, gradientEnd = TealLight,
    textPrimary = Ink, textSecondary = SlateSecondary, textMuted = GreyMuted,
    emptyIcon = GreyEmptyIcon, card = PaperLight,
    pillBg = Teal.copy(alpha = 0.10f), pillText = Teal,
    devidoBg = DevidoBg, devidoLabel = DevidoLabel, devidoValue = DevidoValue,
    pagoBg = PagoBg, pagoLabel = PagoLabel, pagoValue = PagoValue,
    receberBg = ReceberBg, receberLabel = ReceberLabel, receberValue = ReceberValue,
    pagarBg = PagarBg, pagarLabel = PagarLabel, pagarValue = PagarValue,
    warnIcon = WarnIcon, okIcon = OkIcon, okText = OkText,
)

private val DarkExtras = RateioExtras(
    gradientStart = TealAStrong, gradientEnd = TealA,
    textPrimary = InkOnDark, textSecondary = SlateMuted, textMuted = Slate,
    emptyIcon = Slate, card = PaperDark,
    pillBg = TealA.copy(alpha = 0.16f), pillText = TealA,
    devidoBg = DevidoBgDark, devidoLabel = DevidoLabelDark, devidoValue = DevidoValueDark,
    pagoBg = PagoBgDark, pagoLabel = PagoLabelDark, pagoValue = PagoValueDark,
    receberBg = ReceberBgDark, receberLabel = ReceberLabelDark, receberValue = ReceberValueDark,
    pagarBg = PagarBgDark, pagarLabel = PagarLabelDark, pagarValue = PagarValueDark,
    warnIcon = WarnIcon, okIcon = OkIcon, okText = Color(0xFF81C784),
)

val LocalRateioExtras = staticCompositionLocalOf { LightExtras }

object RateioTheme {
    val extras: RateioExtras
        @Composable get() = LocalRateioExtras.current
}

@Composable
fun RateioTheme(
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val extras = if (darkTheme) DarkExtras else LightExtras
    androidx.compose.runtime.CompositionLocalProvider(LocalRateioExtras provides extras) {
        MaterialTheme(
            colorScheme = colors,
            shapes = RateioShapes,
            typography = RateioTypography,
            content = content,
        )
    }
}
