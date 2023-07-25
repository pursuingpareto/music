import org.example.pg.*
import org.example.pg.Sequence
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.todo

class GrammarSpec {
    @Nested
    inner class Terminals {
        @Test
        fun `cannot be created with PascalCase object`() {
            assertThrows<RuntimeException> { Note("PascalCase") }
        }

        @Test
        fun `can be created with non-PascalCase object`() {
            Note("nonPascalCase")
            Note("snake_case")
        }

        @Test
        fun `can be created with single character object`() {
            Note("a")
            Note("z")
        }

        @Test
        fun `cannot be created with empty string object`() {
            assertThrows<RuntimeException> {
                Note("") }
        }
    }

    @Nested
    inner class Optionals {
        @Test
        fun `can be created with terminal subprocess`() {
            Optional(
                Note("a"))
        }

        @Test
        fun `can be created with complex subprocess`() {
            Optional(
                Sequence(
                    Sequence(
                        Note("a"),
                        Note("b")),
                    Sequence(
                        Note("c"),
                        Note("d"))
            ))
        }

        @Test
        fun `cannot contain optionals`() {
            assertThrows<RuntimeException> {
                Optional(
                    Optional(
                        Note("a"))) }
        }

        @Test
        fun `cannot be child of defined process`() {
            val name = Fn.Name("SomeProcess")
            val a = Note("a")
            assertThrows<RuntimeException> { Fn.Definition(name, Optional(a)) }
        }
    }

    @Nested
    inner class DefinedProcessesAndReferences {

        private val ab = Sequence(Note("a"), Note("b"))
        @Test
        fun `can compare names`() {
            val call = Fn.Call(Fn.Name("A"))
            val definition = Fn.Definition(Fn.Name("A"), ab)
            assertEquals(call.name, definition.name)
        }

        @Test
        fun `can access names via Maps`() {
            val call = Fn.Call(Fn.Name("A"))
            val definition = Fn.Definition(Fn.Name("A"), ab)
            val map = mapOf(definition.name to definition.process)
            val process = map[call.name]
            assertEquals(process, definition.process)
        }

        @Test
        fun `defined processes cannot contain single expanding process`() {
            assertThrows<RuntimeException> { Fn.Definition(Fn.Name("A"), Note("foo")) }
        }
    }

    @Nested
    inner class BinaryProcesses {
        @Test
        fun `can contain other binary processes`() {
            Sequence(
                Decision(
                    Parallel(
                        Note("a"),
                        Note("b")),
                    Note("c")),
                Note("d"))
        }

        @Nested
        inner class Sequences {
            @Test
            fun `can have optional steps`() {
                Sequence(
                    Optional(
                        Note("a")),
                    Note("b"))

                Sequence(
                    Note("a"),
                    Optional(
                        Note("b")))

                Sequence(
                    Optional(
                        Note("a")),
                    Optional(
                        Note("b")))
            }
        }

        @Nested
        inner class Decisions {
            @Test
            fun `cannot have optional branches`() {
                assertThrows<RuntimeException> {
                    Decision(
                        Optional(
                            Note("a")),
                        Note("b")) }

                assertThrows<RuntimeException> {
                    Decision(
                        Note("a"),
                        Optional(
                            Note("b"))) }
            }
        }

        @Nested
        inner class ParallelProcesses {
            @Test
            fun `cannot have both layers optional`() {
                assertThrows<RuntimeException> {
                    Parallel(
                        Optional(
                            Note("a")),
                        Optional(
                            Note("b"))) }

                Parallel(
                    Note("a"),
                    Optional(
                        Note("b")))

                Parallel(
                    Optional(
                        Note("a")),
                    Note("b"))
            }
        }
    }

    @Nested
    inner class Grammars {
        private val ab = Sequence(Note("a"), Note("b"))
        private val cd = Sequence(Note("c"), Note("d"))

        @Test
        fun `must contain one or more defined processes`() {
            assertThrows<GrammarValidationException> { Grammar(listOf())  }
        }

        @Test
        fun `defined process names must be unique`() {
            val name = Fn.Name("Example")
            val d1 = Fn.Definition(name, ab)
            val d2 = Fn.Definition(name, cd)
            assertThrows<GrammarValidationException> { Grammar(listOf(d1, d2)) }
        }

        @Test
        fun `can be extended with other grammars`() {
            val g1 = Grammar(listOf(Fn.Definition(Fn.Name("Foo"), ab)))
            val g2 = Grammar(listOf(Fn.Definition(Fn.Name("Bar"), cd)))
            val g3 = g1 extend g2
            assertEquals(2, g3.definitions.count())
        }

        @Test
        fun `have a kotlin-agnostic string representation`() = todo {
            assertEquals("abc", Note("abc").canonical())
            assertEquals("[ a ]", Optional(Note("a")).canonical())
            assertEquals("a > b", Sequence(Note("a"), Note("b")).canonical())
            assertEquals("a | b", Decision(Note("a"), Note("b")).canonical())
            assertEquals("a & b", Parallel(Note("a"), Note("b")).canonical())

            val p1 = Fn.Definition(
                Fn.Name("Process1"),
                Dimension.Time(
                    Note("a"),
                    Note("b")))
            assertEquals("""
              Process1
                : a > b
            """.trimIndent(), p1.canonical())

            val p2 = Fn.Definition(
                Fn.Name("Process2"),
                Fn.Call(Fn.Name("Process1")))
            assertEquals("""
              Process2
                : Process1
            """.trimIndent(), p2.canonical())

            val grammar = Grammar(listOf(p1, p2))
            assertEquals("""
              Process1
                : a > b
              
              Process2
                : Process1
            """.trimIndent(), grammar.canonical())
        }

        @Test
        fun `definitions for all references must exist`() {
            todo { }
        }
    }
}