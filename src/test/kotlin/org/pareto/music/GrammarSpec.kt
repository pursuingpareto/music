package org.pareto.music

import org.pareto.music.Lib.Possible
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
                Note("")
            }
        }
    }

    @Nested
    inner class Optionals {
        @Test
        fun `can be created with terminal subprocess`() {
            Possible(
                Note("a"),
            )
        }

        @Test
        fun `can be created with complex subprocess`() {
            Possible(
                Melody(
                    Melody(
                        Note("a"),
                        Note("b"),
                    ),
                    Melody(
                        Note("c"),
                        Note("d"),
                    ),
                ),
            )
        }

        @Test
        fun `cannot be child of defined process`() {
            val name = Fn.Name("SomeProcess")
            assertThrows<RuntimeException> { Fn.Definition(name, Silence) }
        }
    }

    @Nested
    inner class DefinedProcessesAndReferences {

        private val ab = Melody(Note("a"), Note("b"))

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
            val map = mapOf(definition.name to definition.music)
            val process = map[call.name]
            assertEquals(process, definition.music)
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
            Melody(
                Decision(
                    Harmony(
                        Note("a"),
                        Note("b"),
                    ),
                    Note("c"),
                ),
                Note("d"),
            )
        }

        @Nested
        inner class Sequences {
            @Test
            fun `can have optional steps`() {
                Melody(
                    Possible(
                        Note("a"),
                    ),
                    Note("b"),
                )

                Melody(
                    Note("a"),
                    Possible(
                        Note("b"),
                    ),
                )

                Melody(
                    Possible(
                        Note("a"),
                    ),
                    Possible(
                        Note("b"),
                    ),
                )
            }
        }

        @Nested
        inner class ParallelProcesses {
            @Test
            fun `cannot have two empty layers`() {
                assertThrows<RuntimeException> {
                    Harmony(
                        Silence,
                        Silence
                    )
                }

                Harmony(
                    Note("a"),
                    Silence,
                )

                Harmony(
                    Silence,
                    Note("b"),
                )
            }
        }
    }

    @Nested
    inner class Grammars {
        private val ab = Melody(Note("a"), Note("b"))
        private val cd = Melody(Note("c"), Note("d"))

        @Test
        fun `must contain one or more defined processes`() {
            assertThrows<GrammarValidationException> { Grammar(listOf()) }
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
            assertEquals("[ a ]", Possible(Note("a")).canonical())
            assertEquals("a > b", Melody(Note("a"), Note("b")).canonical())
            assertEquals("a | b", Decision(Note("a"), Note("b")).canonical())
            assertEquals("a & b", Harmony(Note("a"), Note("b")).canonical())

            val p1 = Fn.Definition(
                Fn.Name("Process1"),
                Dimension.Time(
                    Note("a"),
                    Note("b"),
                ),
            )
            assertEquals(
                """
              Process1
                : a > b
                """.trimIndent(),
                p1.canonical(),
            )

            val p2 = Fn.Definition(
                Fn.Name("Process2"),
                Fn.Call(Fn.Name("Process1")),
            )
            assertEquals(
                """
              Process2
                : Process1
                """.trimIndent(),
                p2.canonical(),
            )

            val grammar = Grammar(listOf(p1, p2))
            assertEquals(
                """
              Process1
                : a > b
              
              Process2
                : Process1
                """.trimIndent(),
                grammar.canonical(),
            )
        }

        @Test
        fun `definitions for all references must exist`() {
            todo { }
        }
    }
}
