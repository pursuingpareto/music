import org.example.pg.*
import org.example.pg.Function
import org.example.pg.Sequence
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DSLSpec {

    private fun fromBuilder(block: Builder.DefinedProcessBuilder.() -> Unit): Process {
        val builder = builder()
        builder.apply(block)
        return builder.build().process
    }

    private fun builder(): Builder.DefinedProcessBuilder {
        val name = Name.Defined("SomeName")
        return Builder.DefinedProcessBuilder(name)
    }

    @Nested
    inner class Notation {

        @Test
        fun `sequences are created with THEN`() {
            val process = fromBuilder { "a" then "b" }
            assertIs<Sequence>(process)
        }

        @Test
        fun `decisions are created with OR`() {
            val process = fromBuilder { "a" or "b" }
            assertIs<Decision>(process)
        }

        @Test
        fun `parallel are created with AND`() {
            val process = fromBuilder { "a" and "b" }
            assertIs<Parallel>(process)
        }

        @Test
        fun `optionals are enclosed in braces`() {
            val process = fromBuilder { "a" then {"b"} } as Sequence
            assertIs<Optional>(process.tock)
        }

        @Test
        fun `terminals are non-PascalCase strings`() {
            val process = fromBuilder { "process_a" then "process_b" } as Sequence
            assertIs<Expanding>(process.tick)
            assertIs<Expanding>(process.tock)
        }

        @Test
        fun `references are PascalCase strings`() {
            val process = fromBuilder { "ProcessA" then "ProcessB" } as Sequence
            assertIs<Reference>(process.tick)
            assertIs<Reference>(process.tock)
        }

        @Test
        fun `defined processes are names followed by braces enclosing a process`() {
            val grammar = Grammar.fromDsl {
                "DefinedProcessName" {
                    "a" then "b"
                }
            }
            assertEquals(1, grammar.processes.count())
            assertIs<Function>(grammar.processes.first())
            assertEquals("DefinedProcessName", grammar.processes.first().name.toString())
        }

        @Test
        fun `grammars can be extended`() {
            val grammar = Grammar.fromDsl {
                "DefinedProcessName" {
                    "a" then "b"
                }
            }
            val extended = grammar.extend {
                "OtherProcessName" {
                    "b" then "c"
                }
            }
            assertEquals(2, extended.processes.count())
        }
    }

    @Nested
    inner class Optionals {
        @Test
        fun `can multi-enclose tail of complex subprocesses`() {
            val name = Name.Defined("Complex")
            val builder = Builder.DefinedProcessBuilder(name)
            with(builder) {
                "a" then { "b" or "c" and "d" }
            }
            val process = builder.build().process
            assertIs<Sequence>(process)
            assertIs<Expanding>(process.tick)
            assertIs<Optional>(process.tock)
        }

        @Test
        fun `can multi-enclose head of complex subprocesses`() {
            val name = Name.Defined("Complex")
            val builder = Builder.DefinedProcessBuilder(name)
            with(builder) {
                { "a" then "b" or "c" } and "d"
            }
            val process = builder.build().process
            assertIs<Parallel>(process)
            assertIs<Expanding>(process.fore)
            assertIs<Optional>(process.back)
        }

        @Test
        fun `cannot be top-level child of defined process`() {
            assertThrows<DSLParseException> {
                val name = Name.Defined("Complex")
                val builder = Builder.DefinedProcessBuilder(name)
                with(builder) {
                    { "a" then "b" or "c" and "d" }
                }
                val process = builder.build().process
                assertIs<Optional>(process)
            }
        }
    }

    @Nested
    inner class ParseOrdering {
        @Test
        fun `is left-heavy by default`() {
            val name = Name.Defined("MultiSequence")
            val builder = Builder.DefinedProcessBuilder(name)
            with(builder) {
                "a" then "b" then "c" then "d"
            }
            val process = builder.build().process
            assertIs<Sequence>(process)
            val tick = process.tick
            val tock = process.tock
            assertIs<Sequence>(tick)
            assertIs<Expanding>(tock)
            assertEquals("d", tock.obj.toString())

            val ticktick = tick.tick
            val ticktock = tick.tock
            assertIs<Sequence>(ticktick)
            assertIs<Expanding>(ticktock)
            assertEquals("c", ticktock.obj.toString())

            val tickticktick = ticktick.tick
            val tickticktock = ticktick.tock
            assertIs<Expanding>(tickticktick)
            assertIs<Expanding>(tickticktock)
        }

        @Test
        fun `is still left-heavy with appropriate parentheses`() {
            val name = Name.Defined("MultiSequence")
            val builder = Builder.DefinedProcessBuilder(name)
            with(builder) {
                ( ( "a" then "b" ) then "c") then "d"
            }
            val process = builder.build().process
            assertIs<Sequence>(process)
            val tick = process.tick
            val tock = process.tock
            assertIs<Sequence>(tick)
            assertIs<Expanding>(tock)
            assertEquals("d", tock.obj.toString())

            val ticktick = tick.tick
            val ticktock = tick.tock
            assertIs<Sequence>(ticktick)
            assertIs<Expanding>(ticktock)
            assertEquals("c", ticktock.obj.toString())

            val tickticktick = ticktick.tick
            val tickticktock = ticktick.tock
            assertIs<Expanding>(tickticktick)
            assertIs<Expanding>(tickticktock)
        }

        @Test
        fun `is right-heavy with appropriate parentheses`() {
            val name = Name.Defined("MultiSequence")
            val builder = Builder.DefinedProcessBuilder(name)
            with(builder) {
                "a" then ("b"  then ("c" then "d"))
            }
            val process = builder.build().process
            assertIs<Sequence>(process)
            val tick = process.tick
            val tock = process.tock
            assertIs<Expanding>(tick)
            assertIs<Sequence>(tock)
            assertEquals("a", tick.obj.toString())

            val tocktick = tock.tick
            val tocktock = tock.tock
            assertIs<Sequence>(tocktock)
            assertIs<Expanding>(tocktick)
            assertEquals("b", tocktick.obj.toString())

            val tocktocktick = tocktock.tick
            val tocktocktock = tocktock.tock
            assertIs<Expanding>(tocktocktick)
            assertIs<Expanding>(tocktocktock)
        }
    }
}