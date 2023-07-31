package org.pareto.music

typealias Melody   = Dimension.Time
typealias Decision = Dimension.Choice
typealias Harmony  = Dimension.Space

infix fun Music.then(that: Music) = Melody(this, that)
infix fun Sound.and (that: Music) = Harmony(that, this)
infix fun Sound.or  (that: Music) = Decision(this, that)

/**
 * A container for various process types.  Each subtype can be represented
 * in a "canonical" form by calling [Music.canonical].
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
 * * the lowercase terms (one, two, etc...) represent [Note] processes
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
sealed interface Music

/**
 * The [Silence] object contains nothing, so it is always skipped at runtime.
 *
 * This process can occasionally be useful! The standard library's [Possible][StdLib.Possible]
 * process uses it!
 */
object Silence : Music

/**
 * Every instance of [Music] that isn't [Silence] is [Sound]
 */
sealed interface Sound : Music

sealed interface Expandable : Music {
    val name: MusicName
}

/**
 * A [Note] is terminal, so it has no children.
 *
 * @param name A serial representation of this terminal "object".
 */
data class Note(override val name: Name) : Sound, Expandable {
    constructor(s: String) : this(Name(s))
    class Name(text: Text.nonPascalCase) : MusicName(text) {
        companion object {
            operator fun invoke(s: String): Name =
                Name(Text.nonPascalCase(s))
        }
    }
}


/**
 * Includes all [Sound] except for [Silence] and [Note].
 */
sealed interface NonTerminal : Sound


/**
 * Containers for two processes (`a` and `b`). This class has 3 subclasses:
 *
 * [Dimension.Time] represents a two-step sequence.
 *
 * [Dimension.Choice] represents a two-choice branch.
 *
 * [Dimension.Space] represents two concurrent processes.
 */
@Suppress( "PropertyName", "LocalVariableName")
sealed class Dimension(val Left: Music, val Right: Music) : NonTerminal {

    /**
     * ## `a > b`
     *
     * A sequence of two processes which occur one after the other without gaps.
     * The runtime of a `Time` is equal to the runtime of its components.
     *
     * @param Tick the first subprocess
     * @param Tock the second subprocess
     */
    class Time(Tick: Music, Tock: Music) : Dimension(Tick, Tock) {
        val Tick get() = Left
        val Tock get() = Right
    }

    /**
     * ## `a | b`
     *
     * A fork in the road for process evaluation. There are two choices:
     *
     * @param Will the first choice
     * @param Wont the second choice
     */
    class Choice(Will: Sound, Wont: Music) : Dimension(Will, Wont) {
        val Will get() = Left as Sound
        val Wont get() = Right
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
    class Space(Front: Music, Back: Sound) : Dimension(Front, Back) {
        val Front get() = Left
        val Back get() = Right as Sound
    }
}

/**
 * Produces a canonical, language-agnostic string representation of this process.
 */
fun Music.canonical(): String = when (this) {
    is Dimension.Choice -> when(Wont) {
               // this indicates "Optional" Music
               Silence ->  "[ ${Will.canonical()} ]"
                  else ->   "${Will.canonical()} | ${Wont.canonical()}" }
    is Dimension.Space ->  "${Front.canonical()} & ${Back.canonical()}"
    is Dimension.Time ->    "${Tick.canonical()} > ${Tock.canonical()}"
    is Silence ->          "[ ]"
    is Fn.Call -> { if (params.isNotEmpty())
        "$name(${params.joinToString { it.canonical() }})"
        else "$name" }
    is Note ->             name.toString()
}
