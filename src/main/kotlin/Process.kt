package org.example.pg

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
 * * the lowercase terms (one, two, etc...) represent [Terminal] processes
 *
 * * the three unindented PascalCase words are the [Names][Process.Name] of [Defined] processes,
 * which each have a corresponding `Process` defined below the name.
 *
 * * The ` one | two | three | four | five | six ` is a chain of [Binary.Decision] processes.
 *
 * * The ` RollDie > RollDie ` in `RollTwice` is a [Binary.Sequence] where both steps are identical. The
 * steps are [References][Reference] to the `RollDie` process.
 *
 * * The ` RollTwoDice ` process is meant to represent *simultaneous* throwing of two dice. We use a
 * [Binary.Eclipse] process (`RollDie & RollDie`) to represent these concurrent processes.
 */
sealed interface Process {
  @JvmInline
  value class Name(private val name: String) {
    init { require(name.isPascalCase()) }
    override fun toString() = name
  }

  fun canonical(): String = when(this) {
    is Binary.Decision -> "${left.canonical()} | ${right.canonical()}"
    is Binary.Eclipse  -> "${foreground.canonical()} & ${background.canonical()}"
    is Binary.Sequence -> "${first.canonical()} > ${last.canonical()}"
    is Defined         -> "$name\n  : ${process.canonical()}"
    is Optional        -> "[ ${process.canonical()} ]"
    is Reference       -> referencedName.toString()
    is Terminal        -> obj
  }
}

/**
 * A terminal process has no child processes.
 *
 * @param obj A serial representation of this terminal "object".
 */
data class Terminal(val obj: String): Process

/**
 * A reference to a [Defined] process.
 *
 * @param referencedName the [Name][Process.Name] of the corresponding [Defined] process.
 */
data class Reference(val referencedName: Process.Name): Process

/**
 * A process with a [Name][Process.Name] which can be referred to by a [Reference]. Names
 * must be unique within a [Grammar].
 *
 * @param name the name of this process.
 * @param process the corresponding process.
 */
data class Defined(val name: Process.Name, val process: Process): Process

/**
 * ## `[ p ]`
 * An optional process may be evaluated or skipped.
 */
data class Optional(val process: Process): Process

/**
 * Containers for two processes (`a` and `b`). This class has 3 subclasses:
 *
 * [Binary.Sequence] represents a two-step sequence.
 *
 * [Binary.Decision] represents a two-choice branch.
 *
 * [Binary.Eclipse] represents two concurrent processes.
 */
@Suppress("unused")
sealed class Binary(
  private val a: Process,
  private val b: Process): Process {

  /**
   * ## `a > b`
   *
   * A sequence of two processes which occur one after the other without gaps.
   * The runtime of a `Sequence` is equal to the runtime of its components.
   *
   * @param first the first subprocess
   * @param last the second subprocess
   */
  data class Sequence(
    val first: Process,
    val last: Process): Binary(first, last)

  /**
   * ## `a | b`
   *
   * A fork in the road for process evaluation. There are two choices:
   *
   * @param left the first choice
   * @param right the second choice
   */
  data class Decision(
    val left: Process,
    val right: Process): Binary(left, right)

  /**
   * ## `a & b`
   *
   * An "eclipse" represents two concurrent (parallel) processes.
   *
   * No guarantees about which subprocess starts or ends first--like a
   * celestial eclipse, the overlap can be complete or partial.
   *
   * @param foreground the "first" of two concurrent subprocesses
   * @param background the "second" of two concurrent subprocesses
   */
  data class Eclipse(
    val foreground: Process,
    val background: Process): Binary(foreground, background)
}