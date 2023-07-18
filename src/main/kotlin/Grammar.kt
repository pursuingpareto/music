package org.example.pg

class Grammar(val processes: List<Defined>) {
  infix fun extend(other: Grammar) = Grammar(this.processes + other.processes)
  init { validateGrammar(processes) }
  override fun toString() = processes.joinToString(separator = "\n\n") { it.toString() }
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
