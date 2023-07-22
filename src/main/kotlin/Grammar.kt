package org.example.pg

/**
 * A collection of [Fn.Definition] processes which together define a language of possible programs.
 */
class Grammar(val processes: List<Fn.Definition>) {

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

    fun validateGrammar(components: List<Fn.Definition>) {
      validateAtLeastOneComponent(components)
      validateUniqueNames(components)
//      validateSufficientParams()

    }

//    private fun validateSufficientParams() {
//      TODO("Not yet implemented")
//    }

    private fun validateAtLeastOneComponent(components: List<Fn.Definition>) {
      if (components.isEmpty()) throw GrammarValidationException("cannot create grammar with zero processes")
    }

    private fun validateUniqueNames(components: List<Fn.Definition>) {
      fun message(name: ProcessName, count: Int) = "$name defined $count times"
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
