package org.pareto.music

typealias Melody   = Dimension.Time
typealias Decision = Dimension.Choice
typealias Harmony  = Dimension.Space

infix fun Music?.then(that: Music?) = Melody(this, that)
infix fun Music?.and (that: Music?) = Harmony(that, this)
infix fun Music?.or  (that: Music?) = Decision(this, that)

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

val Silence = null

sealed interface Expandable : Music {
    val name: MusicName
}

/**
 * A [Note] is terminal, so it has no children.
 *
 * @param name A serial representation of this terminal "object".
 */
data class Note(override val name: Name) : Expandable {
    constructor(s: String) : this(Name(s))
    class Name(text: Text.nonPascalCase) : MusicName(text) {
        companion object {
            operator fun invoke(s: String): Name =
                Name(Text.nonPascalCase(s))
        }
    }
}

sealed interface NonTerminal: Music

sealed interface Fn : NonTerminal {

    fun call(params: List<Music?>, namespace: FunctionNamespace<Definition>): Music =
        namespace[name]?.replacingArgsWith(params, namespace) ?: throw UnrunnableProcess("No function with name $name in namespace")

    val name: Name

    class Name(text: Text.PascalCase) : MusicName(text) {
        companion object {
            operator fun invoke(s: String): Name =
                Name(Text.PascalCase(s))
        }
    }

    /**
     * A reference to a [Definition] process.
     *
     * @param name the [Fn.Name] of the corresponding [Definition] process.
     */
    data class Call(override val name: Name, val params: List<Music?> = listOf()) : Fn, Expandable

    /**
     * A process with a [Fn.Name] which can be referred to by a [Call]. Names
     * must be unique within a [Grammar].
     *
     * @param name the name of this process.
     * @param music the corresponding process.
     */
    data class Definition(
        override val name: Name,
        val music: NonTerminal,
        val requiredArgs: List<Note.Name> = listOf(),
    ) : Fn {

        init { Validate.argsAreUsedInBody(this) }

        fun replacingArgsWith(params: List<Music?>, namespace: FunctionNamespace<Definition>): Music? {
            val map = requiredArgs.zip(params).associate { it.first to it.second }.toMap()
            return music.rebuildWithReplacements(map, namespace)
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
    val Left: Music?,
    val Right: Music?,
) : NonTerminal, Music {

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
        val Tick: Music?,
        val Tock: Music?,
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
        val Will: Music?,
        val Wont: Music?,
    ) : Dimension(Will, Wont)

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
        val Front: Music?,
        val Back: Music?,
    ) : Dimension(Front, Back)
}

/**
 * Produces a canonical, language-agnostic string representation of this process.
 */
fun Music?.canonical(): String = when (this) {
    null ->          "[ ]"
    is Dimension.Choice -> when(Wont) {
               // this indicates "Optional" Music
               null ->  "[ ${Will.canonical()} ]"
                  else ->   "${Will.canonical()} | ${Wont.canonical()}" }
    is Dimension.Space ->  "${Front.canonical()} & ${Back.canonical()}"
    is Dimension.Time ->    "${Tick.canonical()} > ${Tock.canonical()}"
    is Fn.Definition -> { if (requiredArgs.isNotEmpty())
        "$name(${requiredArgs.joinToString()})\n  : ${music.canonical()}"
        else "$name\n  : ${music.canonical()}"
    }

    is Fn.Call -> { if (params.isNotEmpty())
        "$name(${params.joinToString { it.canonical() }})"
        else "$name" }
    is Note ->             name.toString()
}

fun Music?.rebuildWithReplacements(replacements: Map<Note.Name, Music?>, namespace: FunctionNamespace<Fn.Definition>): Music? {
    return when(this) {
        null -> null
        is Note -> replacements[this.name] ?: this
        is Fn.Call -> namespace[name]?.replacingArgsWith(params, namespace)?.rebuildWithReplacements(replacements, namespace) ?: throw UnrunnableProcess("name $name does not exist in namespace")
        is Decision -> Decision(Will.rebuildWithReplacements(replacements, namespace), Wont.rebuildWithReplacements(replacements, namespace))
        is Harmony -> Harmony(Front.rebuildWithReplacements(replacements, namespace), Back.rebuildWithReplacements(replacements, namespace))
        is Melody -> Melody(Tick.rebuildWithReplacements(replacements, namespace), Tock.rebuildWithReplacements(replacements, namespace))
        is Fn.Definition -> TODO()
    }
}
