package org.pareto.music.perform

import org.pareto.music.Grammar
import org.pareto.music.Harmony
import org.pareto.music.Melody
import org.pareto.music.Note

/**
 * A [StringBuilderPerformer] builds Strings from a [Grammar] and a [Decider].
 *
 * This [PiecewisePerformer]<[String]> interprets [Harmony] as direct String concatenation
 * and [Melody] as word combination (with a space in between the words).
 */
class StringBuilderPerformer(
    grammar: Grammar,
    private val decider: Decider
): PiecewisePerformer<String> {

    override val namespace = Performer.namespaceFromGrammar(grammar)

    override fun rest() = ""

    override fun playNote(note: Note) = "${note.name}"

    /**
     * Interprets [melody].Tick and [Melody].Tock as words and returns their string
     * representations separated by a space.
     */
    override fun playMelody(melody: Melody) =
        "${melody.Tick.play()} ${melody.Tock.play()}"

    /**
     * Interprets [harmony].Front and [harmony].Back as parts of a single string
     * and returns the concatenation of their string representations.
     */
    override fun playHarmony(harmony: Harmony) =
        "${harmony.Front.play()}${harmony.Back.play()}"

    override fun decide(dilemma: Decider.Dilemma) =
        decider.decide(dilemma, namespace)
}