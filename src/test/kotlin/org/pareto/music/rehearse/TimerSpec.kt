package org.pareto.music.rehearse

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.pareto.music.Grammar
import org.pareto.music.ProcessException
import org.pareto.music.canon.compose
import org.pareto.music.rehearse.threadValidator.Program
import org.pareto.music.rehearse.threadValidator.timer

class TimerSpec {
    private val abc = Grammar.compose {
        "ABC" {
            "a" then "b" then "c"
        }
    }

    @Test
    fun `timer measures times`() {
        val program = Program.from(abc)
        val timer = program.timer("ABC")

        // TODO - use a stubbed clock or some better way to make these tests
        timer.transitionTo("a").also { Thread.sleep(15)}
            .transitionTo("b").also { Thread.sleep(7)}
            .transitionTo("c").also { Thread.sleep(3)}

        val report = timer.stop()

        assertEquals(3, report.size)
        assertEquals(listOf("a", "b", "c"), report.map { it.name } )
        val aDuration = report.first().durationNanos
        val cDuration = report.last().durationNanos
        assertTrue(aDuration >= 15 * 1_000_000 && aDuration < 30 * 1_000_000)
        assertTrue(cDuration >= 3 * 1_000_000 && cDuration < 6 * 1_000_000)
    }

    @Test
    fun `timer fails if instantiated with invalid function name`() {
        assertThrows<ProcessException> {
            val program = Program.from(abc)
            program.timer("abc")
        }

        assertThrows<ProcessException> {
            val program = Program.from(abc)
            program.timer("a")
        }

        assertThrows<ProcessException> {
            val program = Program.from(abc)
            program.timer("ABCD")
        }
    }

    @Test
    fun `timer fails on invalid transition`() {
        val program = Program.from(abc)
        val timer = program.timer("ABC")
        timer.transitionTo("a")
        timer.transitionTo("b")
        assertThrows<ProcessException> { timer.transitionTo("d") }
    }

    @Test
    fun `timer fails on manual END`() {
        val program = Program.from(abc)
        val timer = program.timer("ABC")
        timer.transitionTo("a")
        timer.transitionTo("b")
        timer.transitionTo("c")
        assertThrows<ProcessException> { timer.transitionTo("END") }
    }

    @Test
    fun `timer fails on early stop`() {
        val program = Program.from(abc)
        val timer = program.timer("ABC")
        timer.transitionTo("a")
        timer.transitionTo("b")
        assertThrows<ProcessException> { timer.stop() }
    }
}
