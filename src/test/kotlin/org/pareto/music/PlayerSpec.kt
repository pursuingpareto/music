package org.pareto.music

import org.pareto.music.play.RandomChoiceMusician
import kotlin.test.Test

class PlayerSpec {
    @Test
    fun `piecewise player plays random choices`() {
        val CoinFlip = "CoinFlip"
        val FlipTwice = "FlipTwice"
        val Flip4 = "Flip4"
        val Flip8 = "Flip8"
        val Flip16 = "Flip16"
        val FlipForever = "FlipForever"
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
        }

        val player = RandomChoiceMusician(grammar)
        player.play(Fn.Name(Flip16))
    }
}