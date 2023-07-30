package org.pareto.music.perform

import org.pareto.music.Decision
import org.pareto.music.Fn
import org.pareto.music.FunctionNamespace
import org.pareto.music.Music
import org.pareto.music.UnrunnableProcess
import kotlin.random.Random


/**
 * A [Decider] [decide]s between the two branches in a [Dilemma] in a [FunctionNamespace].
 */
interface Decider {

    fun decide(dilemma: Dilemma, inNamespace: FunctionNamespace<Fn.Definition>): Dilemma.Choice

    fun decide(decision: Decision, inNamespace: FunctionNamespace<Fn.Definition>) =
        decide(Dilemma(decision.Will, decision.Wont), inNamespace)

    class Dilemma(val will: Music, val wont: Music) {

        /**
         * Chooses the [Decision].Will branch.
         */
        private fun will() = Choice(will)

        /**
         * Chooses the [Decision].Wont branch.
         */
        private fun wont() = Choice(wont)

        /**
         * Chooses the Will branch with probability [by] / ([by] + [to])
         */
        fun preferWillByRatio(by: Int, to: Int): Choice {
            val total = (by + to).toDouble()
            return if (Random.nextDouble(until = total) < by.toDouble())
                will() else wont()
        }

        /**
         * Chooses the Wont branch with probability [by] / ([by] + [to])
         */
        @Suppress("Unused")
        fun preferWontByRatio(by: Int, to: Int) = preferWillByRatio(to, by)

        inner class Choice(val choice: Music)
    }

    object UniformRandom: RandomDecider()
}

open class RandomDecider: Decider {
    override fun decide(dilemma: Decider.Dilemma, inNamespace: FunctionNamespace<Fn.Definition>): Decider.Dilemma.Choice {
        val willCount = dilemma.will.branchCount(inNamespace)
        val wontCount = dilemma.wont.branchCount(inNamespace)
        return dilemma.preferWillByRatio(willCount, wontCount)
    }

    private fun Music.branchCount(inNamespace: FunctionNamespace<Fn.Definition>): Int = when(this) {
        is Decision -> Will.branchCount(inNamespace) + Wont.branchCount(inNamespace)
        is Fn.Call -> inNamespace[name]?.call(params, inNamespace)?.branchCount(inNamespace) ?: throw UnrunnableProcess("Function does not exist")
        else -> 1
    }
}