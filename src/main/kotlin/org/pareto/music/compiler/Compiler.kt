package org.pareto.music.compiler

import org.pareto.music.Dimension
import org.pareto.music.Fn
import org.pareto.music.Music
import org.pareto.music.Note
import org.pareto.music.Silence
import org.pareto.music.UnrunnableProcess

typealias Deferred<T> = () -> T

typealias Globals<T> = Map<Fn.Name, Deferred<T>>

typealias Locals<T> = Map<Note.Name, Deferred<T>>

typealias ArgMap = Map<Fn.Name, List<Note.Name>>

/**
 * The goal of compilation is to turn a "dumb" [Music] into a function that we can actually call.
 *
 * The signature of these compiled functions is [T].
 */
interface Compiler<T> {

    /**
     * [Silence] compiled to [T].
     */
    val empty: T

    /**
     * Compiles an [Note] to [T].
     */
    fun note(name: Note.Name): T

    /**
     * Compiles a [Decision] to [T].
     */
    fun decision(will: T, wont: T): T

    /**
     * Compiles a [Melody] to [T].
     */
    fun melody(tick: T, tock: T): T

    /**
     * Compiles a [Harmony] to [T].
     */
    fun harmony(front: T, back: T): T
}


interface GrammarCompiler<T>: Compiler<T> {
    val functionArgs: ArgMap

    fun call(name: Fn.Name, replacements: Locals<T>): T

    private fun Fn.Name.materializeWith(call: Fn.Call): Locals<T> {
        return functionArgs[this]
            ?.zip(call.params.map { { it.compile() } })?.toMap()
            ?: throw UnrunnableProcess("could not materialize args for ${call.name}")
    }

    fun Music.compile(): T = when (this) {
        is Note -> note(obj)
        is Silence -> empty
        is Dimension.Choice -> decision(Will.compile(), Wont.compile())
        is Dimension.Space -> harmony(Front.compile(), Back.compile())
        is Dimension.Time -> melody(Tick.compile(), Tock.compile())
        is Fn.Call -> call(name, name.materializeWith(this))
        is Fn.Definition -> throw Error("Definitions are not directly compiled")
    }
}

interface FunctionCompiler<T>: GrammarCompiler<T> {
    val name: Fn.Name
    val locals: Locals<T>

    fun call(): T
}