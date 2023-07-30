package org.pareto.music.play

import org.pareto.music.Decision
import org.pareto.music.Fn
import org.pareto.music.FunctionNamespace
import org.pareto.music.Harmony
import org.pareto.music.Melody
import org.pareto.music.Music
import org.pareto.music.Note
import org.pareto.music.Silence
import org.pareto.music.UnrunnableProcess

/**
 * Runs a program within a [namespace]
 */
interface Player<T> {
    val namespace: FunctionNamespace<Fn.Definition>

    /**
     * Entrypoint to initiate program evaluation
     */
    fun play(pieceName: Fn.Name) = namespace[pieceName]?.music?.play() ?: throw UnrunnableProcess()

    /**
     * Plays a piece of [Music]
     */
    fun Music.play(): T
}

/**
 * Decomposes the job of a [Player] into 5 easier-to-implement methods:
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
interface PiecewisePlayer<T>: Player<T> {

    fun rest(): T

    fun playNote(note: Note): T

    fun playMelody(melody: Melody): T

    fun playHarmony(harmony: Harmony): T

    /**
     * Decides a [Decision] by returning one of its branches.
     */
    fun decide(decision: Decision): Music

    override fun Music.play(): T = when(this) {
        is Silence -> rest()
        is Note -> playNote(this)
        is Melody -> playMelody(this)
        is Harmony -> playHarmony(this)
        is Decision -> decide(this).play()
        is Fn.Call -> namespace[name]?.replacingArgsWith(params, namespace)?.play() ?: throw UnrunnableProcess("name does not exist in namespace")
        is Fn.Definition -> throw RuntimeException("should never get here")
    }
}

