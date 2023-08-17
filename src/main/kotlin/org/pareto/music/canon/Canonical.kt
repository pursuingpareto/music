package org.pareto.music.canon

import org.pareto.music.Dimension
import org.pareto.music.Fn
import org.pareto.music.Music
import org.pareto.music.Note

/**
 * Produces a canonical, language-agnostic string representation of this process.
 */
fun Music?.canonical(): String = when (this) {
    null ->          "[ ]"
    is Dimension.Choice -> when(Wont) {
        // this indicates "Optional" Music
        null ->  "[ ${Will.canonical()} ]"
        else ->   "${Will.canonical()} | ${Wont.canonical()}" }
    is Dimension.Space ->  "${Front.canonical()} & ${Back.canonical()}"
    is Dimension.Time ->    "${Tick.canonical()} > ${Tock.canonical()}"
    is Fn.Definition -> { if (requiredArgs.isNotEmpty())
        "$name(${requiredArgs.joinToString()})\n  : ${music.canonical()}"
    else "$name\n  : ${music.canonical()}"
    }

    is Fn.Call -> { if (params.isNotEmpty())
        "$name(${params.joinToString { it.canonical() }})"
    else "$name" }
    is Note ->             name.toString()
}