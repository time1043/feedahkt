package com.time1043.feedah_kt.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

/** The flag's red — the card's bookmark once set, the word list's mark, and a
 * flagged column of a round's sweep (hence the dark variant). */
data class FlagRed(val light: Color, val dark: Color)

val FLAG_RED = FlagRed(light = Color(0xFFE5484D), dark = Color(0xFFF0666B))
