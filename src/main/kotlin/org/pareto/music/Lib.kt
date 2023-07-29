@file:Suppress("FunctionName", "MemberVisibilityCanBePrivate")

package org.pareto.music

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
    val process = Note.Name("process")

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
                    Note(process),
                    Silence
                ),
                listOf(process)
            ),
            Fn.Definition(
                Fn.Name(Repeating),
                Melody(
                    Note(process),
                    Fn.Call(
                        Fn.Name(Repeating),
                        listOf(
                            Note(process)
                        )
                    )
                ),
                listOf(process)
            ),
            Fn.Definition(
                Fn.Name(OneOrMore),
                Melody(
                    Note(process),
                    Fn.Call(
                        Fn.Name(Possible),
                        listOf(
                            Fn.Call(
                                Fn.Name(OneOrMore),
                                listOf(
                                    Note(process)
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
                                Note(process)
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
    fun Possible(sound: Sound): Decision = sound or EMPTY

    /**
     * A Repeating process repeats forever.
     *
     * TODO - implement as `tailrec`
     */
    fun Repeating(music: Music): Melody = music then Repeating(music)

    /**
     * A OneOrMore process happens 1 or more times.
     *
     * TODO - implement as `tailrec`
     */
    fun OneOrMore(music: Music): Melody = music then Possible(OneOrMore(music))

    /**
     * A ZeroOrMore process happens 0 or more times.
     */
    fun ZeroOrMore(music: Music): Decision = Possible(OneOrMore(music))

    /**
     * Converts a [Sound] into 1 or more concurrent copies of itself.
     */
    @Suppress("Unused")
    fun Multiple(sound: Sound): Harmony = sound and Possible(Multiple(sound))

    /**
     * The Empty process. Always skipped at runtime.
     */
    val EMPTY = Silence
}
