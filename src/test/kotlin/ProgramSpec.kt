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
        fun `branches are taken when they  user input`() {

        }

        @Test
        fun `must not be ambiguous`() {
            // both branches require an "a" as the first step, which is not allowed
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
    }
}