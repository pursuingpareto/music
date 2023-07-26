@file:Suppress("FunctionName", "MemberVisibilityCanBePrivate")

package org.example.pg

object Lib {

    /**
     * @see [Possible]
     */
    const val Possible = "Possible"

    /**
     * @see [Repeating]
     */
    const val Repeating = "Repeating"

    /**
     * @see [OneOrMore]
     */
    const val OneOrMore = "OneOrMore"

    /**
     * @see [ZeroOrMore]
     */
    const val ZeroOrMore = "ZeroOrMore"

    /**
     * helper string to make your DSL grammars look nice <3
     */
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
     * * [OneOrMore]
     *     ```
     *     OneOrMore(process) {
     *         process then Possible(OneOrMore(process))
     *     }
     *     ```
     *
     * * [ZeroOrMore]
     *     ```
     *     ZeroOrMore(process) {
     *         Possible(OneOrMore(process))
     *     }
     *     ```
     */
    val StandardGrammar: Grammar = Grammar(
        // this isn't pretty, but no need to depend on the DSL in this package.
        listOf(
            Fn.Definition(
                Fn.Name(Possible),
                Decision(
                    Expanding(process),
                    Empty
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
                Fn.Name(OneOrMore),
                Sequence(
                    Expanding(process),
                    Fn.Call(
                        Fn.Name(Possible),
                        listOf(
                            Fn.Call(
                                Fn.Name(OneOrMore),
                                listOf(
                                    Expanding(process)
                                )
                            )
                        )
                    )
                ),
                listOf(process)
            ),
            Fn.Definition(
                Fn.Name(ZeroOrMore),
                Fn.Call(
                    Fn.Name(Possible),
                    listOf(
                        Fn.Call(
                            Fn.Name(OneOrMore),
                            listOf(
                                Expanding(process)
                            )
                        )
                    )
                ),
                listOf(process)
            )
        )
    )

    /**
     * A Possible process can be skipped or evaluated at runtime.
     */
    fun Possible(process: Process): Process = process or EMPTY

    /**
     * A Repeating process repeats forever.
     */
    fun Repeating(process: Process): Process = process then Repeating(process)

    /**
     * A OneOrMore process happens 1 or more times.
     */
    fun OneOrMore(process: Process): Process = process then Possible(OneOrMore(process))

    /**
     * A ZeroOrMore process happens 0 or more times.
     */
    fun ZeroOrMore(process: Process): Process = Possible(OneOrMore(process))

    /**
     * The Empty process. Always skipped at runtime.
     */
    val EMPTY = Empty
}
