package org.example.pg

typealias OnWord = (Word) -> Any?

typealias Deferred<T> = () -> T

typealias Globals = Map<Fn.Name, Deferred<OnWord>>
typealias Locals = Map<Expanding.Name, Deferred<OnWord>>
typealias ArgMap = Map<Fn.Name, List<Expanding.Name>>

interface RealizationContext {

    fun expanding(name: Expanding.Name): OnWord {
        return { input -> ("$name" == input) || throw NoMatchForInput("$input/$name") }
    }

    fun optional(process: OnWord): OnWord = { word ->
        try {
            process(word)
        } catch (e: UnrunnableProcess) {
            null
        }
    }

    fun decision(a: OnWord, b: OnWord): OnWord = { word: Word ->
        val attempts = listOf(a, b).map {
            try {
                it(word)
            } catch (e: ProcessException) {
                false
            }
        }
        if (attempts.all { it == false }) throw NoMatchForInput(word)
        if (attempts.none { it == false }) throw AmbiguousBranching()
        if (attempts.first() == false) attempts.last() else attempts.first()
    }

    fun sequence(x: OnWord, y: OnWord): OnWord = { word: Word ->
        when (val xw = x(word)) {
            null -> y(word)
            true -> { w: Word -> y(w) }
            else -> sequence(xw as OnWord, y)
        }
    }
}

open class GrammarContext(
    protected val grammar: Grammar
): RealizationContext {


    private val functionArgs: ArgMap = grammar.processes.map { it.name to it.requiredArgs.map { Expanding.Name(it) } }.toMap()
    val functionNamespace: Globals = grammar.processes.map { it.name to { it.process.compile() } }.toMap()

    operator fun invoke(input: Word) {

    }

    fun Process.compile(): OnWord = when(this) {
        is Expanding -> expanding(obj)
        is Optional -> optional(process.compile())
        is Dimension.Choice -> decision(will.compile(), wont.compile())
        is Dimension.Space -> TODO()
        is Dimension.Time -> sequence(tick.compile(), tock.compile())
        is Fn.Call -> reference(name, name.materializeWith(this))
        is Fn.Definition -> throw Error("no definitions here")
    }

    fun reference(name: Fn.Name, replacements: Locals): OnWord {
        return FunctionContext(name, replacements).reference()
    }

    private fun Fn.Name.materializeWith(call: Fn.Call): Locals {
        return functionArgs[this]?.zip(call.params.map { { it.compile() } })?.toMap() ?: throw UnrunnableProcess()
    }

    inner class FunctionContext(
        val name: Fn.Name,
        val locals: Locals,
    ): GrammarContext(grammar) {

        override fun expanding(name: Expanding.Name): OnWord {
           return { input: Word -> locals[name]?.invoke()?.invoke(input) ?: super.expanding(name) }
        }

        fun reference(): OnWord {
            val f = functionNamespace[name]
            if (f == null) throw UnrunnableProcess()
            return { input -> f()(input) }
        }

    }
}

