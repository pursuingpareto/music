package org.example.pg

@Suppress("UNCHECKED_CAST")
class Program(private val namespace: Namespace) {

  private val end = "END"
  private var f: OnWord = { _: String -> throw UnrunnableProcess("Must 'begin' process")}

  fun begin(name: String) = begin(Name.Defined(name))

  fun end() = invoke(end)

  operator fun invoke(word: String): Program {
    val fw = this.f(word)
    this.f = when (fw) {
      true -> { w: String -> if (w == end) true else throw ProcessExhausted() }
      null -> throw NoMatchForInput(word)
      else -> fw as OnWord
    }
    return this
  }

  private fun begin(name: Name.Defined): Program {
    f = namespace[name] ?: throw NoMatchForInput(name.toString())
    return this
  }

  companion object {
    fun from(
      grammar: Grammar,
      expander: Expander = Expanders.equality) = Compiler(expander).compile(grammar)
  }
}