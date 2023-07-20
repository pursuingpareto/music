package org.example.pg


typealias OnWord = (String) -> Any?

typealias Expander = Name.Expanding.(String) -> Any?

typealias Namespace = MutableMap<Name, OnWord>

/**
 * A name for a [Defined] process. Must be PascalCase.
 */
sealed class Name(private val name: String) {
    override fun toString() = name
    protected fun String.isPascalCase() = "^([A-Z][a-z0-9]*)+$".toRegex().containsMatchIn(this)

    override fun hashCode() = name.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Name) return false
        if (name != other.name) return false
        return true
    }


    class Defined(name: String): Name(name) {
        init { require(name.isPascalCase()) }
    }


    class Expanding(name: String): Name(name) {
        init {
            require(!name.isPascalCase())
            require(name.isNotEmpty())
        }
    }
}