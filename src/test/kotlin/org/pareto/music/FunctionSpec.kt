package org.pareto.music

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.pareto.music.perform.Decider
import org.pareto.music.perform.StringBuilderPerformer
import org.pareto.music.rehearse.thread_validator.Program
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.todo

@Suppress("LocalVariableName")
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
            val function = grammar.definitions.first().music
            val caller = grammar.definitions.last().music
            assertIs<Melody>(function)
            assertIs<Melody>(caller)
            assertEquals("foo", (caller.Tick as Fn.Call).params.first().canonical())
            assertEquals("bar", (caller.Tock as Note).canonical())
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
        fun `function calls are resolved eagerly when passed as params`() = todo {
            val Quadruple = "Quadruple"
            val word = "word"
            val RandomWord = "RandomWord"
            val Times16 = "Times16"
            val w = "w"
            val x = "x"
            val y = "y"
            val z = "z"
            val RandomWord16Times = "RandomWord16Times"

            val grammar = Grammar.compose {
                Times16(word) {
                    Quadruple(Quadruple(word))
                }

                Quadruple(word) {
                    word then word then word then word
                }

                RandomWord {
                    w or x or y or z
                }

                RandomWord16Times {
                    Times16(RandomWord)
                }
            }
            val string = StringBuilderPerformer(grammar, Decider.UniformRandom).play(RandomWord16Times)
            println(string)
            assertEquals(16, string.split(" ").size)
            assert(string.split(" ").toSet().size == 1)
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
        fun `required params must appear in function body`() {
            assertThrows<DSLParseException> {
                Grammar.compose(false) {
                    "Function"("a", "b", "c", "d") {
                        "a" then "b" then "c"
                    }
                }
            }
        }

        @Test
        fun `param names must be unique within definition site`() {
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
            val body = grammar.definitions.last().music as Melody
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
