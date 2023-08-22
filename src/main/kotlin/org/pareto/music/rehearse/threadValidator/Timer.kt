package org.pareto.music.rehearse.threadValidator

import org.pareto.music.*


/**
 * Performs validation against a [Program]'s [Grammar] while timing the runtime of each step.
 *
 * ```kotlin
 * // before introducing MusicTimer
 * fun complexDomainSpecificLogic(shouldBranch: Boolean) {
 *     complexSetup()
 *     if (shouldBranch) {
 *         branchA()
 *     } else {
 *         branchB()
 *     }
 *     complexTeardown()
 * }
 *
 * // grammar describing complex domain-specific logic
 * val complexGrammar = Grammar.compose {
 *     "ComplexLogic" {
 *         "setup" then "AOrB" then "teardown"
 *     }
 *
 *     "AOrB" {
 *         "a" or "b"
 *     }
 * }
 *
 * // after including validation annotations
 * fun complexDomainSpecificLogic(shouldBranch: Boolean) {
 *     val timer = Program.from(complexGrammar)
 *                        .timer("ComplexLogic")
 *     timer.transitionTo("setup")
 *     complexSetup()
 *     if (shouldBranch) {
 *         timer.transitionTo("a")
 *         branchA()
 *     } else {
 *         timer.transitionTo("b")
 *         branchB()
 *     }
 *     timer.transitionTo("teardown")
 *     complexTeardown()
 *     val timingReport = timer.stop()
 * }
 * ```
 */
class MusicTimer(private val program: Program, rootFunctionName: String) {
    private val timer = Timer()
    private var exhausted = false

    init {
        try {
            Fn.Name(rootFunctionName)
        } catch (ex: Exception) {
            throw InvalidInput(cause = ex)
        }
        program.invoke(rootFunctionName)
    }

    fun transitionTo(text: String): MusicTimer {
        failIfExhausted()
        if (text == Keyword.END) throw InvalidInput("complete timing by calling stop")
        timer.start(text)
        program.invoke(text)
        return this
    }

    fun stop(): TimingReport {
        failIfExhausted()
        exhausted = true
        program.invoke(Keyword.END)
        return timer.stop()
    }

    private fun failIfExhausted() {
        if (exhausted) throw ProcessExhausted("timer has already been used")
    }
}


fun Program.timer(rootFunctionName: String) = MusicTimer(this, rootFunctionName)


typealias TimingReport = List<NamedDuration>


data class NamedDuration(val name: String, val durationNanos: Long)


private class Timer {

    private class CompletedTiming(val name: String, val durationNanos: Long)
    private class StartedTiming(val name: String, val startNanoTime: Long) {
        fun completeAt(endNanoTime: Long) =
            CompletedTiming(name, endNanoTime - startNanoTime)
    }

    private var currentTiming: StartedTiming? = null
    private val completedTimings = mutableListOf<CompletedTiming>()

    fun start(name: String) {
        val now = System.nanoTime()
        currentTiming?.completeAt(now)?.also { completedTimings.add(it) }
        currentTiming = StartedTiming(name, now)
    }

    fun stop(): TimingReport {
        if (currentTiming == null) {
            throw InvalidInput("Nothing was timed.")
        }
        currentTiming?.completeAt(System.nanoTime())?.also { completedTimings.add(it) }
        return completedTimings.map { NamedDuration(it.name, it.durationNanos) }
    }
}
