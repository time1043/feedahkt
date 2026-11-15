package com.time1043.feedah_kt.ui.theme

import androidx.compose.ui.graphics.Color

/** The leaf's lime as a Compose color, for roles that want the fill as a
 * color value (the theme's primary). Also the caret color inside the fields
 * that wear the ring. */
val Lime = Color(0xFFC5F36A)

/** The lime as a raw hex constant, matching the source's LIME in lib/color. */
const val LIME = 0xFFC5F36A
val Black = Color(0xFF000000)
val White = Color(0xFFFFFFFF)
val Gray100 = Color(0xFFF3F4F6)
val Gray300 = Color(0xFFD1D5DB)
val Gray400 = Color(0xFF9CA3AF)
val Gray500 = Color(0xFF6B7280)
val Gray700 = Color(0xFF374151)
val Gray800 = Color(0xFF1F2937)
val Olive = Color(0xFF55741E)

/** The flag's red — the card's bookmark once set, the word list's mark, and a
 * flagged column of a round's sweep (hence the dark variant). */
data class FlagRed(val light: Color, val dark: Color)

val FLAG_RED = FlagRed(light = Color(0xFFE5484D), dark = Color(0xFFF0666B))

/** Picks the theme's half of the pair. */
fun FlagRed.pick(darkTheme: Boolean): Color = if (darkTheme) dark else light

/** The accent for text and icons sitting on the page background: the lime on
 * dark, the deepened olive on light, where the lime would wash out. Matches
 * the source's `text-accent-deep dark:text-accent`. */
val ACCENT = FlagRed(light = Color(0xFF55741E), dark = Color(0xFFC5F36A))

/** A light/dark color pair for the drill cards' option states (the source's
 * tailwind classes). */
data class OptionColor(val light: Color, val dark: Color)

/** Picks the theme's half of the pair. */
fun OptionColor.pick(darkTheme: Boolean): Color = if (darkTheme) dark else light

// The quiz/type option states — the tailwind green and red scales the
// source's choice-card and typing-card paint.
val OPTION_CORRECT_BORDER = OptionColor(Color(0xFF22C55E), Color(0xFF22C55E)) // green-500
val OPTION_CORRECT_BG = OptionColor(Color(0xFFF0FDF4), Color(0xFF052E16)) // green-50 / green-950
val OPTION_CORRECT_TEXT = OptionColor(Color(0xFF15803D), Color(0xFF4ADE80)) // green-700 / green-400
val OPTION_WRONG_BORDER = OptionColor(Color(0xFFEF4444), Color(0xFFEF4444)) // red-500
val OPTION_WRONG_BG = OptionColor(Color(0xFFFEF2F2), Color(0xFF450A0A)) // red-50 / red-950
val OPTION_WRONG_TEXT = OptionColor(Color(0xFFDC2626), Color(0xFFF87171)) // red-600 / red-400
val OPTION_MUTED_TEXT = OptionColor(Color(0xFF9CA3AF), Color(0xFF6B7280)) // gray-400 / gray-500
