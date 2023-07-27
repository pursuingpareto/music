package org.pareto.music.compiler.thread_validator

import org.pareto.music.Fn
import org.pareto.music.Grammar
import org.pareto.music.Keyword
import org.pareto.music.Namespace
import org.pareto.music.NoMatchForInput
import org.pareto.music.Note
import org.pareto.music.ProcessExhausted
import org.pareto.music.Word

/**
 * A [Program] is compiled from a [Grammar] with a [Context].
 *
 * Once compiled, a program can be run by passing it strings. The first word you pass it
 * will be a PascalCase word that matches the [Fn.Name] of one of the functions in the grammar.
 * Subsequent words will be non-PascalCase strings which correspond to the [Note.Name]'s in
 * your grammar.
 *
 * When words can be unambiguously assigned.
 */
class Program(private val namespace: Namespace) {

    /**
     * [f] is a function which gets called and reassigned every time
     * a [Program] is [invoke]d
     */
    private var f: OnWord = { word -> begin(Fn.Name(word)) }

    @Suppress("UNCHECKED_CAST")
    operator fun invoke(word: Word): Program = this
        .apply {
            f = when (val fw = this.f(word)) {
                true -> { w: Word -> if (w == Keyword.END) true else throw ProcessExhausted() }
                null -> throw NoMatchForInput(word)
                else -> fw as OnWord
            }
        }

    private fun begin(name: Fn.Name) = namespace[name]?.also { f = it }
        ?: throw NoMatchForInput(name.toString())

    companion object {

        fun from(grammar: Grammar) = Program(
            GrammarContext(grammar).functionNamespace.mapValues { it.value() },
        )
    }
}
