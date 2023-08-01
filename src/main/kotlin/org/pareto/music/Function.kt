package org.pareto.music

sealed interface Fn {

    val name: Name

    /**
     * calls this [Fn] with [params] in a [namespace]
     */
    fun call(
        params: List<Music>,
        namespace: FunctionNamespace<Definition>
    ): Music = namespace[name]?.replacingArgsWith(params, namespace)
        ?: throw UnrunnableProcess("No function with name $name in namespace")

    companion object {

        /**
         * Rebuilds [Music] by replacing [Note]s with their associated [Music] in [withReplacements]
         */
        fun rebuild(music: Music, inNamespace: FunctionNamespace<Definition>, withReplacements: Map<Note.Name, Music>): Music {
            return when(music) {
                is Note -> withReplacements[music.name] ?: music
                is Call -> music.call(inNamespace, withReplacements)
                is Harmony -> Harmony(rebuild(music.Left, inNamespace, withReplacements), rebuild(music.Right, inNamespace, withReplacements) as Sound)
                is Melody -> Melody(rebuild(music.Left, inNamespace, withReplacements), rebuild(music.Right, inNamespace, withReplacements))
                is Decision -> Decision(rebuild(music.Left, inNamespace, withReplacements) as Sound, rebuild(music.Right, inNamespace, withReplacements))
                is Silence -> Silence
            }
        }
    }


    /**
     * A function's name. Must be PascalCase.
     */
    class Name(text: Text.PascalCase) : MusicName(text) {
        companion object {

            /**
             * Acts as secondary constructor for [Name]
             */
            operator fun invoke(s: String): Name =
                Name(Text.PascalCase(s))
        }
    }


    /**
     * A reference to a [Definition] process.
     *
     * @param name the [Fn.Name] of the corresponding [Definition] process.
     * @param params the [List] of [Music] associated with this call.
     */
    data class Call(override val name: Name, val params: List<Music> = listOf()) : Fn, Expandable, NonTerminal {

        /**
         * Convenience method for calling a [Fn.Call] [inNamespace].
         */
        fun call(inNamespace: FunctionNamespace<Definition>) = call(params, inNamespace)

        /**
         * Calls a function by first materializing its [params] [withReplacements].
         */
        fun call(
            inNamespace: FunctionNamespace<Definition>,
            withReplacements: Map<Note.Name, Music>
        ) = Call(name, params.map {rebuild(it, inNamespace, withReplacements)})
            .call(inNamespace)
    }

    /**
     * A process with a [Fn.Name] which can be referred to by a [Call]. Names
     * must be unique within a [Grammar].
     *
     * @param name the name of this process.
     * @param music the corresponding process.
     */
    data class Definition(
        override val name: Name,
        val music: NonTerminal,
        val requiredArgs: List<Note.Name> = listOf(),
    ) : Fn {

        init { Validate.argsAreUsedInBody(this) }

        fun replacingArgsWith(params: List<Music>, namespace: FunctionNamespace<Definition>): Music {
            val map = requiredArgs.zip(params).associate { it.first to it.second }.toMap()
            return rebuild(music, namespace, map)
        }
    }
}

/**
 * Generates a language-agnostic [String] representation of a [Fn.Definition].
 */
fun Fn.Definition.canonical() =
    if (requiredArgs.isNotEmpty())
        "$name(${requiredArgs.joinToString()})\n  : ${music.canonical()}"
    else
        "$name\n  : ${music.canonical()}"