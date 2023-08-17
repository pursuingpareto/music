@file:Suppress("FunctionName", "MemberVisibilityCanBePrivate")

package org.pareto.music.canon

import org.pareto.music.Decision
import org.pareto.music.Fn
import org.pareto.music.Grammar
import org.pareto.music.Harmony
import org.pareto.music.Melody
import org.pareto.music.Music
import org.pareto.music.Note
import org.pareto.music.Silence
import org.pareto.music.and
import org.pareto.music.or
import org.pareto.music.then

object StdLib {

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
     * grace note to make your DSL grammars look nice <3
     */
    val music = Note.Name("music")

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
            /**
             * Possible(music) {
             *   music or Silence
             * }
             */
            Fn.Definition(
                Fn.Name(Possible),
                Decision(
                    Note(music),
                    Silence),
                listOf(music)),
            /**
             * Repeating(music) {
             *   music then Repeating(music)
             * }
             */
            Fn.Definition(
                Fn.Name(Repeating),
                Melody(
                    Note(music),
                    Fn.Call(
                        Fn.Name(Repeating),
                        listOf(
                            Note(music)
                        ))
                ),
                listOf(music)),
            /**
             * OneOrMore(music) {
             *   music then Possible(OneOrMore(music))
             * }
             */
            Fn.Definition(
                Fn.Name(OneOrMore),
                Melody(
                    Note(music),
                    Fn.Call(
                        Fn.Name(Possible),
                        listOf(
                            Fn.Call(
                                Fn.Name(OneOrMore),
                                listOf(
                                    Note(music)
                                ))
                        ))
                ),
                listOf(music)),
            /**
             * ZeroOrMore(music) {
             *   Possible(OneOrMore(music))
             * }
             */
            Fn.Definition(
                Fn.Name(ZeroOrMore),
                Fn.Call(
                    Fn.Name(Possible),
                    listOf(
                        Fn.Call(
                            Fn.Name(OneOrMore),
                            listOf(
                                Note(music)
                            ))
                    )),
                listOf(music))
        ))

    /**
     * A Possible process can be skipped or evaluated at runtime.
     */
    fun Possible(music: Music): Decision = music or EMPTY

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
     * Converts a [Music] into 1 or more concurrent copies of itself.
     */
    @Suppress("Unused")
    fun Multiple(music: Music): Harmony = music and Possible(Multiple(music))

    /**
     * The Empty process. Always skipped at runtime.
     */
    val EMPTY = null
}
