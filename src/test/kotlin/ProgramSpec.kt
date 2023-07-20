import org.example.pg.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

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
        fun `programs can begin and transition and end`() {
            val grammar = Grammar.fromDsl {
                "Program" {
                    "beginning" then "transition" then "finish"
                }
            }

            // this is good!
            Program.from(grammar)
                .begin("Program")
                .invoke("beginning")("transition")("finish")
                .end()

            // but it can fail at any of the phases...
            // like in the beginning:
            assertThrows<NoMatchForInput> {
                Program.from(grammar)
                    .begin("Pilgrim") }

            // or in the middle:
            assertThrows<NoMatchForInput> {
                Program.from(grammar)
                    .begin("Program")
                    .invoke("beginning")("finish") }

            // or by ending before the process has finished
            assertThrows<NoMatchForInput> {
                Program.from(grammar)
                    .begin("Program")
                    .invoke("beginning")("transition")
                    .end()
            }
        }
    }

    @Nested
    inner class Sequences {

        @Test
        fun `simple sequence runs successfully`() {
            Program.from(ab).begin("AB")("a")("b")
        }

        @Test
        fun `simple sequence fails after exhaustion`() {
            assertThrows<UnrunnableProcess> {
                Program.from(ab).begin("AB")("a")("b")("c")
            }
        }

        @Test
        fun `three element sequence runs successfully`() {
            Program.from(abc).begin("ABC")("a")("b")("c")
        }

        @Test
        fun `three element sequence fails when improperly called`() {
            assertThrows<UnrunnableProcess> {
                Program.from(abc).begin("ABC")("a")("b")("c")("d")
            }

            assertThrows<UnrunnableProcess> {
                Program.from(abc).begin("ABC")("a")("b")("d")
            }
        }

        @Nested
        inner class WithOptionals {

            @Test
            fun `optional at beginning of sequence is optional`() {
                Program.from(optionalAThenB)
                    .begin("OptionalAThenB")
                    .invoke("a")("b")

                Program.from(optionalAThenB)
                    .begin("OptionalAThenB")
                    .invoke("b")

                assertThrows<UnrunnableProcess> {
                    Program.from(optionalAThenB)
                        .begin("OptionalAThenB")
                        .invoke("fail")
                }
            }

            @Test
            fun `optional at end of sequence is optional`() {
                Program.from(aThenOptionalB)
                    .begin("AThenOptionalB")
                    .invoke("a")("b")

                assertThrows<UnrunnableProcess> {
                    Program.from(aThenOptionalB)
                        .begin("AThenOptionalB")
                        .invoke("b")
                }
            }

            @Test
            fun `optional in middle of sequence can be included`() {
                Program.from(optionalMiddle)
                    .begin("OptionalMiddle")
                    .invoke("a")("b")("c")
            }

            @Test
            fun `optional in middle of sequence can be skipped`() {
                Program.from(optionalMiddle)
                    .begin("OptionalMiddle")
                    .invoke("a")("c")
            }

            @Test
            fun `sequences with two optionals are not allowed`() {
                val grammar = Grammar.fromDsl {
                    "DoubleOptional" {
                        { "a" } then { "b" }
                    }
                }

                val begin = { Program.from(grammar).begin("DoubleOptional") }

                // these are both allowed
                begin()("a")("b")
                begin()("b")

                // these are not
                assertThrows<NoMatchForInput> {
                    begin()("blah") }
                assertThrows<ProcessExhausted> {
                    begin()("a")("b")("c") }
                assertThrows<NoMatchForInput> {
                    begin()("a")("c") }
            }

            @Test
            fun `three element sequence with optional middle fails when called incorrectly`() {
                assertThrows<UnrunnableProcess> {
                    Program.from(optionalMiddle)
                        .begin("OptionalMiddle")
                        .invoke("a")("d")
                }

                assertThrows<UnrunnableProcess> {
                    Program.from(optionalMiddle)
                        .begin("OptionalMiddle")
                        .invoke("a")("b")("f")
                }

                assertThrows<UnrunnableProcess> {
                    Program.from(optionalMiddle)
                        .begin("OptionalMiddle")
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

            val begin = { Program.from(grammar).begin("Branching") }

            // taking the first branch is allowed
            begin()("a")("b")

            // taking the second branch is allowed
            begin()("c")("d")

            // but you can't "change your mind" once you've committed to a branch
            assertThrows<NoMatchForInput> {
                begin()("a")("c") }
            assertThrows<NoMatchForInput> {
                begin()("a")("d") }
            assertThrows<NoMatchForInput> {
                begin()("c")("a") }
            assertThrows<NoMatchForInput> {
                begin()("c")("b") }
        }

        @Test
        fun `branches fail on invalid input`() {
            val grammar = Grammar.fromDsl {
                "Branching" {
                    ("a" then "b") or
                    ("c" then "d")
                } }

            val begin = { Program.from(grammar).begin("Branching") }

            assertThrows<NoMatchForInput> { begin()("b") }
            assertThrows<NoMatchForInput> { begin()("d") }
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
                Program.from(grammar).begin("AmbiguousBranch")("a")
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

                val begin = { Program.from(grammar).begin("Branching") }

                // unambiguous calls are fine
                begin()("a")("b")

                // but ambiguous calls fail
                assertThrows<AmbiguousBranching> { begin()("b") }
            }
        }
    }
}