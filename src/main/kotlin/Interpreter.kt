package org.example.pg


//fun interface PGRunnable {
//    operator fun invoke(input: Word): Interpreter.RunResult
//}

//typealias RunResult = Interpreter.RunResult


class Interpreter(val functionNamespace: Map<Fn.Name, Fn.Definition>) {


//
//    private var lastRunResult: RunResult? = null
//
//    sealed interface RunResult: PGRunnable {
//
//        override operator fun invoke(input: Word): RunResult {
//            return when(this) {
//                is Success.ProgramContinues -> this.continueWith(input)
//                is Success.ProgramComplete -> Fail.ProcessExhausted
//                is Fail -> Fail.AlreadyFailed
//            }
//        }
//
//        enum class Fail: RunResult {
//            AmbiguousBranch, NoMatchForInput, ProcessExhausted, AlreadyFailed
//        }
//
//        sealed class Success(open val continueWith: PGRunnable?): RunResult {
//
//            object ProgramComplete: Success(null)
//
//            inner class ProgramContinues(override val continueWith: PGRunnable): Success(continueWith)
//        }
//    }
//
//    private fun continueWith(process: Process, inNamespace: Map<ProcessName, Process>) = PGRunnable {
//            input -> process.evaluate(input, inNamespace)
//    }
//
//    override fun invoke(input: Word): RunResult {
//        return if (currentProcess == null) {
//            try {
//                name = Fn.Name(input)
//            } catch (ex: Exception) {
//                return RunResult.Fail.NoMatchForInput
//            }
//            val func = functionNamespace[name] ?: return RunResult.Fail.NoMatchForInput
//            RunResult.Success.ProgramContinues(continueWith(func.process))
//        }
//            Fn.Name(input).let {name ->
//                functionNamespace[name]?. let { definition ->
//                    { w: Word, definition.run(w) }
//                }
//            }
//            functionNamespace[Fn.Name(input)]?.let { definition ->
//                definition.run(word)
//            }
//            currentProcess = process
//            return if (process != null) RunResult.Success.continuingWith( process )
//        }
//    }
//
//    fun Process.evaluate(input: Word, inNamespace: Map<ProcessName, Process>): RunResult {
//        return when(this) {
//            is Expanding -> this.evaluate(input, inNamespace)
//            is Fn        -> this.evaluate(input, inNamespace)
//            is Optional  -> this.evaluate(input, inNamespace)
//            is Decision  -> this.evaluate(input, inNamespace)
//            is Sequence  -> this.evaluate(input, inNamespace)
//            is Parallel  -> throw NotImplementedError("No support for parallel processes")
//        }
//    }
//
//    private fun Expanding.evaluate(input: Word, inNamespace: Map<ProcessName, Process>): RunResult {
//        return inNamespace[Expanding.Name(input)]?.evaluate(input, inNamespace)
//            ?: if ("$obj" == input) {
//                Interpreter.RunResult.Success.ProgramContinues
//            }
//    }
//
//    private fun Optional.evaluate(input: Word, inNamespace: Map<ProcessName, Process>): RunResult {
//        TODO()
//    }
//
//    private fun Sequence.evaluate(input: Word, inNamespace: Map<ProcessName, Process>): RunResult {
//        TODO()
//    }
//
//    private fun Decision.evaluate(input: Word, inNamespace: Map<ProcessName, Process>): RunResult {
//        TODO()
//    }
//
//    private fun Fn.evaluate(input: Word, inNamespace: Map<ProcessName, Process>): RunResult {
//        TODO()
//    }


}

