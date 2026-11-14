package com.time1043.feedah_kt.drills

import androidx.compose.runtime.Composable
import com.time1043.feedah_kt.logic.AnswerState
import com.time1043.feedah_kt.types.DeckWord

/**
 * The drill id space is open: the built-ins are "swipe" | "quiz" | "type",
 * a plugin may use any string. Ids are persisted — the DataStore settings
 * file ('study-mode' key) keeps the user's choice — so a plugin id must be
 * unique across the registry and stable across releases.
 */
typealias DrillId = String

/** Per-card render input, assembled by the mode screen. Cards hold no
 * session state of their own — the pager recycles them. Date-walk screens
 * (a day replay, a flag review) would pass entries enriched with their
 * bucket and round; single-bucket decks omit that. */
data class DrillCardInput(
    val word: DeckWord,
    /** null = not answered yet (or the drill has no answers). */
    val answered: AnswerState?,
    val onAnswer: (correct: Boolean, picked: String?) -> Unit,
    /** Reveal without recording an answer (typing's "show answer"). */
    val onSkip: () -> Unit,
    /** The card's draft text (typing input), lifted to the screen so it
     * survives page recycling. */
    val draft: String,
    val onDraft: (value: String) -> Unit,
    val onReplay: () -> Unit,
    val onToggleFlag: () -> Unit,
    /** Whether the meaning waits for a tap — the swipe card's setting, passed
     * through by the deck. Answer-driven cards (quiz, type) ignore it: their
     * reveal is the answer state. */
    val tapToReveal: Boolean,
)

/** The end page's facts, as the mode screen freezes them at round end. */
data class EndCopyFacts(
    val nextRound: Int,
    val total: Int,
    val correct: Int,
    val answered: Int,
)

/** End-page copy: title, then stats (when present), then note — one line
 * each, never a packed single line. */
data class EndCopy(
    val title: String,
    /** The round's tally line, e.g. "4 of 4 correct" — null when the drill
     * records no answers. */
    val stats: String? = null,
    /** The closing line, e.g. "Round 6 starts in a moment". */
    val note: String,
)

/**
 * A drill is "how a card is answered", not a separate progress: every drill
 * shares the probe, the round flags and the round-advance protocol, and the
 * mode it runs in (study/review/browse) decides what answering means for
 * the session. The abstraction lives at the screen layer — WordDeck stays a
 * dumb pager and only knows the renderCard slot.
 *
 * This interface is the drill plugin contract. Implement it in a
 * self-contained object in this package (one file per drill), and add it to
 * the registry's manifest — nothing else in the core changes.
 */
interface Drill {
    val id: DrillId

    /** Settings chip + deck title drill label, e.g. Study(Quiz) 2050. */
    val label: String

    /** Runs once per data load: rebuilds the drill's private per-card data
     * (e.g. shuffled options). All randomness lives here. */
    fun prepare(words: List<DeckWord>) {}

    val renderCard: @Composable (DrillCardInput) -> Unit

    /** The deck auto-speaks the current card on open/settle/scrub. For the
     * typing drill this IS the dictation cue (speech never leaks the
     * spelling); a future drill that must stay silent turns it off. */
    val speakOnSettle: Boolean

    /** Whether the progress bar may be scrubbed (scrubbing flips the deck
     * into browse mode, which records nothing). */
    val scrubbable: Boolean

    /** Whether user gestures scroll the deck freely. Answer-gated drills are
     * false: the deck cannot scroll past an unanswered card, so the only way
     * forward is answering. */
    val scrollable: Boolean

    /** After a CORRECT answer the deck auto-advances after this many ms. */
    val autoAdvanceMs: Long? get() = null

    /** A wrong answer auto-flags the word (the user can unflag freely). */
    val autoFlagOnWrong: Boolean get() = false

    /** End-page copy. nextRound is the round that starts after the linger. */
    fun endCopy(facts: EndCopyFacts): EndCopy
}
