package com.angel.jelaioupas.ui

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.angel.jelaioupas.R

/** Police "Square" (pleine) — pour le mot SCAN / CODE-BARRES, style code-barres. */
val SquareFont = FontFamily(Font(R.font.square))

/** Police "Square Outline" — variante contour, dispo si besoin. */
val SquareOutlineFont = FontFamily(Font(R.font.square_outline))

/** Lato — la police de la charte Press SCAN (titres, boutons, textes). */
val Lato = FontFamily(
    Font(R.font.lato_regular, FontWeight.Normal),
    Font(R.font.lato_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.lato_bold, FontWeight.Bold),
    Font(R.font.lato_black, FontWeight.Black)
)
