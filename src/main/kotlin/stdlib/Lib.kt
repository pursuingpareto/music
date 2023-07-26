package org.example.pg.stdlib
import org.example.pg.* // ktlint-disable no-wildcard-imports
import org.example.pg.Process
import org.example.pg.Sequence

@Suppress("FunctionNames")
object Lib {

    const val Possible = "Possible"
    const val Repeating = "Repeating"
    const val Repeatable = "Repeatable"
    const val Reversed = "Reversed"
    const val Inverted = "Inverted"
    const val InsideOut = "InsideOut"
    const val process = "process"

    /**
     * The [StandardGrammar] is prepended to every grammar built with the DSL.
     * It contains the following functions:
     *
     * * [Possible]
     *    ```
     *    Possible(process) {
     *        process or EMPTY
     *    }
     *    ```
     *
     * * [Repeating]
     *     ```
     *     Repeating(process) {
     *         process then Repeating(process)
     *     }
     *     ```
     *
     * * [Repeatable]
     *     ```
     *     Repeating(process) {
     *         process then Possible(Repeatable(process))
     *     }
     *     ```
     */
    val StandardGrammar: Grammar = Grammar(
        listOf(
            Fn.Definition(
                Fn.Name(Possible),
                Sequence(
                    Expanding(process),
                    Process.Empty
                ),
                listOf(process)
            ),
            Fn.Definition(
                Fn.Name(Repeating),
                Sequence(
                    Expanding(process),
                    Fn.Call(
                        Fn.Name(Repeating),
                        listOf(
                            Expanding(process)
                        )
                    )
                ),
                listOf(process)
            ),
            Fn.Definition(
                Fn.Name(Repeatable),
                Sequence(
                    Expanding(process),
                    Fn.Call(
                        Fn.Name(Possible),
                        listOf(
                            Fn.Call(
                                Fn.Name(Repeatable),
                                listOf(
                                    Expanding(process)
                                )
                            )
                        )
                    )
                ),
                listOf(process)
            )
        )
    )

    /**
     * A [Possible] process can be skipped or evaluated at runtime.
     */
    fun Possible(process: Process): Process = process or EMPTY

    /**
     * A [Repeating] process repeats forever.
     */
    fun Repeating(process: Process): Process = process then Repeating(process)

    /**
     * A [Repeatable] process happens 1 or more times.
     */
    fun Repeatable(process: Process): Process = process then Possible(Repeatable(process))

    /**
     * A [Reversed] Sequence goes tock-tick instead of tick-tock.
     */
    fun Reversed(time: Dimension.Time): Sequence = time.Tock then time.Tick

    /**
     * An [Inverted] Decision swaps options.
     */
    fun Inverted(choice: Dimension.Choice): Decision = choice.Wont or choice.Will

    /**
     * An [InsideOut] Parallel process exchanges front and back.
     */
    fun InsideOut(space: Dimension.Space): Parallel = space.Front and space.Back

    /**
     * The Empty process. Always skipped at runtime.
     */
    val EMPTY = Process.Empty
}
