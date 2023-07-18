package org.example.pg

sealed interface IProcess {
  infix fun and(other: IProcess): IParallel
  infix fun and(other: String): IParallel

  infix fun or(other: IProcess): IDecision
  infix fun or(other: String): IDecision

  infix fun then(other: IProcess): ISequence
  infix fun then(other: String): ISequence

  infix fun named(name: Name): IDefined
  infix fun named(other: String): IDefined

  fun optional(): IOptional
}

interface ITerminal: IProcess { val obj: String }

interface IReference: IProcess { val referencedName: Name }

interface IDefined: IProcess {
  val name: Name
  val process: IProcess
  val reference: IReference
}

interface IOptional: IProcess { val process: IProcess }

sealed interface IBinOp: IProcess {
  val left: IProcess
  val right: IProcess
  val separator: Char
}

interface ISequence: IBinOp {
  val first: IProcess
    get() = left
  val last: IProcess
    get() = right
  override val separator: Char
    get() = '>'
}
interface IParallel: IBinOp {
  val foreground: IProcess
    get() = left
  val background: IProcess
    get() = right
  override val separator: Char
    get() = '&'
}
interface IDecision: IBinOp {
  val default: IProcess
    get() = left
  val choice: IProcess
    get() = right
  override val separator: Char
    get() = '|'
}