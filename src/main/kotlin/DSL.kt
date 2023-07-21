package org.example.pg

fun Grammar.Companion.fromDsl(block: Builder.() -> Unit) = Builder().apply(block).build()

infix fun Grammar.extend(block: Builder.() -> Unit) = this.extend(Builder().apply(block).build())

class Builder {
  private var name: Name.Defined? = null
  private val components = mutableListOf<Defined>()
  private var definedBuilder: DefinedProcessBuilder? = null

  fun build() = Grammar(components)

  operator fun String.invoke(block: DefinedProcessBuilder.() -> Unit) = this.apply {
      name = Name.Defined(this).also { name ->
        definedBuilder = DefinedProcessBuilder(name).also { builder ->
          builder.apply(block)
          components.add(builder.build()) } } }


  class DefinedProcessBuilder(private val name: Name.Defined) {
    private var process: Process? = null

    /**
     * Constructs a [Dimension.Time] from **`process1`** and **`process2`**
     *
     * Example Usage:
     * ```
     * val a_then_b = "a" then "b"
     * ```
     */
    infix fun Any.then(other: Any): Dimension = makeBinOp(this, other) {
        l, r -> Dimension.Time(l, r) }

    /**
     * Constructs a [Dimension.Choice] from **`process1`** and **`process2`**
     *
     * Example Usage:
     * ```
     * val a_or_b = "a" or "b"
     * ```
     */
    infix fun Any.or(other: Any): Dimension = makeBinOp(this, other) {
        l, r -> Dimension.Choice(l, r) }

    /**
     * Constructs a [Dimension.Space] from **`process1`** and **`process2`**
     *
     * Example Usage:
     * ```
     * val a_and_b = "a" and "b"
     * ```
     */
    infix fun Any.and(other: Any): Dimension = makeBinOp(this, other) {
        l, r -> Dimension.Space(l, r) }

    fun build(): Defined {
      return process?.let { Defined(name, it) }
        ?: throw DSLParseException("can't build null process") }

    private fun makeBinOp(
      left: Any,
      right: Any,
      create: (l: Process, r: Process) -> Dimension
    ): Dimension {
      return create(left.asProcess(), right.asProcess())
        .also { process = it }
    }

    /**
     * Coerces receiver of [Any] type to [Process]. Fails unless receiver is
     * [String], [Process], or a 0-arity function.
     *
     * When the receiver is a [Function], call it and return an [Optional] with
     * child `process` equal to the coerced return value.
     *
     * When receiver is a String, first try to coerce it to a [Reference].
     * If this fails (which will happen if the string is not PascalCase), then
     * coerce to a [Expanding] process.
     */
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
          Reference(Name.Defined(arg)) }
        catch (ex: IllegalArgumentException) {
          Expanding(arg) }
      }
      if (arg !is Process) { throw DSLParseException("could not convert $arg to Process") }
      if (optional) { arg = Optional(arg) }
      return arg as? Process ?: throw DSLParseException("could not convert $arg to Process>")
    }
  }
}