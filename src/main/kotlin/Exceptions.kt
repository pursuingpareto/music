package org.example.pg

open class ProcessException(message: String? = null, cause: Throwable? = null) : Exception(message, cause)
class GrammarValidationException(message: String? = null, cause: Throwable? = null): ProcessException(message, cause)
class UnrunnableProcess(message: String? = null, cause: Throwable? = null): ProcessException(message, cause)
class DSLParseException(message: String? = null, cause: Throwable? = null): ProcessException(message, cause)
