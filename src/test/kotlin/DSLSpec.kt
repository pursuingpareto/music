import org.example.pg.*
import org.example.pg.Sequence
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DSLSpec {

    private fun fromBuilder(block: GrammarBuilder.FunctionDefinitionBuilder.() -> Unit): Process {
        val builder = builder()
        builder.apply(block)
        return builder.build().process
    }

    private fun builder(): GrammarBuilder.FunctionDefinitionBuilder {
        val name = Fn.Name("SomeName")
        return GrammarBuilder.FunctionDefinitionBuilder(name)
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
            assertIs<Decision>(process.Tock)
        }

        @Test
        fun `terminals are non-PascalCase strings`() {
            val process = fromBuilder { "process_a" then "process_b" } as Sequence
            assertIs<Expanding>(process.Tick)
            assertIs<Expanding>(process.Tock)
        }

        @Test
        fun `references are PascalCase strings`() {
            val process = fromBuilder { "ProcessA" then "ProcessB" } as Sequence
            assertIs<Fn.Call>(process.Tick)
            assertIs<Fn.Call>(process.Tock)
        }

        @Test
        fun `defined processes are names followed by braces enclosing a process`() {
            val grammar = Grammar.compose {
                "DefinedProcessName" {
                    "a" then "b"
                }
            }
            assertEquals(1, grammar.definitions.count())
            assertIs<Fn.Definition>(grammar.definitions.first())
            assertEquals("DefinedProcessName", grammar.definitions.first().name.toString())
        }

        @Test
        fun `grammars can be extended`() {
            val grammar = Grammar.compose {
                "DefinedProcessName" {
                    "a" then "b"
                }
            }
            val extended = grammar.extend {
                "OtherProcessName" {
                    "b" then "c"
                }
            }
            assertEquals(2, extended.definitions.count())
        }
    }

    @Nested
    inner class Emptys {
        @Test
        fun `can be added with empty lambda`() {
            val builder = GrammarBuilder.FunctionDefinitionBuilder(Fn.Name("WithEmptys"))
            with(builder) {
                "" then "b"
            }
            val process = builder.build()
            assertIs<Fn.Definition>(process)
            val sequence = process.process
            assertIs<Sequence>(sequence)
            assertIs<Process.Empty>(sequence.Tick)
        }
    }

    @Nested
    inner class Optionals {
        @Test
        fun `can multi-enclose tail of complex subprocesses`() {
            val name = Fn.Name("Complex")
            val grammarBuilder = GrammarBuilder.FunctionDefinitionBuilder(name)
            with(grammarBuilder) {
                "a" then { "b" or "c" and "d" }
            }
            val process = grammarBuilder.build().process
            assertIs<Sequence>(process)
            assertIs<Expanding>(process.Tick)
            assertIs<Decision>(process.Tock)
        }

        @Test
        fun `can multi-enclose head of complex subprocesses`() {
            val name = Fn.Name("Complex")
            val grammarBuilder = GrammarBuilder.FunctionDefinitionBuilder(name)
            with(grammarBuilder) {
                { "a" then "b" or "c" } and "d"
            }
            val process = grammarBuilder.build().process
            assertIs<Parallel>(process)
            assertIs<Expanding>(process.Back)
            assertIs<Decision>(process.Front)
        }

        @Test
        fun `cannot be top-level child of defined process`() {
            assertThrows<DSLParseException> {
                val name = Fn.Name("Complex")
                val grammarBuilder = GrammarBuilder.FunctionDefinitionBuilder(name)
                with(grammarBuilder) {
                    { "a" then "b" or "c" and "d" }
                }
                val process = grammarBuilder.build().process
                assertIs<Decision>(process)
            }
        }
    }

    @Nested
    inner class ParseOrdering {
        @Test
        fun `is left-heavy by default`() {
            val name = Fn.Name("MultiSequence")
            val grammarBuilder = GrammarBuilder.FunctionDefinitionBuilder(name)
            with(grammarBuilder) {
                "a" then "b" then "c" then "d"
            }
            val process = grammarBuilder.build().process
            assertIs<Sequence>(process)
            val tick = process.Tick
            val tock = process.Tock
            assertIs<Sequence>(tick)
            assertIs<Expanding>(tock)
            assertEquals("d", tock.obj.toString())

            val ticktick = tick.Tick
            val ticktock = tick.Tock
            assertIs<Sequence>(ticktick)
            assertIs<Expanding>(ticktock)
            assertEquals("c", ticktock.obj.toString())

            val tickticktick = ticktick.Tick
            val tickticktock = ticktick.Tock
            assertIs<Expanding>(tickticktick)
            assertIs<Expanding>(tickticktock)
        }

        @Test
        fun `is still left-heavy with appropriate parentheses`() {
            val name = Fn.Name("MultiSequence")
            val grammarBuilder = GrammarBuilder.FunctionDefinitionBuilder(name)
            with(grammarBuilder) {
                ( ( "a" then "b" ) then "c") then "d"
            }
            val process = grammarBuilder.build().process
            assertIs<Sequence>(process)
            val tick = process.Tick
            val tock = process.Tock
            assertIs<Sequence>(tick)
            assertIs<Expanding>(tock)
            assertEquals("d", tock.obj.toString())

            val ticktick = tick.Tick
            val ticktock = tick.Tock
            assertIs<Sequence>(ticktick)
            assertIs<Expanding>(ticktock)
            assertEquals("c", ticktock.obj.toString())

            val tickticktick = ticktick.Tick
            val tickticktock = ticktick.Tock
            assertIs<Expanding>(tickticktick)
            assertIs<Expanding>(tickticktock)
        }

        @Test
        fun `is right-heavy with appropriate parentheses`() {
            val name = Fn.Name("MultiSequence")
            val grammarBuilder = GrammarBuilder.FunctionDefinitionBuilder(name)
            with(grammarBuilder) {
                "a" then ("b"  then ("c" then "d"))
            }
            val process = grammarBuilder.build().process
            assertIs<Sequence>(process)
            val tick = process.Tick
            val tock = process.Tock
            assertIs<Expanding>(tick)
            assertIs<Sequence>(tock)
            assertEquals("a", tick.obj.toString())

            val tocktick = tock.Tick
            val tocktock = tock.Tock
            assertIs<Sequence>(tocktock)
            assertIs<Expanding>(tocktick)
            assertEquals("b", tocktick.obj.toString())

            val tocktocktick = tocktock.Tick
            val tocktocktock = tocktock.Tock
            assertIs<Expanding>(tocktocktick)
            assertIs<Expanding>(tocktocktock)
        }
    }
}