package org.pareto.music.perform

import org.pareto.music.Decision
import org.pareto.music.Fn
import org.pareto.music.FunctionNamespace
import org.pareto.music.Grammar
import org.pareto.music.Harmony
import org.pareto.music.Melody
import org.pareto.music.Music
import org.pareto.music.Note
import org.pareto.music.Silence
import org.pareto.music.UnrunnableProcess


/**
 * Runs a program within a [namespace]
 */
interface Performer<T> {
    val namespace: FunctionNamespace<Fn.Definition>

    /**
     * Entrypoint to initiate program evaluation
     */
    fun play(pieceName: Fn.Name) = namespace[pieceName]?.music?.play() ?: throw UnrunnableProcess()

    /**
     * Convenience method for playing a function by name.
     */
    fun play(functionName: String) = play(Fn.Name(functionName))

    /**
     * Plays a piece of [Music]
     */
    fun Music.play(): T

    companion object {
        fun namespaceFromGrammar(grammar: Grammar): FunctionNamespace<Fn.Definition> =
            grammar.definitions.associateBy { it.name }
    }
}

/**
 * Decomposes the job of a [Performer] into 5 easier-to-implement methods:
 *
 * [rest] plays [Silence].
 *
 * [decide] decides between one of the two branches in a [Decision] by returning that branch.
 *
 * [playNote] plays [Note].
 *
 * [playMelody] plays [Melody].
 *
 * [playHarmony] plays [Harmony].
 */
interface PiecewisePerformer<T>: Performer<T> {

    /**
     * Plays [Silence].
     */
    fun rest(): T

    /**
     * Plays [Note].
     */
    fun playNote(note: Note): T

    /**
     * Plays [Melody].
     */
    fun playMelody(melody: Melody): T

    /**
     * Plays [Harmony].
     */
    fun playHarmony(harmony: Harmony): T

    /**
     * Decides a [Decision] by returning one of its branches.
     */
    fun decide(dilemma: Decider.Dilemma): Decider.Dilemma.Choice

    /**
     * Plays [Music] by delegating to the [Performer] helper methods.
     */
    override fun Music.play(): T = when(this) {
        is Silence -> rest()
        is Note -> playNote(this)
        is Melody -> playMelody(this)
        is Harmony -> playHarmony(this)
        is Decision -> decide(Decider.Dilemma(Will, Wont)).choice.play()
        is Fn.Call -> namespace[name]?.replacingArgsWith(params, namespace)?.play() ?: throw UnrunnableProcess("name does not exist in namespace")
        is Fn.Definition -> throw RuntimeException("should never get here")
    }
}

