package org.pareto.music

/**
 * Handles validation of various types in this package.
 */
object Validate {

    //region Grammar validation
    fun atLeastOneDefinition(grammar: Grammar) {
        if (grammar.definitions.isEmpty()) throw GrammarValidationException("cannot create grammar with zero processes")
    }

    fun uniqueFunctionNames(grammar: Grammar) {
        fun message(name: MusicName, count: Int) = "$name defined $count times"
        val errors = grammar.definitions
            .map { it.name }
            .groupingBy { it }
            .eachCount()
            .filter { (_, count) -> count > 1 }
            .map { (name, count) -> message(name, count) }
        if (errors.isNotEmpty()) {
            throw GrammarValidationException("Defined process names must be unique: ${errors.joinToString(separator = ", ")}")
        }
    }

    fun allFunctionCallsHaveDefinitions(grammar: Grammar) {
        val definitionNames = grammar.definitions.map { it.name }.toSet()
        val undefined = grammar.definitions.flatMap { def ->
            def.getDescendents { (it is Fn.Call) && !definitionNames.contains(it.name)} }
            .filterIsInstance<Fn.Call>()
            .map { it.name }
        if (undefined.isNotEmpty()) throw GrammarValidationException("The following functions were called but never defined: ${
            undefined.joinToString(", " ){ it.value }}")
    }
    //endregion


    //region Fn.Definition validation
    fun argsAreUsedInBody(definition: Fn.Definition) {
        val unaccounted =
            definition.requiredArgs.toSet() - definition.music.getDescendents { it is Expandable }
            .filterIsInstance<Expandable>()
            .map { it.name }
            .toSet()

        if (unaccounted.isNotEmpty()) throw DSLParseException(
            "Unused arguments in call to ${definition.name}: ${unaccounted.joinToString(", ")}")
    }
    //endregion


    //region Helpers
    private fun Music?.getDescendents(where: (Music?) -> Boolean): List<Music?> = when (this) {
        null -> listOf()
        is Dimension -> {
            val descendents = mutableListOf(Left, Right).filter(where).toMutableList()
            descendents.addAll(Left.getDescendents(where))
            descendents.addAll(Right.getDescendents(where))
            descendents
        }
        is Fn.Call -> {
            val descendents = mutableListOf<Music?>()
            if (where(this)) descendents.add(this)
            this.params.forEach {
                when(it) {
                    is Note -> if (where(it)) descendents.add(it)
                    else -> descendents.addAll(it.getDescendents(where) ) } }
            descendents
        }
        is Fn.Definition -> this.music.getDescendents(where)
        is Note -> if (where(this)) listOf(this) else listOf()
    }
    //endregion
}

