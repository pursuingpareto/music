package org.pareto.music.compiler.thread_validator

import org.pareto.music.AmbiguousBranching
import org.pareto.music.compiler.ArgMap
import org.pareto.music.Fn
import org.pareto.music.compiler.Globals
import org.pareto.music.Grammar
import org.pareto.music.Keyword
import org.pareto.music.compiler.Locals
import org.pareto.music.Music
import org.pareto.music.NoMatchForInput
import org.pareto.music.Note
import org.pareto.music.ProcessException
import org.pareto.music.RequiredArg
import org.pareto.music.UnrunnableProcess
import org.pareto.music.Text
import org.pareto.music.compiler.PiecewiseCompiler

typealias OnWord = (Text) -> Any?

/**
 * A [Music] can be compiled to an [OnWord] function in a bare [Context] if it requires no
 * "extra" information to run.
 *
 * @sample PiecewiseCompiler
 */
interface Context: PiecewiseCompiler<OnWord> {

    override val empty: OnWord
        get() = { input -> if (input.value == Keyword.END) true else null }


    override fun note(name: Note.Name): OnWord = { input ->
        if (name.value == input.value) {
            true
        } else {
            throw NoMatchForInput("$input/$name")
        }
    }

    // TODO - cleanup messy nested branches!
    override fun decision(will: OnWord, wont: OnWord): OnWord = { input: Text ->
        // apply the functions for each branch to the input, mapping exceptions to `false`
        val attempts = listOf(will, wont).map { f ->
            try { f(input) }
            catch (e: ProcessException) { false }
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
                        if (attempts.first() == null) attempts.last() else {
                            attempts.first()
                        }
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
    override fun melody(tick: OnWord, tock: OnWord): OnWord = { word: Text ->
        when (val xw = tick(word)) {
            null -> tock(word)
            true -> { w: Text -> tock(w) }
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
) : Context, PiecewiseCompiler<OnWord> {

    private val functionArgs: ArgMap =
        grammar.definitions.associate { it.name to it.requiredArgs }

    val functionNamespace: Globals<OnWord> =
        grammar.definitions.associate { it.name to { compile(it.music) } }

    /**
     * Compiles a [Fn.Call] process into an [OnWord] function.
     */
    override fun call(name: Fn.Name, replacements: Locals<OnWord>): OnWord {
        return FunctionContext(name, replacements).call()
    }

    override fun call(call: Fn.Call): OnWord {
        val args = call.name.materializeWith(call)
        return FunctionContext(call.name, args).call()
    }

    override fun define(name: Fn.Name, args: List<Note.Name>, music: OnWord): OnWord {
        throw RuntimeException("not how you compile definitions here")
    }

    private fun Fn.Name.materializeWith(call: Fn.Call): Locals<OnWord> {
        return functionArgs[this]
            ?.zip(call.params.map { { compile(it) } })?.toMap()
            ?: throw UnrunnableProcess("could not materialize args for ${call.name}")
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
        val name: Fn.Name,
        private val locals: Locals<OnWord>,
    ) : GrammarContext(grammar), PiecewiseCompiler<OnWord> {

        /**
         * Compiles a [Note] process into an [OnWord] function and--if this [Note]
         * is a param in a function--makes the appropriate replacement.
         */
        override fun note(name: Note.Name): OnWord {
            val f = locals[name]
            return if (f == null) { super<GrammarContext>.note(name) } else { input: Text -> f()(input) }
        }

        /**
         * Compiles the [Fn.Call] process associated with THIS context to an [OnWord] function.
         */
        fun call(): OnWord {
            val f = functionNamespace[name] ?: throw UnrunnableProcess()
            return { input -> f()(input) }
        }
    }
}
