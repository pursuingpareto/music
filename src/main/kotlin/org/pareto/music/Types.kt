@file:Suppress("Unused")

package org.pareto.music

import org.pareto.music.canon.canonical

/**
 * We separate [String]s into [Text.PascalCase]s and [Text.nonPascalCase]s.
 */
sealed interface Text {

    val value: String

    @JvmInline
    /**
     * [PascalCase] [Text]s start with a capital letter and contain no underscores or special characters.
     */
    value class PascalCase(override val value: String) : Text {
        init { require(value.isPascalCase()) }
    }

    @Suppress("ClassName")
    @JvmInline
    /**
     * [nonPascalCase] [Text]s are anything that is not [PascalCase]
     */
    value class nonPascalCase(override val value: String) : Text {
        init {
            require(!value.isPascalCase())
            require(value.isNotEmpty())
        }
    }

    companion object {
        fun from(string: String): Text =
            if (string.isPascalCase()) PascalCase(string) else nonPascalCase(string)
    }
}


/**
 * A name for music. A [Fn.Name] is PascalCase and used for [Fn]'s.
 * An [Note.Name] is used for terminal [Note] processes.
 */
sealed class MusicName(val text: Text) {

    val value: String
        get() = text.value

    companion object {
        fun from(string: String): MusicName = when(val word = string.toWord()) {
            is Text.PascalCase -> Fn.Name(word)
            is Text.nonPascalCase -> Note.Name(word)
        }
    }

    override fun toString() = text.value

    override fun hashCode() = text.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MusicName) return false
        if (text != other.text) return false
        return true
    }
}


/**
 * A collection of [Fn.Definition] processes which together define a language of possible programs.
 */
class Grammar(val definitions: List<Fn.Definition>) {

    init {
        Validate.atLeastOneDefinition(this)
        Validate.uniqueFunctionNames(this)
        Validate.allFunctionCallsHaveDefinitions(this)
    }

    /**
     * Creates a new grammar from two existing grammars.
     */
    infix fun extend(newDefinitions: List<Fn.Definition>) = Grammar(this.definitions + newDefinitions)

    /**
     * Produces a canonical, language-agnostic string representation of this grammar.
     */
    fun canonical() = definitions.joinToString(separator = "\n\n") { it.canonical() }

    override fun toString() = canonical()

    companion object
}


//region Exceptions
typealias GrammarValidationException = Mistake.GrammarValidationException
typealias CouldNotCompile = Mistake.CouldNotCompile
typealias DSLParseException = Mistake.DSLParseException
typealias InvalidInput = Mistake.InvalidInput
typealias ProcessExhausted = Unrunnable.ProcessExhausted
typealias AmbiguousBranching = Unrunnable.AmbiguousBranching
typealias FunctionNotDefined = Unrunnable.FunctionNotDefined
typealias NoMatchForInput = Unrunnable.NoMatchForInput

/**
 * Base exception for org.pareto.music
 */
sealed class Mistake(
    message: String? = null,
    cause: Throwable? = null) : Exception(message, cause) {

    /**
     * Thrown immediately after a [Grammar] is constructed.
     */
    class GrammarValidationException(
        message: String? = null,
        cause: Throwable? = null) : Mistake(message, cause)

    /**
     * Catch-all for compilation errors.
     */
    class CouldNotCompile(
        message: String? = null,
        cause: Throwable? = null) : Mistake(message, cause)

    /**
     * A DSLParseException is only thrown at grammar-definition time.
     */
    class DSLParseException(
        message: String? = null,
        cause: Throwable? = null) : Mistake(message, cause)

    /**
     * Thrown at runtime when invalid input provided.
     */
    class InvalidInput(
        message: String? = null,
        cause: Throwable? = null) : Mistake(message, cause)
}


/**
 * [Unrunnable] and its subclasses are thrown *after* a [Grammar] has been successfully
 * defined.
 */
sealed class Unrunnable(
    message: String? = null,
    cause: Throwable? = null) : Mistake(message, cause) {

    /**
     * Thrown when caller attempts to continue running an already-completed process.
     */
    class ProcessExhausted(
        message: String? = null,
        cause: Throwable? = null) : Unrunnable(message, cause)

    /**
     * Thrown at runtime when evaluating a [Decision] that cannot be unambiguously resolved
     * with the provided input.
     */
    class AmbiguousBranching(
        message: String? = null,
        cause: Throwable? = null) : Unrunnable(message, cause)

    /**
     * Thrown whenever a function definition does not exist.
     */
    class FunctionNotDefined(
        message: String?,
        cause: Throwable? = null) : Unrunnable(message, cause) {
        constructor(functionName: Fn.Name) : this("Function ${functionName.value} is not defined.")
    }

    /**
     * Thrown when validating a [Grammar] when the provided input does not match that grammar.
     */
    class NoMatchForInput(
        message: String? = null,
        cause: Throwable? = null) : Unrunnable(message, cause) {
        constructor(word: Text) : this(message = "No way to transition to $word")
        }
    }
//endregion


/**
 * A namespace that maps [Fn.Name] to some type T
 */
typealias FunctionNamespace<T> = Map<Fn.Name, T>


/**
 * Reserved [Music] keywords
 */
object Keyword {
    const val END = "END"
}

private fun String.toWord(): Text = Text.from(this)

private fun String.isPascalCase() = "^([A-Z][a-z0-9]*)+$".toRegex().containsMatchIn(this)