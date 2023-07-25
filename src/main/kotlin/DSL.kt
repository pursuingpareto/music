package org.example.pg

fun Grammar.Companion.compose(block: GrammarBuilder.() -> Unit) = GrammarBuilder().apply(block).build()

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
        private var process: Process? = null

        /**
         * Converts `"Foo"("a")` to a [Fn.Call] with single param `Expanding("a")`
         */
        operator fun String.invoke(vararg maybeParams: Any): Fn.Call = assignToProcess {
            val params = maybeParams.map { it.asProcess() }
            Fn.Call(Fn.Name(this), params)
        }

        override fun build(): Fn.Definition = process?.let {
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
            Dimension.Time(this.asProcess(), that.asProcess())
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
            Dimension.Choice(this.asProcess(), that.asProcess())
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
            Dimension.Space(this.asProcess(), that.asProcess())
        }

        private fun <T : Process> assignToProcess(block: () -> T): T = block().also { process = it }

        /**
         * Coerces receiver of [Any] type to [Process]. Fails unless receiver is
         * [String], [Process], or a 0-arity function.
         *
         * When the receiver is a zero-arity function, call it and return an [Process.Optional] with
         * child `process` equal to the coerced return value.
         *
         * When receiver is a String, first try to coerce it to a [Fn.Call].
         * If this fails (which will happen if the string is not PascalCase), then
         * coerce to an [Expanding] process.
         */
        private fun Any.asProcess(): Process = when (this) {
            is Function0<*> -> asProcess()
            is String -> asProcess()
            is Process -> this
            else -> throw DSLParseException("could not convert $this to Process")
        }

        private fun Function0<*>.asProcess(): Process {
            return invoke()?.asProcess()
                ?.let { Process.Optional(it) }
                ?: throw DSLParseException("can't convert function to process")
        }

        private fun String.asProcess(): Process {
            return if (isEmpty()) { Process.Empty } else { try {
                Fn.Call(Fn.Name(this))
            } catch (_: IllegalArgumentException) {
                Expanding(this)
            }
            }
        }
    }
}
