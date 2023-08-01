@file:Suppress("Unused")

package org.pareto.music

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
        return text == other.text
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
open class ProcessException(message: String? = null, cause: Throwable? = null) : Exception(message, cause)

class GrammarValidationException(message: String? = null, cause: Throwable? = null) : ProcessException(message, cause)

open class UnrunnableProcess(message: String? = null, cause: Throwable? = null) : ProcessException(message, cause)

class ProcessExhausted(message: String? = null, cause: Throwable? = null) : UnrunnableProcess(message, cause)

class AmbiguousBranching(message: String? = null, cause: Throwable? = null) : UnrunnableProcess(message, cause)

class NoMatchForInput(message: String? = null, cause: Throwable? = null) : UnrunnableProcess(message, cause) {
    constructor(word: Text) : this(message = "No way to transition to $word")
}

class DSLParseException(message: String? = null, cause: Throwable? = null) : ProcessException(message, cause)
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


/**
 * Compiles [Music] to [T]. Offers runtime performance improvements by pre-compiling [Music].
 *
 * Use a [Compiler] and a [Runner] (or just a [CompilerRunner])
 * instead of an [Interpreter] when you know the full [Grammar] in advance and don't need to inject [Music] into the program at runtime.
 *
 * @param T the type of the compilation artifact.
 */
fun interface Compiler<T> {
    fun compile(music: Music): T
}


/**
 * Interprets [Music] as [T]. This is an on-the-fly alternative to using a [Compiler] and a [Runner].
 *
 * Use an [Interpreter] instead of a [Compiler] when you don't know the full [Grammar] that you're running in advance.
 *
 * @param T the output type of the interpreter. May often be ignored if interpretation is intended to work via side effects.
 */
fun interface Interpreter<T> {
    fun interpret(music: Music): T
}


/**
 * Converts [Music] to [Music]!
 */
fun interface Transposer {
    fun transpose(music: Music): Music
}


/**
 * Runs [Music] and produces [T]
 */
fun interface Runner<T> {
    suspend fun run(music: Music): T
}


/**
 * A [Runner<T>] that works by first [compile]ing [Music] to an intermediate artifact of type [C].
 */
interface CompilerRunner<C, T> : Runner<T>, Compiler<C> {

    suspend fun run(compiled: C): T

    override suspend fun run(music: Music): T = run(compile(music))
}


private fun String.toWord(): Text = Text.from(this)

private fun String.isPascalCase() = "^([A-Z][a-z0-9]*)+$".toRegex().containsMatchIn(this)