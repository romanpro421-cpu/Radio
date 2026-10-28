package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class FmReceiverColors(
    val lcdBackground: Color,
    val lcdBorder: Color,
    val phosphorActive: Color,
    val phosphorDim: Color,
    val phosphorGlow: Color,
    val needleColor: Color,
    val signalActive: Color,
    val knobOuter: Color,
    val knobInner: Color,
    val knobHighlight: Color
)

val LocalFmReceiverColors = staticCompositionLocalOf {
    FmReceiverColors(
        lcdBackground = PhosphorGlassBg,
        lcdBorder = PhosphorGlassBorder,
        phosphorActive = PhosphorAmber,
        phosphorDim = PhosphorAmberDim,
        phosphorGlow = PhosphorAmberGlow,
        needleColor = VermilionPrimary,
        signalActive = SignalEmerald,
        knobOuter = KnobMetalOuter,
        knobInner = KnobMetalInner,
        knobHighlight = KnobHighlight
    )
}

private val OndaDarkColorScheme = darkColorScheme(
    primary = VermilionPrimary,
    onPrimary = VermilionOnPrimary,
    primaryContainer = VermilionContainer,
    onPrimaryContainer = VermilionOnContainer,
    secondary = PhosphorAmber,
    onSecondary = ObsidianBg,
    secondaryContainer = ObsidianSurfaceHigh,
    onSecondaryContainer = PhosphorAmber,
    tertiary = SignalEmerald,
    background = ObsidianBg,
    onBackground = TitaniumText,
    surface = ObsidianSurface,
    onSurface = TitaniumText,
    surfaceVariant = ObsidianSurfaceHigh,
    onSurfaceVariant = MutedAluminum,
    outline = ObsidianBezel,
    outlineVariant = KnobMetalOuter
)

private val OndaLightColorScheme = lightColorScheme(
    primary = VermilionLightPrimary,
    onPrimary = Color.White,
    primaryContainer = VermilionOnContainer,
    onPrimaryContainer = VermilionContainer,
    secondary = AmberLightSecondary,
    onSecondary = Color.White,
    secondaryContainer = AluminumLightSurfaceHigh,
    onSecondaryContainer = AluminumLightText,
    tertiary = SignalEmerald,
    background = AluminumLightBg,
    onBackground = AluminumLightText,
    surface = AluminumLightSurface,
    onSurface = AluminumLightText,
    surfaceVariant = AluminumLightSurfaceHigh,
    onSurfaceVariant = AluminumMutedText,
    outline = AluminumMutedText,
    outlineVariant = AluminumLightSurfaceHigh
)

val OndaShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    // Tactile Obsidian receiver palette by default for cohesive hardware display
    val colorScheme = if (darkTheme) OndaDarkColorScheme else OndaLightColorScheme
    val receiverColors = FmReceiverColors(
        lcdBackground = PhosphorGlassBg,
        lcdBorder = PhosphorGlassBorder,
        phosphorActive = PhosphorAmber,
        phosphorDim = PhosphorAmberDim,
        phosphorGlow = PhosphorAmberGlow,
        needleColor = VermilionPrimary,
        signalActive = SignalEmerald,
        knobOuter = KnobMetalOuter,
        knobInner = KnobMetalInner,
        knobHighlight = KnobHighlight
    )

    CompositionLocalProvider(LocalFmReceiverColors provides receiverColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = OndaShapes,
            content = content
        )
    }
}
