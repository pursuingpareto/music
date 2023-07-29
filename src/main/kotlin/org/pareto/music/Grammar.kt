package org.pareto.music


/**
 * A collection of [Fn.Definition] processes which together define a language of possible programs.
 */
class Grammar(val definitions: List<Fn.Definition>) {

    init {
        Validate.atLeastOneDefinition(this)
        Validate.uniqueFunctionNames(this)
    }

    /**
     * Creates a new grammar from two existing grammars.
     */
    infix fun extend(other: Grammar) = Grammar(this.definitions + other.definitions)

    /**
     * Produces a canonical, language-agnostic string representation of this grammar.
     */
    fun canonical() = definitions.joinToString(separator = "\n\n") { it.canonical() }

    override fun toString() = canonical()

    companion object
}
