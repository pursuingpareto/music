package org.example.pg

fun Grammar.Companion.fromDsl(block: Builder.() -> Unit) = Builder().apply(block).build()

class Builder {
  private var name: Process.Name? = null
  private val components = mutableListOf<Defined>()
  private var definedBuilder: DefinedProcessBuilder? = null

  fun build() = Grammar(components)

  operator fun String.invoke(block: DefinedProcessBuilder.() -> Unit): String {
    describe(this, block)
    return this
  }

  private fun describe(processName: String, block: DefinedProcessBuilder.() -> Unit) {
    name = Process.Name(processName).also { name ->
      definedBuilder = DefinedProcessBuilder(name).also { builder ->
        builder.apply(block)
        components.add(builder.build())
      }
    }
  }

  @Suppress("FunctionName")
  class DefinedProcessBuilder(private val name: Process.Name) {
    private var process: Process? = null

    infix fun Any.THEN(other: Any): Binary = makeBinOp(this, other) { l, r ->
      Binary.Sequence(l, r) }
    infix fun Any.OR(other: Any): Binary = makeBinOp(this, other) { l, r ->
      Binary.Decision(l, r) }
    infix fun Any.AND(other: Any): Binary = makeBinOp(this, other) { l, r ->
      Binary.Eclipse(l, r) }

    private fun makeBinOp(left: Any, right: Any,
      create: (l: Process, r: Process) -> Binary): Binary {
      return create(left.asProcess(), right.asProcess()).also { process = it }
    }

    @Suppress("UNCHECKED_CAST")
    private fun Any.asProcess(): Process {
      var arg = this
      var optional = false
      if (arg is Function0<*>) {
        optional = true
        arg = arg.invoke() as Any
      }
      if (arg is String) {
        arg = try {
          Reference(Process.Name(arg)) }
        catch (ex: IllegalArgumentException) {
          Terminal(arg) }
      }
      if (arg !is Process) { throw DSLParseException("could not convert $arg to Process") }
      if (optional) { arg = Optional(arg) }
      return arg as? Process ?: throw DSLParseException("could not convert $arg to Process>")
    }
    fun build(): Defined {
      val p = process
      if (p == null) { throw DSLParseException("Can't build a process with only a name") }
      else { return Defined(name, p) }
    }
  }
}

fun String.isPascalCase() = "^([A-Z][a-z0-9]*)+$".toRegex().containsMatchIn(this)