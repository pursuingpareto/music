package org.pareto.music.play

import org.pareto.music.Decision
import org.pareto.music.Fn
import org.pareto.music.Grammar
import org.pareto.music.Harmony
import org.pareto.music.Melody
import org.pareto.music.Music
import org.pareto.music.Note
import org.pareto.music.canonical

class RandomChoiceMusician(
    grammar: Grammar,
    val onNote: (note: Note) -> Boolean =
        { note -> true.also { println(note.canonical()) } }
): PiecewisePlayer<Boolean> {

    fun play(functionName: String): Boolean = play(Fn.Name(functionName))

    override val namespace = grammar.definitions.associateBy { it.name }

    override fun rest(): Boolean = true

    override fun playNote(note: Note): Boolean = onNote(note)

    override fun playMelody(melody: Melody): Boolean = melody.Tick.play() && melody.Tock.play()

    override fun playHarmony(harmony: Harmony): Boolean {
        TODO("Not yet implemented")
    }

    override fun decide(decision: Decision): Music = listOf(decision.Will, decision.Wont).random()
}