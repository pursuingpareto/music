package org.example.pg

typealias Sequence = Dimension.Time
typealias Decision = Dimension.Choice
typealias Parallel = Dimension.Space

infix fun Process.then(that: Process) = Dimension.Time(this, that)

infix fun Process.and(that: Process) = Dimension.Space(this, that)

infix fun Process.or(that: Process) = Dimension.Choice(this, that)

/**
 * A container for various process types.  Each subtype can be represented
 * in a "canonical" form by calling [Process.canonical].
 *
 * Consider the following representation of a grammar describing dice rolls:
 *
 * ```
 * RollDie
 *   : one | two | three | four | five | six
 *
 * RollTwice
 *   : RollDie > RollDie
 *
 * RollTwoDice
 *   : RollDie & RollDie
 * ```
 *
 * In this grammar:
 * * the lowercase terms (one, two, etc...) represent [Expanding] processes
 *
 * * the three unindented PascalCase words are the [Names][Fn.Name] of [Fn.Definition] processes,
 * which each have a corresponding `Process` defined below the name.
 *
 * * The ` one | two | three | four | five | six ` is a chain of [Dimension.Choice] processes.
 *
 * * The ` RollDie > RollDie ` in `RollTwice` is a [Dimension.Time] where both steps are identical. The
 * steps are [References][Fn.Call] to the `RollDie` process.
 *
 * * The ` RollTwoDice ` process is meant to represent *simultaneous* throwing of two dice. We use a
 * [Dimension.Space] process (`RollDie & RollDie`) to represent these concurrent processes.
 */
sealed interface Process

/**
 * The [Empty] process contains nothing, so it is always skipped at runtime.
 *
 * This process can occasionally be useful! The standard library's [Possible][org.example.pg.stdlib.Lib.Possible]
 * process uses it!
 */
object Empty : Process

/**
 * An expanding process is terminal, so it has no children.
 *
 * @param obj A serial representation of this terminal "object".
 */
data class Expanding(val obj: Name) : Process {
    constructor(name: String) : this(Name(name))

    class Name(name: String) : ProcessName(name) {

        init {
            require(!name.isPascalCase())
            require(name.isNotEmpty())
        }
    }
}

sealed interface Fn : Process {

    val name: Name

    class Name(name: String) : ProcessName(name) {

        init { require(name.isPascalCase()) }
    }

    /**
     * A reference to a [Definition] process.
     *
     * @param name the [Fn.Name] of the corresponding [Definition] process.
     */
    data class Call(override val name: Name, val params: Params = listOf()) : Fn

    /**
     * A process with a [Fn.Name] which can be referred to by a [Call]. Names
     * must be unique within a [Grammar].
     *
     * @param name the name of this process.
     * @param process the corresponding process.
     */
    data class Definition(
        override val name: Name,
        val process: Process,
        val requiredArgs: List<RequiredArg> = listOf(),
    ) : Fn {

        init {
            require(process !is Empty && process !is Expanding)
        }
    }
}

/**
 * Containers for two processes (`a` and `b`). This class has 3 subclasses:
 *
 * [Dimension.Time] represents a two-step sequence.
 *
 * [Dimension.Choice] represents a two-choice branch.
 *
 * [Dimension.Space] represents two concurrent processes.
 */
@Suppress("unused", "PropertyName")
sealed class Dimension(
    private val left: Process,
    private val right: Process,
) : Process {

    /**
     * ## `a > b`
     *
     * A sequence of two processes which occur one after the other without gaps.
     * The runtime of a `Time` is equal to the runtime of its components.
     *
     * @param Tick the first subprocess
     * @param Tock the second subprocess
     */
    data class Time(
        val Tick: Process,
        val Tock: Process,
    ) : Dimension(Tick, Tock)

    /**
     * ## `a | b`
     *
     * A fork in the road for process evaluation. There are two choices:
     *
     * @param Will the first choice
     * @param Wont the second choice
     */
    data class Choice(
        val Will: Process,
        val Wont: Process,
    ) : Dimension(Will, Wont) {

        init {
            require(Will !is Empty)
        }
    }

    /**
     * ## `a & b`
     *
     * A `Dimension.Space` represents two concurrent (parallel) processes.
     *
     * No guarantees about which subprocess starts or ends first. Think of
     * these processes like a solar eclipse: the overlap can be complete or partial.
     *
     * @param Front the "first" of two concurrent subprocesses
     * @param Back the "second" of two concurrent subprocesses
     */
    data class Space(
        val Front: Process,
        val Back: Process,
    ) : Dimension(Front, Back) {

        init {
            require(Front !is Empty || Back !is Empty)
        }
    }
}

/**
 * Produces a canonical, language-agnostic string representation of this process.
 */
fun Process.canonical(): String = when (this) {
    is Dimension.Choice -> "${Will.canonical()} | ${Wont.canonical()}"
    is Dimension.Space -> "${Front.canonical()} & ${Back.canonical()}"
    is Dimension.Time -> "${Tick.canonical()} > ${Tock.canonical()}"
    is Fn.Definition -> "$name(${requiredArgs.joinToString()})\n  : ${process.canonical()}"
    is Empty -> "[ ]"
    is Fn.Call -> "$name(${params.joinToString { it.canonical() }})"
    is Expanding -> obj.toString()
}
