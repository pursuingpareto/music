package org.example.pg

@JvmInline
value class Name(private val name: String) {
  init { require(name.isPascalCase()) }
  override fun toString() = name
}
sealed class Process : IProcess {
  override infix fun and(other: IProcess)  = Parallel(this, other)
  override infix fun and(other: String)    = Parallel(this, Terminal(other))

  override infix fun or(other: IProcess)   = Decision(this, other)
  override infix fun or(other: String)     = Decision(this, Terminal(other))

  override infix fun then(other: IProcess) = Sequence(this, other)
  override infix fun then(other: String)   = Sequence(this, Terminal(other))

  override infix fun named(name: Name)     = Defined(name, this)
  override infix fun named(other: String)  = Defined(Name(other), this)

  override fun optional() = Optional(this)
}
data class Terminal(override val obj: String): Process(), ITerminal {
  override fun toString() = obj
}
data class Reference(override val referencedName: Name): Process(), IReference {
  override fun toString() = referencedName.toString()
}

data class Defined(override val name: Name, override val process: Process): Process(), IDefined {
  override val reference = Reference(name)
  override fun toString() = "$name\n  : $process"
}

data class Optional(override val process: Process): Process(), IOptional {
  override fun toString() = "[ $process ]"
}

sealed class BinOp(override val left: IProcess, override val right: IProcess): Process(), IBinOp {
  override fun toString() = "$left $separator $right"
}

data class Sequence(
  override val first: IProcess,
  override val last: IProcess): BinOp(first, last), ISequence

data class Decision(
  override val default: IProcess,
  override val choice: IProcess): BinOp(default, choice), IDecision

data class Parallel(
  override val foreground: IProcess,
  override val background: IProcess): BinOp(foreground, background), IParallel
