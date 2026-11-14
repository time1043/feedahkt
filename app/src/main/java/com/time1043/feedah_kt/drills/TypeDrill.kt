package com.time1043.feedah_kt.drills

import androidx.compose.runtime.Composable
import com.time1043.feedah_kt.ui.components.drill.TypingCard

/**
 * The type drill: the TTS pronounces the word as a dictation cue and the
 * user types what they hear; the spelling stays hidden until they submit. A
 * misspelling is exactly what the flag list exists for: wrong submits join
 * the round's review queue, and the bookmark stays as the undo.
 */
object TypeDrill : Drill {
    override val id: DrillId = "type"
    override val label: String = "Type"

    override val renderCard: @Composable (DrillCardInput) -> Unit = { props ->
        TypingCard(
            word = props.word,
            answered = props.answered,
            draft = props.draft,
            onDraft = props.onDraft,
            onAnswer = { correct -> props.onAnswer(correct, null) },
            onReplay = props.onReplay,
            onToggleFlag = props.onToggleFlag,
        )
    }

    // The TTS pronounces the word as a dictation cue — the user hears it and
    // types what they hear. The spelling stays hidden until they submit.
    override val speakOnSettle: Boolean = true
    override val scrubbable: Boolean = true
    override val scrollable: Boolean = false
    // Correct submits slide on by themselves; wrong ones stay put so the
    // revealed answer gets read before the next swipe.
    override val autoAdvanceMs: Long = 900L
    override val autoFlagOnWrong: Boolean = true

    override fun endCopy(facts: EndCopyFacts): EndCopy = EndCopy(
        title = "Round complete",
        stats = "${facts.correct} of ${facts.answered} typed right",
        note = "Round ${facts.nextRound} starts in a moment",
    )
}
