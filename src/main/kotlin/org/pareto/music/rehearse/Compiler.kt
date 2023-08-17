package org.pareto.music.rehearse

import org.pareto.music.Dimension
import org.pareto.music.Fn
import org.pareto.music.Music
import org.pareto.music.Decision
import org.pareto.music.Melody
import org.pareto.music.Harmony
import org.pareto.music.Note
import org.pareto.music.Silence


typealias Deferred<T> = () -> T

typealias Globals<T> = Map<Fn.Name, Deferred<T>>

typealias Locals<T> = Map<Note.Name, Deferred<T>>

typealias ArgMap = Map<Fn.Name, List<Note.Name>>

/**
 * The goal of compilation is to turn a "dumb" [Music] into a function that we can actually call.
 *
 * The signature of these compiled functions is [T].
 */
interface PiecewiseCompiler<T> : org.pareto.music.Compiler<T> {

    /**
     * [Silence] compiled to [T].
     */
    val empty: T

    /**
     * Compiles an [Note] to [T].
     */
    fun note(name: Note.Name): T

    fun note(note: Note): T = note(note.name)

    /**
     * Compiles a [Decision] to [T].
     */
    fun decision(will: T, wont: T): T

    fun decision(will: Music?, wont: Music?): T = decision(compile(will), compile(wont))

    /**
     * Compiles a [Melody] to [T].
     */
    fun melody(tick: T, tock: T): T

    fun melody(tick: Music?, tock: Music?): T = melody(compile(tick), compile(tock))

    /**
     * Compiles a [Harmony] to [T].
     */
    fun harmony(front: T, back: T): T

    fun harmony(front: Music?, back: Music?): T = harmony(compile(front), compile(back))

    /**
     * Compiles a [Fn.Call] to [T].
     */
    fun call(name: Fn.Name, replacements: Locals<T>): T

    fun call(call: Fn.Call): T = throw NotImplementedError()

    /**
     * Compiles a [Fn.Definition] to [T].
     */
    fun define(name: Fn.Name, args: List<Note.Name>, music: T): T

    fun define(def: Fn.Definition) = define(def.name, def.requiredArgs, compile(def.music))


    override fun compile(music: Music?): T = with(music) {
        when (this) {
            is Note -> note(this)
            null -> empty
            is Dimension.Choice -> decision(Will, Wont)
            is Dimension.Space -> harmony(Front, Back)
            is Dimension.Time -> melody(Tick, Tock)
            is Fn.Call -> call(this)
            is Fn.Definition -> define(this)
        }
    }
}