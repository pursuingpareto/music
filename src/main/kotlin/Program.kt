package org.example.pg

class Program(private val namespace: Namespace) {

  /**
   * [f] is a function which gets called and reassigned every time
   * a [Program] is [invoke]d
   */
  private var f: OnWord = { word -> begin(Fn.Name(word)) }

  operator fun invoke(word: Word) = this
    .apply {
      f = when (val fw = this.f(word)) {
        true -> { w: Word -> if (w == Keyword.END) true else throw ProcessExhausted() }
        null -> throw NoMatchForInput(word)
        else -> fw as OnWord } }

  private fun begin(name: Fn.Name) = namespace[name]?.also { f = it }
    ?: throw NoMatchForInput(name.toString())

  companion object {
//    fun from(grammar: Grammar, expander: Expander = Expanders.equality): Program {
//      return Compiler(expander).compile(grammar)
//    }

    fun from(grammar: Grammar): Program {
      return Program(GrammarContext(grammar).functionNamespace.mapValues { it.value() })
    }
  }
}