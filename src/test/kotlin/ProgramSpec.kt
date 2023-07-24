import Grammars.RPSGrammar
import Grammars.RockPaperScissors
import Grammars.celebrate
import Grammars.paper
import Grammars.rock
import Grammars.scissors
import Grammars.shoot
import Grammars.win
import org.example.pg.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class ProgramSpec {

    private val optionalAThenB = Grammar.fromDsl {
        "OptionalAThenB" {
            { "a" } then "b"
        }
    }

    private val aThenOptionalB = Grammar.fromDsl {
        "AThenOptionalB" {
            "a" then { "b" }
        }
    }

    private val optionalMiddle = Grammar.fromDsl {
        "OptionalMiddle" {
            "a" then { "b" } then "c"
        }
    }

    private val ab = Grammar.fromDsl {
        "AB" {
            "a" then "b"
        }
    }

    private val abc = Grammar.fromDsl {
        "ABC" {
            "a" then "b" then "c"
        }
    }

    @Nested
    inner class Lifecycle {

        @Test
        fun `programs can invoke and transition and end`() {
            val grammar = Grammar.fromDsl {
                "Program" {
                    "invokening" then "transition" then "finish"
                }
            }

            // this is good!
            Program.from(grammar)
                .invoke("Program")
                .invoke("invokening")("transition")("finish")
                .invoke("END")

            // but it can fail at any of the phases...
            // like in the invokening:
            assertThrows<NoMatchForInput> {
                Program.from(grammar)
                    .invoke("Pilgrim") }

            // or in the middle:
            assertThrows<NoMatchForInput> {
                Program.from(grammar)
                    .invoke("Program")
                    .invoke("invokening")("finish") }

            // or by ending before the process has finished
            assertThrows<NoMatchForInput> {
                Program.from(grammar)
                    .invoke("Program")
                    .invoke("invokening")("transition")
                    .invoke("END")
            }
        }
    }

    @Nested
    inner class Sequences {

        @Test
        fun `simple sequence runs successfully`() {
            Program.from(ab).invoke("AB")("a")("b")
        }

        @Test
        fun `simple sequence fails after exhaustion`() {
            assertThrows<UnrunnableProcess> {
                Program.from(ab).invoke("AB")("a")("b")("c")
            }
        }

        @Test
        fun `three element sequence runs successfully`() {
            Program.from(abc).invoke("ABC")("a")("b")("c")
        }

        @Test
        fun `three element sequence fails when improperly called`() {
            assertThrows<UnrunnableProcess> {
                Program.from(abc).invoke("ABC")("a")("b")("c")("d")
            }

            assertThrows<UnrunnableProcess> {
                Program.from(abc).invoke("ABC")("a")("b")("d")
            }
        }

        @Nested
        inner class WithOptionals {

            @Test
            fun `optional at invokening of sequence is optional`() {
                Program.from(optionalAThenB)
                    .invoke("OptionalAThenB")
                    .invoke("a")("b")

                Program.from(optionalAThenB)
                    .invoke("OptionalAThenB")
                    .invoke("b")

                assertThrows<UnrunnableProcess> {
                    Program.from(optionalAThenB)
                        .invoke("OptionalAThenB")
                        .invoke("fail")
                }
            }

            @Test
            fun `optional at end of sequence is optional`() {
                Program.from(aThenOptionalB)
                    .invoke("AThenOptionalB")
                    .invoke("a")("b")

                assertThrows<UnrunnableProcess> {
                    Program.from(aThenOptionalB)
                        .invoke("AThenOptionalB")
                        .invoke("b")
                }
            }

            @Test
            fun `optional in middle of sequence can be included`() {
                Program.from(optionalMiddle)
                    .invoke("OptionalMiddle")
                    .invoke("a")("b")("c")
            }

            @Test
            fun `optional in middle of sequence can be skipped`() {
                Program.from(optionalMiddle)
                    .invoke("OptionalMiddle")
                    .invoke("a")("c")
            }

            @Test
            fun `sequences with two optionals are not allowed`() {
                val grammar = Grammar.fromDsl {
                    "DoubleOptional" {
                        { "a" } then { "b" }
                    }
                }

                val invoke = { Program.from(grammar).invoke("DoubleOptional") }

                // these are both allowed
                invoke()("a")("b")
                invoke()("b")

                // these are not
                assertThrows<NoMatchForInput> {
                    invoke()("blah") }
                assertThrows<ProcessExhausted> {
                    invoke()("a")("b")("c") }
                assertThrows<NoMatchForInput> {
                    invoke()("a")("c") }
            }

            @Test
            fun `three element sequence with optional middle fails when called incorrectly`() {
                assertThrows<UnrunnableProcess> {
                    Program.from(optionalMiddle)
                        .invoke("OptionalMiddle")
                        .invoke("a")("d")
                }

                assertThrows<UnrunnableProcess> {
                    Program.from(optionalMiddle)
                        .invoke("OptionalMiddle")
                        .invoke("a")("b")("f")
                }

                assertThrows<UnrunnableProcess> {
                    Program.from(optionalMiddle)
                        .invoke("OptionalMiddle")
                        .invoke("a")("c")("f")
                }
            }
        }
    }

    @Nested
    inner class Decisions {

        @Test
        fun `branches are committed to when they match user input`() {
            val grammar = Grammar.fromDsl {
                "Branching" {
                    ("a" then "b") or
                    ("c" then "d")
                }
            }

            val invoke = { Program.from(grammar).invoke("Branching") }

            // taking the first branch is allowed
            invoke()("a")("b")

            // taking the second branch is allowed
            invoke()("c")("d")

            // but you can't "change your mind" once you've committed to a branch
            assertThrows<NoMatchForInput> {
                invoke()("a")("c") }
            assertThrows<NoMatchForInput> {
                invoke()("a")("d") }
            assertThrows<NoMatchForInput> {
                invoke()("c")("a") }
            assertThrows<NoMatchForInput> {
                invoke()("c")("b") }
        }

        @Test
        fun `branches fail on invalid input`() {
            val grammar = Grammar.fromDsl {
                "Branching" {
                    ("a" then "b") or
                    ("c" then "d")
                } }

            val invoke = { Program.from(grammar).invoke("Branching") }

            assertThrows<NoMatchForInput> { invoke()("b") }
            assertThrows<NoMatchForInput> { invoke()("d") }
        }

        @Test
        fun `must not be ambiguous`() {
            // Note that BOTH branches start with "a", which makes this ambiguous.
            val grammar = Grammar.fromDsl {
                "AmbiguousBranch" {
                    ("a" then "b") or
                    ("a" then "c")
                }
            }
            assertThrows<AmbiguousBranching> {
                Program.from(grammar).invoke("AmbiguousBranch")("a")
            }
        }

        /**
         * Branches may not be optional. But they might contain optionals.
         */
        @Nested
        inner class InvolvingOptionals {

            @Test
            fun `branches may not be optional`() {
                assertThrows<RuntimeException> {
                    Program.from(Grammar.fromDsl {
                        "OptionalBranch" {
                            { "a" } or "b"
                        }})}

                assertThrows<RuntimeException> {
                    Program.from(Grammar.fromDsl {
                        "OptionalBranch" {
                            "a" or { "b" }
                        }})}
            }

            @Test
            fun `sequence branches with optionals fail at runtime when ambiguous`() {
                val grammar = Grammar.fromDsl {
                    "Branching" {
                        ({"a"} then "b") or ("b" then "c")
                    }}

                val invoke = { Program.from(grammar).invoke("Branching") }

                // unambiguous calls are fine
                invoke()("a")("b")

                // but ambiguous calls fail
                assertThrows<AmbiguousBranching> { invoke()("b") }
            }
        }
    }

//    @Test
//    fun `the compiler can be modified to add additional functionality`() {
//        val timings = mutableListOf<Pair<ProcessName, Double>>()
//        var start = System.nanoTime()
//        Program
//            .from(RPSGrammar) { word ->
//                val now = System.nanoTime()
//                val dur = now - start
//                start = now
//                timings.add(Pair(this, dur / 1000.0))
//                if ("$this" != word) throw NoMatchForInput("Invalid!") else true
//            }
//            .invoke(RockPaperScissors)
//            .invoke(rock).also { Thread.sleep(42) }
//            .invoke(paper).also { Thread.sleep(33) }
//            .invoke(scissors)(shoot)(rock)(win)(celebrate)
//
//        assertEquals(11, timings.count())
//    }
}