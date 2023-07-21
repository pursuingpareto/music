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
            assertThrows<RuntimeException> { Expanding("PascalCase") }
        }

        @Test
        fun `can be created with non-PascalCase object`() {
            Expanding("nonPascalCase")
            Expanding("snake_case")
        }

        @Test
        fun `can be created with single character object`() {
            Expanding("a")
            Expanding("z")
        }

        @Test
        fun `cannot be created with empty string object`() {
            assertThrows<RuntimeException> {
                Expanding("") }
        }
    }

    @Nested
    inner class Optionals {
        @Test
        fun `can be created with terminal subprocess`() {
            Optional(
                Expanding("a"))
        }

        @Test
        fun `can be created with complex subprocess`() {
            Optional(
                Sequence(
                    Sequence(
                        Expanding("a"),
                        Expanding("b")),
                    Sequence(
                        Expanding("c"),
                        Expanding("d"))
            ))
        }

        @Test
        fun `cannot contain optionals`() {
            assertThrows<RuntimeException> {
                Optional(
                    Optional(
                        Expanding("a"))) }
        }

        @Test
        fun `cannot be child of defined process`() {
            val name = Name.Defined("SomeProcess")
            val a = Expanding("a")
            assertThrows<RuntimeException> { Function(name, Optional(a)) }
        }
    }

    @Nested
    inner class DefinedProcessesAndReferences {

        private val ab = Sequence(Expanding("a"), Expanding("b"))
        @Test
        fun `can compare names`() {
            val reference = Reference(Name.Defined("A"))
            val function = Function(Name.Defined("A"), ab)
            assertEquals(reference.referencedName, function.name)
        }

        @Test
        fun `can access names via Maps`() {
            val reference = Reference(Name.Defined("A"))
            val function = Function(Name.Defined("A"), ab)
            val map = mapOf(function.name to function.process)
            val process = map[reference.referencedName]
            assertEquals(process, function.process)
        }

        @Test
        fun `defined processes cannot contain single expanding process`() {
            assertThrows<RuntimeException> { Function(Name.Defined("A"), Expanding("foo")) }
        }
    }

    @Nested
    inner class BinaryProcesses {
        @Test
        fun `can contain other binary processes`() {
            Sequence(
                Decision(
                    Parallel(
                        Expanding("a"),
                        Expanding("b")),
                    Expanding("c")),
                Expanding("d"))
        }

        @Nested
        inner class Sequences {
            @Test
            fun `can have optional steps`() {
                Sequence(
                    Optional(
                        Expanding("a")),
                    Expanding("b"))

                Sequence(
                    Expanding("a"),
                    Optional(
                        Expanding("b")))

                Sequence(
                    Optional(
                        Expanding("a")),
                    Optional(
                        Expanding("b")))
            }
        }

        @Nested
        inner class Decisions {
            @Test
            fun `cannot have optional branches`() {
                assertThrows<RuntimeException> {
                    Decision(
                        Optional(
                            Expanding("a")),
                        Expanding("b")) }

                assertThrows<RuntimeException> {
                    Decision(
                        Expanding("a"),
                        Optional(
                            Expanding("b"))) }
            }
        }

        @Nested
        inner class ParallelProcesses {
            @Test
            fun `cannot have both layers optional`() {
                assertThrows<RuntimeException> {
                    Parallel(
                        Optional(
                            Expanding("a")),
                        Optional(
                            Expanding("b"))) }

                Parallel(
                    Expanding("a"),
                    Optional(
                        Expanding("b")))

                Parallel(
                    Optional(
                        Expanding("a")),
                    Expanding("b"))
            }
        }
    }

    @Nested
    inner class Grammars {
        private val ab = Sequence(Expanding("a"), Expanding("b"))
        private val cd = Sequence(Expanding("c"), Expanding("d"))

        @Test
        fun `must contain one or more defined processes`() {
            assertThrows<GrammarValidationException> { Grammar(listOf())  }
        }

        @Test
        fun `defined process names must be unique`() {
            val name = Name.Defined("Example")
            val d1 = Function(name, ab)
            val d2 = Function(name, cd)
            assertThrows<GrammarValidationException> { Grammar(listOf(d1, d2)) }
        }

        @Test
        fun `can be extended with other grammars`() {
            val g1 = Grammar(listOf(Function(Name.Defined("Foo"), ab)))
            val g2 = Grammar(listOf(Function(Name.Defined("Bar"), cd)))
            val g3 = g1 extend g2
            assertEquals(2, g3.processes.count())
        }

        @Test
        fun `have a kotlin-agnostic string representation`() {
            assertEquals("abc", Expanding("abc").canonical())
            assertEquals("[ a ]", Optional(Expanding("a")).canonical())
            assertEquals("a > b", Sequence(Expanding("a"), Expanding("b")).canonical())
            assertEquals("a | b", Decision(Expanding("a"), Expanding("b")).canonical())
            assertEquals("a & b", Parallel(Expanding("a"), Expanding("b")).canonical())

            val p1 = Function(
                Name.Defined("Process1"),
                Dimension.Time(
                    Expanding("a"),
                    Expanding("b")))
            assertEquals("""
              Process1
                : a > b
            """.trimIndent(), p1.canonical())

            val p2 = Function(
                Name.Defined("Process2"),
                Reference(Name.Defined("Process1")))
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