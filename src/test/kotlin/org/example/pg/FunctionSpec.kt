package org.example.pg

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.todo

class FunctionSpec {

    @Nested
    inner class DSL {
        @Test
        fun `functions can require params by defining with parens`() {
            val grammar = Grammar.compose(false) {
                "Function"("a", "b", "c") {
                    "a" then "b" then "c"
                }
            }
            val function = grammar.definitions.first()
            assertEquals(3, function.requiredArgs.size)
        }

        @Test
        fun `references can pass params into function with required args`() {
            val grammar = Grammar.compose(false) {
                "Function"("a") {
                    "a" then "b"
                }

                "Caller" {
                    "Function"("foo") then "bar"
                }
            }
            val function = grammar.definitions.first().process
            val caller = grammar.definitions.last().process
            assertIs<Sequence>(function)
            assertIs<Sequence>(caller)
            assertEquals("foo", (caller.Tick as Fn.Call).params.first().canonical())
            assertEquals("bar", (caller.Tock as Expanding).canonical())
        }

        @Test
        fun `parameters can be passed through multiple functions`() {
            val grammar = Grammar.compose(false) {
                "Repeat"("process") {
                    "process" then { "Repeat"("process") }
                }

                "Song"("hook") {
                    "intro" then "Repeat"(
                        "verse" then "Chorus"("hook")
                    )
                        .then("conclusion")
                }

                "Chorus"("hook") {
                    "hook" then "middle" then "hook"
                }
            }

            val prog = Program.from(grammar)
            prog("Song")("intro")("verse")("hook")
            prog("middle")
            prog("hook")
            prog("conclusion")
            prog("END")
        }

        @Test
        fun `parameter passing works`() {
            val grammar = Grammar.compose(false) {
                "Function"("a") {
                    "a" then "b"
                }

                "Caller" {
                    "Function"("foo") then "bar"
                }
            }
            val prog = Program.from(grammar)
            prog("Caller")
            prog("foo")
            prog("b")
            prog("bar")
            prog("END")
        }

        @Test
        fun `programs fail as expected while passing params`() {
            val grammar = Grammar.compose(false) {
                "Function"("a") {
                    "a" then "b"
                }

                "Caller" {
                    "Function"("foo") then "bar"
                }
            }
            val prog = Program.from(grammar)
            prog("Caller")
            assertThrows<NoMatchForInput> { prog("b") }
        }

        @Test
        fun `required params must appear in function body`() = todo {
            assertThrows<DSLParseException> {
                Grammar.compose(false) {
                    "Function"("a", "b", "c", "d") {
                        "a" then "b" then "c"
                    }
                }
            }
        }

        @Test
        fun `param names must be unique within definition site`() = todo {
            assertThrows<DSLParseException> {
                Grammar.compose(false) {
                    "Function"("a", "b", "b") {
                        "a" then "b" then "c"
                    }
                }
            }
        }

        @Test
        fun `repeated calls work`() {
            val grammar = Grammar.compose(false) {
                "Repeated"("process") {
                    "process" then "Repeated"("process")
                }

                "Heart" {
                    "Repeated"("beat")
                }
            }

            val prog = Program.from(grammar)
            prog.invoke("Heart")
            prog.invoke("beat")
            prog.invoke("beat")
            prog.invoke("beat")
            prog.invoke("beat")
            prog.invoke("beat")
            prog.invoke("beat")
            assertThrows<NoMatchForInput> { prog.invoke("whoops") }
        }

        @Test
        fun `nested function calls allowed`() {
            val grammar = Grammar.compose {
                "Function"("a") {
                    "a" then "b"
                }

                "Caller" {
                    "Function"("Function"("foo")) then "bar"
                }
            }
            val body = grammar.definitions.last().process as Sequence
            assertEquals("bar", body.Tock.canonical())
            val tick = body.Tick
            assertIs<Fn.Call>(tick)
            assertEquals("Function", tick.name.toString())
            assertEquals("Function(foo)", tick.params.first().canonical())
            val prog = Program.from(grammar).invoke("Caller")
            prog.invoke("foo")
            prog.invoke("b")
            prog.invoke("b")
            prog.invoke("bar")
            prog.invoke("END")
        }
    }
}
