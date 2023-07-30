package org.pareto.music

import org.pareto.music.perform.Decider
import org.pareto.music.perform.StringBuilderPerformer
import kotlin.test.Test
import kotlin.test.assertEquals

@Suppress("LocalVariableName")
class PlayerSpec {
    @Test
    fun `piecewise player plays random choices`() {
        val CoinFlip = "CoinFlip"
        val FlipTwice = "FlipTwice"
        val Flip4 = "Flip4"
        val Flip8 = "Flip8"
        val Flip16 = "Flip16"
        val Flip32 = "Flip32"
        val heads = "heads"
        val tails = "tails"

        val grammar = Grammar.compose {
            CoinFlip {
                heads or tails
            }

            FlipTwice {
                CoinFlip then CoinFlip
            }

            Flip4 {
                FlipTwice then FlipTwice
            }

            Flip8 {
                Flip4 then Flip4
            }

            Flip16 {
                Flip8 then Flip8
            }

            Flip32 {
                Flip16 then Flip16
            }
        }

        val player = StringBuilderPerformer(grammar, Decider.UniformRandom)
        val flips = player.play(Flip32)
        assertEquals(32, flips.split(" ").size)

        // This will fail 1 in every 2^31 runs :(
        assertEquals(setOf(heads, tails), flips.split(" ").toSet())

        // more statistical assertions
        val headsCount = flips.split(" ").count { it == "heads" }
        val tailsCount = flips.split(" ").count { it == "tails" }
        assert(headsCount > 8) { "headsCount was $headsCount" }
        assert(tailsCount > 8) { "tailsCount was $tailsCount" }
    }
}