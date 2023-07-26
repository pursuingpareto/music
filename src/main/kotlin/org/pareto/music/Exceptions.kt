package org.pareto.music

open class ProcessException(message: String? = null, cause: Throwable? = null) : Exception(message, cause)

class GrammarValidationException(message: String? = null, cause: Throwable? = null) : ProcessException(message, cause)

open class UnrunnableProcess(message: String? = null, cause: Throwable? = null) : ProcessException(message, cause)

class ProcessExhausted(message: String? = null, cause: Throwable? = null) : UnrunnableProcess(message, cause)

class AmbiguousBranching(message: String? = null, cause: Throwable? = null) : UnrunnableProcess(message, cause)

class NoMatchForInput(message: String? = null, cause: Throwable? = null) : UnrunnableProcess(message, cause) {
    constructor(word: String) : this(message = "No way to transition to $word")
}

class DSLParseException(message: String? = null, cause: Throwable? = null) : ProcessException(message, cause)
