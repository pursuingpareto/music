package org.example.pg

typealias P = (String) -> Any?
typealias T = String.(String) -> Any?

@Suppress("UNCHECKED_CAST")
class Program(val terminal: T = { word -> word == this}) {
  private val namespace = mutableMapOf<String, P>()
  private var f: P = { _: String -> println("Process Exhausted!") }
  fun begin(name: String): Program {
    this.f = this.namespace[name] ?: throw UnrunnableProcess("Invalid name")
    return this
  }

  operator fun invoke(word: String): Program {
    val fw = this.f(word)
    this.f = fw as? P ?: { _: String -> throw UnrunnableProcess("Process exhausted") }
    return this
  }

  companion object {
    fun from(grammar: Grammar, terminal: T = { word -> word == this}): Program {
      val program = Program(terminal)
      grammar.processes.forEach { program.functionFrom(it) }
      return program
    }
  }

  private fun term(obj: String): P = { word -> terminal(obj, word)}

  private fun opt(process: P) = { word: String ->
    process(word) }

  private fun alt(a: P, b: P) = { word: String ->
    a(word) ?: b(word) }

  private fun ref(name: String) = { word: String ->
    this.namespace[name]?.invoke(word) }

  private fun seq(x: P, y: P): P =   { word: String ->
    when (val xw = x(word)) {
      null -> y(word)
      true -> { w: String -> y(w) }
      false -> throw UnrunnableProcess()
      else -> seq(xw as P, y) } }

  private fun named(p: P, name: String): P {
    this.namespace[name] = p
    return { word: String -> p(word) } }

  private fun functionFrom(process: Process): P {
    return when (process) {
      is Binary.Sequence  -> seq(functionFrom(process.first), functionFrom(process.last))
      is Binary.Decision  -> alt(functionFrom(process.left), functionFrom(process.right))
      is Optional  -> opt(functionFrom(process.process))
      is Terminal  -> term(process.obj)
      is Defined   -> named(functionFrom(process.process), process.name.toString())
      is Reference -> ref(process.referencedName.toString())
      else         -> throw UnrunnableProcess("not supported ${process}!")
    }
  }
}