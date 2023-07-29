package org.pareto.music

import org.pareto.music.Lib.Possible

fun Grammar.Companion.compose(includeStdLib: Boolean = true, block: GrammarBuilder.() -> Unit): Grammar {
    return if (includeStdLib) { Lib.StandardGrammar.extend(block) } else { GrammarBuilder().apply(block).build() }
}

infix fun Grammar.extend(block: GrammarBuilder.() -> Unit) = this.extend(GrammarBuilder().apply(block).build())

interface Builder<T> { fun build(): T }

/**
 * Converts the DSL representation of a [Grammar] to an actual Grammar.
 *
 * The following code will produce a Grammar with two [Fn.Definition]s:
 *
 * ```kotlin
 * "Repeated"("process") {
 *   "process" then "Repeated"("process")
 * }
 *
 * "Heart" {
 *   "Repeated"("beat")
 * }
 * ```
 */
class GrammarBuilder : Builder<Grammar> {
    private var name: Fn.Name? = null
    private val components = mutableListOf<Fn.Definition>()
    private var definedBuilder: FunctionDefinitionBuilder? = null

    override fun build() = Grammar(components)

    operator fun String.invoke(vararg args: RequiredArg, block: FunctionDefinitionBuilder.() -> Unit) = this.apply {
        name = Fn.Name(this).also { name ->
            definedBuilder = FunctionDefinitionBuilder(name, args).also { builder ->
                builder.apply(block)
                components.add(builder.build())
            }
        }
    }

    /**
     * Converts the DSL representation of a [Fn.Definition] to an actual definition.
     *
     * The following code will produce a Fn.Definition:
     *
     * ```kotlin
     * "Repeated"("process") {
     *   "process" then "Repeated"("process")
     * }
     * ```
     */
    class FunctionDefinitionBuilder(
        private val name: Fn.Name,
        args: Array<out RequiredArg>? = null,
    ) : Builder<Fn.Definition> {
        private val args = args?.toList() ?: listOf()
        private var music: NonTerminal? = null

        /**
         * Converts `"Foo"("a")` to a [Fn.Call] with single param `Expanding("a")`
         */
        operator fun String.invoke(vararg maybeParams: Any): Fn.Call = assignToProcess {
            val params = maybeParams.map { it.asMusic() }
            Fn.Call(Fn.Name(this), params)
        }

        override fun build(): Fn.Definition = music?.let {
            Fn.Definition(name, it, args)
        } ?: throw DSLParseException("can't build null process")

        /**
         * Constructs a [Dimension.Time] from **`process1`** and **`process2`**
         *
         * Example Usage:
         * ```
         * val a_then_b = "a" then "b"
         * ```
         */
        infix fun Any.then(that: Any) = assignToProcess {
            Dimension.Time(this.asMusic(), that.asMusic())
        }

        /**
         * Constructs a [Dimension.Choice] from **`process1`** and **`process2`**
         *
         * Example Usage:
         * ```
         * val a_or_b = "a" or "b"
         * ```
         */
        infix fun Any.or(that: Any) = assignToProcess {
            Dimension.Choice(this.asSound(), that.asMusic())
        }

        /**
         * Constructs a [Dimension.Space] from **`process1`** and **`process2`**
         *
         * Example Usage:
         * ```
         * val a_and_b = "a" and "b"
         * ```
         */
        infix fun Any.and(that: Any) = assignToProcess {
            Dimension.Space(this.asMusic(), that.asSound())
        }

        private fun <T : NonTerminal> assignToProcess(block: () -> T): T = block().also { music = it }

        /**
         * Coerces receiver of [Any] type to [Music]. Fails unless receiver is
         * [String], [Music], or a 0-arity function.
         *
         * When the receiver is a zero-arity function, call it and return an [Possible] with
         * child `process` equal to the coerced return value.
         *
         * When receiver is a String, first try to coerce it to a [Fn.Call].
         * If this fails (which will happen if the string is not PascalCase), then
         * coerce to an [Note] process.
         */
        private fun Any.asMusic(): Music = when (this) {
            is Function0<*> -> asSound()
            is String -> asSound()
            is Music -> this
            else -> throw DSLParseException("could not convert $this to Process")
        }

        private fun Any.asSound(): Sound = this.asMusic() as? Sound ?: throw DSLParseException("could not convert ${this.asMusic()} to Sound")

        private fun Function0<*>.asSound(): Sound {
            return invoke()?.asSound()
                ?.let { Possible(it) }
                ?: throw DSLParseException("can't convert function to process")
        }

        private fun String.asSound(): Music {
            return if (isEmpty()) { Silence } else { try {
                Fn.Call(Fn.Name(this))
            } catch (_: IllegalArgumentException) {
                Note(Note.Name(this))
            }
            }
        }
    }
}
