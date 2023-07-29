package org.pareto.music

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.pareto.music.compiler.thread_validator.Program
import java.lang.IllegalArgumentException

class ProgramSpec {

    private val optionalAThenB = Grammar.compose {
        "OptionalAThenB" {
            { "a" } then "b"
        }
    }

    private val aThenOptionalB = Grammar.compose {
        "AThenOptionalB" {
            "a" then { "b" }
        }
    }

    private val optionalMiddle = Grammar.compose {
        "OptionalMiddle" {
            "a" then { "b" } then "c"
        }
    }

    private val ab = Grammar.compose {
        "AB" {
            "a" then "b"
        }
    }

    private val abc = Grammar.compose {
        "ABC" {
            "a" then "b" then "c"
        }
    }

    @Nested
    inner class Lifecycle {

        @Test
        fun `programs can invoke and transition and end`() {
            val grammar = Grammar.compose {
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
                    .invoke("Pilgrim")
            }

            // or in the middle:
            assertThrows<NoMatchForInput> {
                Program.from(grammar)
                    .invoke("Program")
                    .invoke("invokening")("finish")
            }

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
                val grammar = Grammar.compose {
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
                    invoke()("blah")
                }
                assertThrows<ProcessExhausted> {
                    invoke()("a")("b")("c")
                }
                assertThrows<NoMatchForInput> {
                    invoke()("a")("c")
                }
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
            val grammar = Grammar.compose {
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
                invoke()("a")("c")
            }
            assertThrows<NoMatchForInput> {
                invoke()("a")("d")
            }
            assertThrows<NoMatchForInput> {
                invoke()("c")("a")
            }
            assertThrows<NoMatchForInput> {
                invoke()("c")("b")
            }
        }

        @Test
        fun `branches fail on invalid input`() {
            val grammar = Grammar.compose {
                "Branching" {
                    ("a" then "b") or
                        ("c" then "d")
                }
            }

            val invoke = { Program.from(grammar).invoke("Branching") }

            assertThrows<NoMatchForInput> { invoke()("b") }
            assertThrows<NoMatchForInput> { invoke()("d") }
        }

        @Test
        fun `must not be ambiguous`() {
            // Note that BOTH branches start with "a", which makes this ambiguous.
            val grammar = Grammar.compose {
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
        inner class InvolvingEmpty {

            @Test
            fun `left branch may not be empty`() {
                assertThrows<DSLParseException> {
                    Program.from(
                        Grammar.compose {
                            "OptionalBranch" {
                                "" or ""
                            }
                        },
                    )
                }

                assertThrows<DSLParseException> {
                    Program.from(
                        Grammar.compose {
                            "OptionalBranch" {
                                "" or { "a" }
                            }
                        },
                    )
                }

                Program.from(
                    Grammar.compose {
                        "OptionalBranch" {
                            "a" or ""
                        }
                    },
                )
            }

            @Test
            fun `sequence branches with emptys fail at runtime when ambiguous`() {
                val grammar = Grammar.compose {
                    "Branching" {
                        ({ "a" } then "b") or ("b" then "c")
                    }
                }

                val prog = Program.from(grammar).invoke("Branching")

                // unambiguous calls are fine
                prog.invoke("a")("b")("END")

                // but ambiguous calls fail
                assertThrows<ProcessExhausted> { prog.invoke("b") }

                val prog2 = Program.from(grammar).invoke("Branching")

                assertThrows<AmbiguousBranching> { prog2("b") }
            }

            @Test
            fun `decisions with empty branch can be skipped`() {
                val g = Grammar.compose {
                    "F" {
                        "a" then ("b" or "") then "c"
                    }
                }
                Program.from(g)("F")("a")("b")("c")("END")
                val prog = Program.from(g)("F")("a")
                prog("c")
                prog("END")
            }
        }
    }

    @Test
    fun `empty program can be ended immediately`() {
        val g = Grammar.compose {
            "F" { "" then "" }
        }
        Program.from(g)("F")("END")
        assertThrows<NoMatchForInput> {
            Program.from(g)("F")("F")
        }
        assertThrows<IllegalArgumentException> {
            Program.from(g)("F")("")
        }
    }
}
