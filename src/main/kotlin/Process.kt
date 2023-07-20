package org.example.pg

typealias Sequence = Dimension.Time
typealias Decision = Dimension.Choice
typealias Parallel = Dimension.Space

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
 * * the three unindented PascalCase words are the [Names][Name.Defined] of [Defined] processes,
 * which each have a corresponding `Process` defined below the name.
 *
 * * The ` one | two | three | four | five | six ` is a chain of [Dimension.Choice] processes.
 *
 * * The ` RollDie > RollDie ` in `RollTwice` is a [Dimension.Time] where both steps are identical. The
 * steps are [References][Reference] to the `RollDie` process.
 *
 * * The ` RollTwoDice ` process is meant to represent *simultaneous* throwing of two dice. We use a
 * [Dimension.Space] process (`RollDie & RollDie`) to represent these concurrent processes.
 */
sealed interface Process {



  /**
   * Produces a canonical, language-agnostic string representation of this process.
   */
  fun canonical(): String = when(this) {
    is Dimension.Choice -> "${left.canonical()} | ${right.canonical()}"
    is Dimension.Space  -> "${back.canonical()} & ${fore.canonical()}"
    is Dimension.Time   -> "${tick.canonical()} > ${tock.canonical()}"
    is Defined          -> "$name\n  : ${process.canonical()}"
    is Optional         -> "[ ${process.canonical()} ]"
    is Reference        -> referencedName.toString()
    is Expanding        -> obj.toString()
  }
}


/**
 * An expanding process is terminal, so it has no children.
 *
 * @param obj A serial representation of this terminal "object".
 */
data class Expanding(val obj: Name.Expanding): Process {
  constructor(name: String): this(Name.Expanding(name))
}


/**
 * A reference to a [Defined] process.
 *
 * @param referencedName the [Name.Defined] of the corresponding [Defined] process.
 */
data class Reference(val referencedName: Name.Defined): Process


/**
 * A process with a [Name.Defined] which can be referred to by a [Reference]. Names
 * must be unique within a [Grammar].
 *
 * @param name the name of this process.
 * @param process the corresponding process.
 */
data class Defined(val name: Name.Defined, val process: Process): Process {
  init { require(process !is Optional && process !is Expanding) } }


/**
 * ## `[ p ]`
 * An optional process may be evaluated or skipped.
 */
data class Optional(val process: Process): Process {
  init { require(process !is Optional) }}


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
  private val a: Process,
  private val b: Process): Process {


  /**
   * ## `a > b`
   *
   * A sequence of two processes which occur one after the other without gaps.
   * The runtime of a `Time` is equal to the runtime of its components.
   *
   * @param tick the first subprocess
   * @param tock the second subprocess
   */
  data class Time(
    val tick: Process,
    val tock: Process): Dimension(tick, tock)


  /**
   * ## `a | b`
   *
   * A fork in the road for process evaluation. There are two choices:
   *
   * @param left the first choice
   * @param right the second choice
   */
  data class Choice(
    val left: Process,
    val right: Process): Dimension(left, right) {
      init { require(left !is Optional && right !is Optional) } }


  /**
   * ## `a & b`
   *
   * A `Dimension.Space` represents two concurrent (parallel) processes.
   *
   * No guarantees about which subprocess starts or ends first. Think of
   * these processes like a solar eclipse: the overlap can be complete or partial.
   *
   * @param back the "first" of two concurrent subprocesses
   * @param fore the "second" of two concurrent subprocesses
   */
  data class Space(
    val back: Process,
    val fore: Process): Dimension(back, fore) {
    init { require(back !is Optional || fore !is Optional) } }
}