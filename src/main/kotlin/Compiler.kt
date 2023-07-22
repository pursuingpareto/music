package org.example.pg

object Expanders {
    val equality: Expander = {
        ("$this" == it) || throw NoMatchForInput(it)}
}

/**
 * Compiles a [Grammar] into a runnable [Program].
 *
 * Compilation produces a [Program] by converting each [Fn.Definition] process into a
 * single function of type [OnWord].
 *
 * Each of these functions can be called by passing it a word
 */
@Suppress("UNCHECKED_CAST")
class Compiler(private val expander: Expander = Expanders.equality)  {

    private val namespace: MutableNamespace = mutableMapOf()

    fun compile(grammar: Grammar): Program {
        namespace.clear()
        grammar.processes.forEach { it.functionalize() }
        return Program(namespace)
    }

    private fun expanding(obj: Expanding.Name): OnWord = {
        word -> expander(obj, word) }

    private fun optional(process: OnWord): OnWord = {
        word -> try { process(word) }
                catch (e: UnrunnableProcess) { null } }

    private fun decision(a: OnWord, b: OnWord) = { word: Word ->
        val attempts = listOf(a, b).map {
            try { it(word) }
            catch (e: ProcessException) { false } }
        if (attempts.all  { it == false }) throw NoMatchForInput(word)
        if (attempts.none { it == false }) throw AmbiguousBranching()
        if (attempts.first() == false) attempts.last() else attempts.first() }

    private fun reference(name: Fn.Name, params: List<Param>) = { word: Word ->
        this.namespace[name]?.invoke(word) ?: throw NoMatchForInput(word) }

    private fun sequence(x: OnWord, y: OnWord): OnWord = { word: Word ->
        when (val xw = x(word)) {
            null -> y(word)
            true -> { w: Word -> y(w) }
            else -> sequence(xw as OnWord, y) } }

    private fun named(
        onWord: OnWord,
        name: Fn.Name
    ): OnWord {
        this.namespace[name] = onWord
        return { word: Word -> onWord(word) } }

    private fun Process.functionalize(
        params: Map<Expanding.Name, Param> = mapOf()
    ): OnWord = when (this) {
        is Dimension.Time   -> sequence(tick.functionalize(params), tock.functionalize(params))
        is Dimension.Choice -> decision(will.functionalize(params), wont.functionalize(params))
        is Optional         -> optional(process.functionalize(params))
        is Expanding        -> params[obj]?.functionalize(params) ?: expanding(obj)
        is Fn.Definition    -> named(process.functionalize(params), name)
        is Fn.Call          -> reference(referencedName, this.params)
        else                -> throw UnrunnableProcess("not supported ${this}!") }
}