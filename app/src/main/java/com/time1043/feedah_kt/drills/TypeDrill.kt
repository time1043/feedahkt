package com.time1043.feedah_kt.drills

import androidx.compose.runtime.Composable
import com.time1043.feedah_kt.ui.components.WordCard
import com.time1043.feedah_kt.types.toWord

/**
 * The type drill, in placeholder shape: registered and switchable, but the
 * card is still the plain feed card and the deck scrolls freely. The
 * dictation card, the grading and the auto-flag/auto-advance policy land
 * with the drill's implementation.
 */
object TypeDrill : Drill {
    override val id: DrillId = "type"
    override val label: String = "Type"

    override val renderCard: @Composable (DrillCardInput) -> Unit = { props ->
        WordCard(
            word = props.word.toWord(),
            tapToReveal = props.tapToReveal,
            flagged = props.word.flagged,
            onReplay = props.onReplay,
            onToggleFlag = props.onToggleFlag,
        )
    }

    override val speakOnSettle: Boolean = true
    override val scrubbable: Boolean = true

    // Freely scrollable until the answer gate exists — a gate with nothing
    // to answer would dead-end the deck.
    override val scrollable: Boolean = true

    override fun endCopy(facts: EndCopyFacts): EndCopy = EndCopy(
        title = "Round complete",
        note = "Round ${facts.nextRound} starts in a moment",
    )
}
