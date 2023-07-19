package org.example.pg

object Expanders {
    val equality: Expander = {
        ("$this" == it) || throw UnrunnableProcess()}
}

/**
 * Compiles a [Grammar] into a [Program] with an [Expander]
 */
@Suppress("UNCHECKED_CAST")
class Compiler(private val expander: Expander = Expanders.equality)  {

    private val namespace: Namespace = mutableMapOf()

    fun compile(grammar: Grammar): Program {
        grammar.processes.associate { it.name to functionFrom(it) }
        return Program(namespace)
    }

    private fun expanding(obj: Name.Expanding): OnWord = { word -> expander(obj, word)}

    private fun optional(process: OnWord) = { word: String ->
        try { process(word) } catch (e: UnrunnableProcess) { null } }

    private fun decision(a: OnWord, b: OnWord) = { word: String ->
        a(word) ?: b(word) }

    private fun ref(name: Name.Defined) = { word: String ->
        this.namespace[name]?.invoke(word) }

    private fun sequence(x: OnWord, y: OnWord): OnWord =   { word: String ->
        when (val xw = x(word)) {
            null -> y(word)
            true -> { w: String -> y(w) }
            false -> throw UnrunnableProcess()
            else -> sequence(xw as OnWord, y)
        } }

    private fun named(p: OnWord, name: Name.Defined): OnWord {
        this.namespace[name] = p
        return { word: String -> p(word) } }

    private fun functionFrom(process: Process): OnWord {
        return when (process) {
            is Dimension.Time   -> sequence(functionFrom(process.tick), functionFrom(process.tock))
            is Dimension.Choice -> decision(functionFrom(process.left), functionFrom(process.right))
            is Optional         -> optional(functionFrom(process.process))
            is Expanding        -> expanding(process.obj)
            is Defined          -> named(functionFrom(process.process), process.name)
            is Reference        -> ref(process.referencedName)
            else                -> throw UnrunnableProcess("not supported ${process}!")
        }
    }
}