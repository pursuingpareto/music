package org.pareto.music.rehearse.thread_validator

import org.pareto.music.Fn
import org.pareto.music.Grammar
import org.pareto.music.Keyword
import org.pareto.music.FunctionNamespace
import org.pareto.music.NoMatchForInput
import org.pareto.music.Note
import org.pareto.music.ProcessExhausted
import org.pareto.music.Text

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
class Program(private val namespace: FunctionNamespace<OnWord>) {

    /**
     * [f] is a function which gets called and reassigned every time
     * a [Program] is [invoke]d
     */
    private var f: OnWord = { word ->
        if (word is Text.PascalCase) begin(Fn.Name(word))
        else throw NoMatchForInput("First invoking must be with PascalCase term.")}

    @Suppress("UNCHECKED_CAST")
    operator fun invoke(word: Text): Program = this
        .apply {
            f = when (val fw = this.f(word)) {
                true -> { w: Text -> if (w.value == Keyword.END) true else throw ProcessExhausted() }
                null -> throw NoMatchForInput(word)
                else -> fw as OnWord
            }
        }

    operator fun invoke(s: String): Program = invoke(Text.from(s))

    private fun begin(name: Fn.Name) = namespace[name]?.also { f = it }
        ?: throw NoMatchForInput(name.toString())

    companion object {

        fun from(grammar: Grammar) = Program(
            GrammarContext(grammar).functionNamespace.mapValues { it.value() },
        )
    }
}
