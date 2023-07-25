package org.example.pg

typealias OnWord = (Word) -> Any?

typealias Deferred<T> = () -> T

typealias Globals = Map<Fn.Name, Deferred<OnWord>>
typealias Locals = Map<Note.Name, Deferred<OnWord>>
typealias ArgMap = Map<Fn.Name, List<Note.Name>>

interface Context {

    fun expanding(name: Note.Name): OnWord {
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

    @Suppress("UNCHECKED_CAST")
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
): Context {

    private val functionArgs: ArgMap = grammar.definitions.map { it.name to it.requiredArgs.map { Note.Name(it) } }.toMap()

    val functionNamespace: Globals = grammar.definitions.map { it.name to { it.process.compile() } }.toMap()

    private fun Process.compile(): OnWord = when(this) {
        is Note -> expanding(obj)
        is Optional -> optional(process.compile())
        is Dimension.Choice -> decision(Will.compile(), Wont.compile())
        is Dimension.Space -> throw NotImplementedError("Parallel processes not yet supported")
        is Dimension.Time -> sequence(Tick.compile(), Tock.compile())
        is Fn.Call -> reference(name, name.materializeWith(this))
        is Fn.Definition -> throw Error("Definitions are not directly compiled")
    }

    private fun reference(name: Fn.Name, replacements: Locals): OnWord {
        return FunctionContext(name, replacements).reference()
    }

    private fun Fn.Name.materializeWith(call: Fn.Call): Locals {
        return functionArgs[this]?.zip(call.params.map { { it.compile() } })?.toMap()
            ?: throw UnrunnableProcess("could not materialize args for ${call.name}")
    }

    inner class FunctionContext(
        val name: Fn.Name,
        val locals: Locals,
    ): GrammarContext(grammar) {

        override fun expanding(name: Note.Name): OnWord {
            val f = locals[name]
            return if (f == null) { super.expanding(name) }
            else { input: Word -> f()(input) }
        }

        fun reference(): OnWord {
            val f = functionNamespace[name]
            if (f == null) throw UnrunnableProcess()
            return { input -> f()(input) }
        }

    }
}

