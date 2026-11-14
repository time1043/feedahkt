package com.time1043.feedah_kt.drills

import androidx.compose.runtime.Composable
import com.time1043.feedah_kt.ui.components.WordCard
import com.time1043.feedah_kt.types.toWord

/**
 * The swipe drill: no answering at all — the card IS the feed card, the
 * deck scrolls freely and nothing is recorded. This is also the registry's
 * fallback (getDrill on an unknown id).
 */
object SwipeDrill : Drill {
    override val id: DrillId = "swipe"
    override val label: String = "Swipe"

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
    override val scrollable: Boolean = true

    override fun endCopy(facts: EndCopyFacts): EndCopy = EndCopy(
        title = "Round complete",
        note = "Round ${facts.nextRound} starts in a moment",
    )
}
