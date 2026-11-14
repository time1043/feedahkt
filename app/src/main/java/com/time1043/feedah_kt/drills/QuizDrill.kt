package com.time1043.feedah_kt.drills

import androidx.compose.runtime.Composable
import com.time1043.feedah_kt.logic.QuizOption
import com.time1043.feedah_kt.logic.buildOptions
import com.time1043.feedah_kt.logic.pickDistractors
import com.time1043.feedah_kt.types.DeckWord
import com.time1043.feedah_kt.types.toWord
import com.time1043.feedah_kt.ui.components.drill.ChoiceCard

/**
 * The quiz drill: the English word on top, four Chinese options below —
 * curated distractors topped up from the bucket pool. Answering gates the
 * deck: a wrong pick auto-flags the word and stays put, a correct one slides
 * on by itself.
 */
object QuizDrill : Drill {
    override val id: DrillId = "quiz"
    override val label: String = "Quiz"

    // The quiz drill's per-card options, rebuilt by prepare() on every data
    // load and read by renderCard. Keyed by word position (AN decks are
    // single-bucket; the source keys bucket:position for its date walks).
    // Module-level on purpose: the data must stay stable across pager page
    // recycling (and a process restart simply rebuilds it).
    private var options: Map<Int, List<QuizOption>> = emptyMap()

    // Options are computed once per load: curated distractors topped up from
    // the bucket, shuffled into display order. Fixed for the whole session —
    // recycled cards must not reshuffle in front of the user.
    override fun prepare(words: List<DeckWord>) {
        // The option math speaks in plain words; the deck's flagged copies
        // convert.
        val plain = words.map { it.toWord() }
        options = plain.associate { word ->
            word.position to buildOptions(word, pickDistractors(word, word.distractors, plain))
        }
    }

    override val renderCard: @Composable (DrillCardInput) -> Unit = { props ->
        ChoiceCard(
            word = props.word,
            options = options[props.word.position] ?: emptyList(),
            answered = props.answered,
            onAnswer = props.onAnswer,
            onReplay = props.onReplay,
            onToggleFlag = props.onToggleFlag,
        )
    }

    // Speaking the English word is safe here — the options are Chinese.
    override val speakOnSettle: Boolean = true
    override val scrubbable: Boolean = true
    override val scrollable: Boolean = false
    override val autoAdvanceMs: Long = 650L
    override val autoFlagOnWrong: Boolean = true

    override fun endCopy(facts: EndCopyFacts): EndCopy = EndCopy(
        title = "Round complete",
        stats = "${facts.correct} of ${facts.answered} correct",
        note = "Round ${facts.nextRound} starts in a moment",
    )
}
