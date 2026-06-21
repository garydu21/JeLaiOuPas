package com.angel.jelaioupas.ui

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Palette officielle Press SCAN. */
object Ps {
    // Possédé (vert)
    val Success = Color(0xFF01903A)
    val SuccessDark = Color(0xFF003F19)
    // Pas possédé (rouge)
    val Danger = Color(0xFFD50505)
    val DangerDark = Color(0xFF890101)
    // Inconnu (orange)
    val Warning = Color(0xFFFEC759)
    val WarningDark = Color(0xFFFF8800)
    // Info (inchangé)
    val Info = Color(0xFF33B5E5)
    val InfoDark = Color(0xFF0099CC)
}

/**
 * Fond en dégradé radial : couleur claire au centre, foncée vers les bords,
 * la couleur foncée étant atteinte un peu avant le bord haut/bas.
 */
fun Modifier.radialBg(center: Color, edge: Color): Modifier = this.drawWithCache {
    val brush = Brush.radialGradient(
        colorStops = arrayOf(
            0.0f to center,
            0.85f to edge,   // foncé atteint avant les bords
            1.0f to edge
        ),
        center = Offset(size.width / 2f, size.height / 2f),
        radius = size.height * 0.62f
    )
    onDrawBehind { drawRect(brush) }
}

/** Donne la variante foncée d'une couleur de la charte (pour le dégradé). */
fun edgeColor(c: Color): Color = when (c) {
    Ps.Success -> Ps.SuccessDark
    Ps.Danger -> Ps.DangerDark
    Ps.Warning -> Ps.WarningDark
    Ps.Info -> Ps.InfoDark
    else -> c
}
