package org.example.pg

typealias OnWord = (Word) -> Any?

typealias Deferred<T> = () -> T

typealias Globals = Map<Fn.Name, Deferred<OnWord>>

typealias Locals = Map<Expanding.Name, Deferred<OnWord>>

typealias ArgMap = Map<Fn.Name, List<Expanding.Name>>

/**
 * The goal of compilation is to turn a "dumb" [Process] into a function that we can actually call.
 *
 * The signature of these compiled functions is called [OnWord].
 *
 * A [Process] can be compiled to an [OnWord] function in a bare [Context] if it requires no
 * "extra" information to run.
 */
interface Context {

    /**
     * Compiles a [Process.Empty] process into an [OnWord] function.
     */
    fun empty(): OnWord = { null }

    /**
     * Compiles an [Expanding] process into an [OnWord] function.
     */
    fun expanding(name: Expanding.Name): OnWord = { input ->
            if ("$name" == input) {
                true
            } else {
                throw NoMatchForInput("$input/$name")
            }
    }

    /**
     * Compiles a [Decision] process into an [OnWord] function.
     */
    fun decision(a: OnWord, b: OnWord): OnWord = { input: Word ->
        // apply the functions for each branch to the input, mapping exceptions to `false`
        val attempts = listOf(a, b).map { f ->
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
            if (attempts.any { it == true } && attempts.any { it == null }) {
                true
            } else {
                throw AmbiguousBranching()
            }
        } else if (attempts.first() == false) {
            attempts.last()
        } else {
            attempts.first()
        }
    }

    /**
     * Compiles a [Sequence] process into an [OnWord] function.
     */
    @Suppress("UNCHECKED_CAST")
    fun sequence(x: OnWord, y: OnWord): OnWord = { word: Word ->
        when (val xw = x(word)) {
            null -> y(word)
            true -> { w: Word -> y(w) }
            else -> sequence(xw as OnWord, y)
        }
    }
}

/**
 * [Process]es compiled in a [GrammarContext] may rely on the whole [Grammar],
 * since the grammar provides a [functionNamespace] which allows us to resolve
 * [Fn.Call] processes.
 */
open class GrammarContext(
    protected val grammar: Grammar,
) : Context {

    private val functionArgs: ArgMap =
        grammar.definitions.associate { it.name to it.requiredArgs.map { arg -> Expanding.Name(arg) } }

    val functionNamespace: Globals =
        grammar.definitions.associate { it.name to { it.process.compile() } }

    private fun Process.compile(): OnWord = when (this) {
        is Expanding -> expanding(obj)
        is Process.Empty -> empty()
        is Dimension.Choice -> decision(Will.compile(), Wont.compile())
        is Dimension.Space -> throw NotImplementedError("Parallel processes not yet supported")
        is Dimension.Time -> sequence(Tick.compile(), Tock.compile())
        is Fn.Call -> call(name, name.materializeWith(this))
        is Fn.Definition -> throw Error("Definitions are not directly compiled")
    }

    /**
     * Compiles a [Fn.Call] process into an [OnWord] function.
     */
    private fun call(name: Fn.Name, replacements: Locals): OnWord {
        return FunctionContext(name, replacements).call()
    }

    private fun Fn.Name.materializeWith(call: Fn.Call): Locals {
        return functionArgs[this]?.zip(call.params.map { { it.compile() } })?.toMap()
            ?: throw UnrunnableProcess("could not materialize args for ${call.name}")
    }

    /**
     * [Process]es compiled in a [FunctionContext] may also rely on a collections of [locals] which
     * are used to replace [Expanding] processes where appropriate. Consider the following example:
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
        private val name: Fn.Name,
        private val locals: Locals,
    ) : GrammarContext(grammar) {

        /**
         * Compiles a [Expanding] process into an [OnWord] function and--if this [Expanding]
         * is a param in a function--makes the appropriate replacement.
         */
        override fun expanding(name: Expanding.Name): OnWord {
            val f = locals[name]
            return if (f == null) { super.expanding(name) } else { input: Word -> f()(input) }
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
