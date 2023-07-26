package org.pareto.music

/**
 * A collection of [Fn.Definition] processes which together define a language of possible programs.
 */
class Grammar(val definitions: List<Fn.Definition>) {

    init { validateGrammar(definitions) }

    /**
     * Creates a new grammar from two existing grammars.
     */
    infix fun extend(other: Grammar) = Grammar(this.definitions + other.definitions)

    /**
     * Produces a canonical, language-agnostic string representation of this grammar.
     */
    fun canonical() = definitions.joinToString(separator = "\n\n") { it.canonical() }

    override fun toString() = canonical()

    companion object {

        fun validateGrammar(components: List<Fn.Definition>) {
            validateAtLeastOneComponent(components)
            validateUniqueNames(components)
        }

        private fun validateAtLeastOneComponent(components: List<Fn.Definition>) {
            if (components.isEmpty()) throw GrammarValidationException("cannot create grammar with zero processes")
        }

        private fun validateUniqueNames(components: List<Fn.Definition>) {
            fun message(name: ProcessName, count: Int) = "$name defined $count times"
            val errors = components
                .map { it.name }
                .groupingBy { it }
                .eachCount()
                .filter { (_, count) -> count > 1 }
                .map { (name, count) -> message(name, count) }
            if (errors.isNotEmpty()) {
                throw GrammarValidationException("Defined process names must be unique: ${errors.joinToString(separator = ", ")}")
            }
        }
    }
}
