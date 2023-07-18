package org.example.pg

fun Grammar.Companion.fromDsl(block: Builder.() -> Unit) = Builder().apply(block).build()

class Builder {
  private var name: Name? = null
  private val components = mutableListOf<Defined>()
  private var definedBuilder: DefinedProcessBuilder? = null

  fun build() = Grammar(components)

  operator fun String.invoke(block: DefinedProcessBuilder.() -> Unit): String {
    describe(this, block)
    return this
  }

  private fun describe(processName: String, block: DefinedProcessBuilder.() -> Unit) {
    name = Name(processName).also { name ->
      definedBuilder = DefinedProcessBuilder(name).also { builder ->
        builder.apply(block)
        components.add(builder.build())
      }
    }
  }

  @Suppress("FunctionName")
  class DefinedProcessBuilder(private val name: Name) {
    private var process: Process? = null

    infix fun Any.THEN(other: Any): BinOp = makeBinOp(this, other) { l, r ->
      Sequence(l, r) }
    infix fun Any.OR(other: Any): BinOp = makeBinOp(this, other) { l, r ->
      Decision(l, r) }
    infix fun Any.AND(other: Any): BinOp = makeBinOp(this, other) { l, r ->
      Parallel(l, r) }

    private fun makeBinOp(
      left: Any,
      right: Any,
      create: (l: Process,
               r: Process) -> BinOp
    ): BinOp {
      return create(left.asProcess(), right.asProcess())
        .also { process = it }
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
          Reference(Name(arg)) }
        catch (ex: IllegalArgumentException) {
          Terminal(arg) }
      }
      if (arg !is Process) { throw DSLParseException("could not convert $arg to Process<*>") }
      if (optional) { arg = Optional(arg) }
      return arg as? Process ?: throw DSLParseException("could not convert $arg to Process<String>")
    }
    fun build(): Defined {
      val p = process
      if (p == null) { throw DSLParseException("Can't build a process with only a name") }
      else { return Defined(name, p) }
    }
  }
}

fun String.isPascalCase() = "^([A-Z][a-z0-9]*)+$".toRegex().containsMatchIn(this)