package com.time1043.feedah_kt.data.settings

/**
 * The mode screens' read side of the deck-chrome settings (mirror of the
 * deck atoms in src/atoms/deck.ts): the flags the study and browse decks
 * wear, bundled as one value so the screens take a single parameter. Built
 * from Settings at the app shell; the settings tab flips the stored fields.
 */
data class DeckChrome(
    /** Whether the mode screens' header shows at all (back + search stay
     * with it; hiding it hides the whole row). */
    val showHeader: Boolean = true,
    /** Whether the header shows its title (the icons stay either way). */
    val showTitle: Boolean = true,
    /** Whether the deck's scrubbable progress bar shows. */
    val showProgressBar: Boolean = true,
)
