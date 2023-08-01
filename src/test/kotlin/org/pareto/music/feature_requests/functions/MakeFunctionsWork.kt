package org.pareto.music.feature_requests.functions

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.pareto.music.Fn
import org.pareto.music.Grammar
import org.pareto.music.GrammarBuilder
import org.pareto.music.Music
import org.pareto.music.NonTerminal
import org.pareto.music.Note
import org.pareto.music.StdLib
import org.pareto.music.compose
import org.pareto.music.perform.Decider
import org.pareto.music.perform.StringBuilderPerformer
import org.pareto.music.feature_requests.FeatureRequest
import kotlin.test.assertEquals

open class MakeFunctionsWork : FeatureRequest() {

    @Nested
    inner class WithPositionalAndNamedArgs {

        @Nested
        inner class ArgumentRealization {

            @Test
            fun `occurs before function realization`() = feature {

                val out = StringBuilderPerformer(
                    randomLetters16Context.grammar(),
                    Decider.UniformRandom
                ).play("Test")

                assertEquals(1, out.split(" ").toSet().size)
            }

            @Test
            fun `interprets overcalled functions as uncurrying`() = feature {}
        }

        @Nested
        inner class PositionalArgs {

            @Nested
            inner class AreDefined {

                @Test
                fun `with list of non-terminal names`() = feature {
                    assertDoesNotThrow {
                        val g = Grammar.compose {
                            "F"("a", "b", "c") {
                                "a" then "b" then "c"
                            }
                        }
                        assertEquals(1, g.definitions.size - StdLib.StandardGrammar.definitions.size)
                        assertEquals(listOf("a", "b", "c"), g.definitions.last().requiredArgs.map { it.value }) } }
            }

            @Nested
            inner class AreRealized {

                @Test
                fun `with list of Music`() = feature {
                    val g = Grammar.compose {
                        "F"("a", "b", "c") {
                            "a" then "b" then "c" } }
                    val f = g.definitions.last()
                    assertEquals("F", f.name.value)
                    throw NotImplementedError("""
                        val called = f(Note("x"), Note("y"), Note("z"))
                        assertEquals(listOf("x", "y", "z"), called.params.map { it.toString() } )
                    """.trimIndent())
                }

                @Nested
                inner class WithListOfMusic {

                    @Nested
                    inner class WhenMusicIs {

                        @Nested
                        inner class Note {

                            @Test
                            fun `note is passed in directly by default`() = feature {}

                            @Test
                            fun `note is replaced with positional arg when note name is param`() = feature { }

                            @Test
                            fun `note is replaced by consulting appropriate lookup when named args passed in`() = feature {}
                        }

                        @Nested
                        inner class FunctionCall {

                            @Test
                            fun `are realized from inside out`() = feature {}
                        }

                        @Nested
                        inner class Dimension // TODO

                        @Nested
                        inner class Silence // TODO
                    }
                }

                @Test
                fun `from inside out when nested`() = feature {}
            }

            @Test
            fun `are realized by passing list of Music elements`() =
                feature {}

            @Test
            fun `are required when calling`() =
                feature {}

            @Test
            fun `are interpreted as overcall at callsites when a matching named arg is also included`() =
                feature {}
        }

        @Nested
        inner class NamedArgs {

            @Test
            fun `as`() = feature {}
        }
    }

    // TODO - remove this and go through the standard DSL
    open class Context(val namespace: MutableMap<Fn.Name, Fn.Definition>) {

        operator fun get(name: String) = namespace[Fn.Name(name)]?.music ?: error("illegal")

        infix fun def(name: String) = ArgBuilder(Fn.Name(name))

        fun grammar(): Grammar = Grammar(namespace.values.toList())

        @Suppress("unused")
        inner class ArgBuilder(private val name: Fn.Name) {
            private var requiredArgs: List<Note.Name>? = null
            private var namedArgs: Map<Note.Name, Music>? = null
            private var body: Music? = null

            operator fun invoke(
                vararg required: String,
                block: GrammarBuilder.FunctionDefinitionBuilder.() -> Unit
            ): Context {
                requiredArgs = required
                    .map { Note.Name(it) }
                body = GrammarBuilder
                    .FunctionDefinitionBuilder(name, requiredArgs)
                    .apply(block)
                    .build()
                    .music
                build()
                return this@Context
            }


            private fun build(): Fn.Definition {
                val thisBody = body as? NonTerminal ?: error("bad")
                val args = requiredArgs ?: listOf()
                return Fn.Definition(name, thisBody, args).also { namespace[name] = it }
            }
        }
    }

    protected class EmptyContext : Context(mutableMapOf())

    val randomLetters16Context = with(EmptyContext()) {
        def("Do2")("x") {
            "x" then "x"
        }

        def("Do4")("x") {
            "Do2"("x") then "Do2"("x")
        }

        def("Do8")("x") {
            "Do4"("x") then "Do4"("x")
        }

        def("Do16")("x") {
            "Do8"("x") then "Do8"("x")
        }

        def("RandomLetter")() {
            "a" or "b" or "c" or "d"
        }

        def("Test")() {
            "Do16"("RandomLetter")
        }
    }
}

