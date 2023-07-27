package org.pareto.music.compiler.thread_validator

import org.pareto.music.AmbiguousBranching
import org.pareto.music.compiler.ArgMap
import org.pareto.music.Fn
import org.pareto.music.compiler.FunctionCompiler
import org.pareto.music.compiler.Globals
import org.pareto.music.Grammar
import org.pareto.music.compiler.GrammarCompiler
import org.pareto.music.compiler.Compiler
import org.pareto.music.Keyword
import org.pareto.music.compiler.Locals
import org.pareto.music.Music
import org.pareto.music.NoMatchForInput
import org.pareto.music.Note
import org.pareto.music.ProcessException
import org.pareto.music.UnrunnableProcess
import org.pareto.music.Word

typealias OnWord = (Word) -> Any?

/**
 * A [Music] can be compiled to an [OnWord] function in a bare [Context] if it requires no
 * "extra" information to run.
 *
 * @sample Compiler
 */
interface Context: Compiler<OnWord> {

    override val empty: OnWord
        get() = { input -> if (input == Keyword.END) true else null }


    override fun note(name: Note.Name): OnWord = { input ->
        if ("$name" == input) {
            true
        } else {
            throw NoMatchForInput("$input/$name")
        }
    }

    // TODO - cleanup messy nested branches!
    override fun decision(will: OnWord, wont: OnWord): OnWord = { input: Word ->
        // apply the functions for each branch to the input, mapping exceptions to `false`
        val attempts = listOf(will, wont).map { f ->
            try {
                f(input)
            } catch (e: ProcessException) {
                false
            }
        }

        // Every branch failed to match the input
        if (attempts.all { it == false }) throw NoMatchForInput(input)

        // No branch failed to match the input
        if (attempts.none { it == false }) {
            if (attempts.any { it == null }) {
                if (attempts.any { it == true }) {
                    true
                } else {
                    if (attempts.any { it is Function1<*, *> }) {
                        if (attempts.first() == null) attempts.last() else { attempts.first() }
                    } else {
                        throw AmbiguousBranching()
                    }
                }
            } else {
                throw AmbiguousBranching("attempts were ${attempts.first()} and ${attempts.last()}")
            }
        } else if (attempts.first() == false) {
            attempts.last()
        } else {
            attempts.first()
        }
    }

    // TODO - implement as `tailrec`
    @Suppress("UNCHECKED_CAST")
    override fun melody(tick: OnWord, tock: OnWord): OnWord = { word: Word ->
        when (val xw = tick(word)) {
            null -> tock(word)
            true -> { w: Word -> tock(w) }
            is Function1<*, *> -> melody(xw as OnWord, tock)
            else -> throw UnrunnableProcess("Failed to cast $xw to OnWord")
        }
    }

    override fun harmony(front: OnWord, back: OnWord): OnWord {
        TODO("Not yet implemented")
    }
}

/**
 * [Music]es compiled in a [GrammarContext] may rely on the whole [Grammar],
 * since the grammar provides a [functionNamespace] which allows us to resolve
 * [Fn.Call] processes.
 */
open class GrammarContext(
    protected val grammar: Grammar,
) : Context, GrammarCompiler<OnWord> {

    override val functionArgs: ArgMap =
        grammar.definitions.associate { it.name to it.requiredArgs.map { arg -> Note.Name(arg) } }

    val functionNamespace: Globals<OnWord> =
        grammar.definitions.associate { it.name to { it.music.compile() } }

    /**
     * Compiles a [Fn.Call] process into an [OnWord] function.
     */
    override fun call(name: Fn.Name, replacements: Locals<OnWord>): OnWord {
        return FunctionContext(name, replacements).call()
    }


    /**
     * [Music]es compiled in a [FunctionContext] may also rely on a collections of [locals] which
     * are used to replace [Note] processes where appropriate. Consider the following example:
     *
     * ```kotlin
     * val RepeatTwice = "RepeatTwice"
     * val x = "x"
     *
     * ...
     *
     * RepeatTwice(x) {
     *   x > x
     * }
     *
     * CoinFlip {
     *   heads | tails
     * }
     *
     * FlipTwice {
     *   RepeatTwice(CoinFlip)
     * }
     * ```
     *
     * In order to actually evaluate `RepeatTwice(CoinFlip)`, we need:
     *   1. a [GrammarContext] to map the [Fn.Name]s "CoinFlip" and "RepeatTwice" to their [Fn.Definition].
     *   2. a [FunctionContext] while referencing "RepeatTwice" so we can "remember" that we passed "CoinFlip" as a replacement for "x"
     */
    inner class FunctionContext(
        override val name: Fn.Name,
        override val locals: Locals<OnWord>,
    ) : GrammarContext(grammar), FunctionCompiler<OnWord> {

        /**
         * Compiles a [Note] process into an [OnWord] function and--if this [Note]
         * is a param in a function--makes the appropriate replacement.
         */
        override fun note(name: Note.Name): OnWord {
            val f = locals[name]
            return if (f == null) { super.note(name) } else { input: Word -> f()(input) }
        }

        /**
         * Compiles the [Fn.Call] process associated with THIS context to an [OnWord] function.
         */
        override fun call(): OnWord {
            val f = functionNamespace[name] ?: throw UnrunnableProcess()
            return { input -> f()(input) }
        }
    }
}
