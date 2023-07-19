package org.example.pg

/**
 * A collection of [Defined] processes which together define a language of possible programs.
 */
class Grammar(val processes: List<Defined>) {

  init { validateGrammar(processes) }

  /**
   * Creates a new grammar from two existing grammars.
   */
  infix fun extend(other: Grammar) = Grammar(this.processes + other.processes)

  /**
   * Produces a canonical, language-agnostic string representation of this grammar.
   */
  fun canonical() = processes.joinToString(separator = "\n\n") { it.canonical() }

  override fun toString() = canonical()

  companion object {

    fun validateGrammar(components: List<Defined>) {
      validateUniqueNames(components)
    }

    private fun validateUniqueNames(components: List<Defined>) {
      fun message(name: Name, count: Int) = "$name defined $count times"
      val errors = components
        .map { it.name }
        .groupingBy { it }
        .eachCount()
        .filter { (_, count) -> count > 1 }
        .map { (name, count) -> message(name, count)}
      if (errors.isNotEmpty()) {
        throw GrammarValidationException("Defined process names must be unique: ${errors.joinToString(separator = ", ")}")
      }
    }
  }
}
