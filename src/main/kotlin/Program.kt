package org.example.pg

@Suppress("UNCHECKED_CAST")
class Program(private val namespace: Namespace) {

  private var f: OnWord = { _: String -> throw UnrunnableProcess("Must 'begin' process")}

  fun begin(name: String) = begin(Name.Defined(name))

  operator fun invoke(word: String): Program {
    val fw = this.f(word)
    this.f = fw as? OnWord ?: { _: String -> throw UnrunnableProcess("Process exhausted") }
    return this
  }

  private fun begin(name: Name.Defined): Program {
    f = namespace[name] ?: throw UnrunnableProcess("Invalid name")
    return this
  }

  companion object {
    fun from(
      grammar: Grammar,
      expander: Expander = Expanders.equality) = Compiler(expander).compile(grammar)
  }
}