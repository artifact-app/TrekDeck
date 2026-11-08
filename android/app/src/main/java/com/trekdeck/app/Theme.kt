package com.trekdeck.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BgTop = Color(0xFF050B1F)
val BgBottom = Color(0xFF0A1E52)
val Surface1 = Color(0xFF0C1736)
val Accent = Color(0xFF38BDF8)
val AccentDeep = Color(0xFF2563EB)
val TextMain = Color(0xFFE6EEFF)
val TextDim = Color(0xFF8FA6D6)

// 0 синий, 1 циан, 2 индиго, 3 бирюзовый, 4 фиолетово-синий, 5 красный
val TilePalette = listOf(
    Color(0xFF3B82F6),
    Color(0xFF22D3EE),
    Color(0xFF6366F1),
    Color(0xFF14B8A6),
    Color(0xFF8B5CF6),
    Color(0xFFEF4444),
)

fun tileColor(i: Int): Color = TilePalette[((i % TilePalette.size) + TilePalette.size) % TilePalette.size]

private val Scheme = darkColorScheme(
    primary = Accent,
    onPrimary = Color(0xFF031225),
    secondary = AccentDeep,
    background = BgTop,
    onBackground = TextMain,
    surface = Surface1,
    onSurface = TextMain,
    surfaceVariant = Color(0xFF13234F),
    onSurfaceVariant = TextDim,
    outline = Color(0xFF29427F),
)

@Composable
fun TrekTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
