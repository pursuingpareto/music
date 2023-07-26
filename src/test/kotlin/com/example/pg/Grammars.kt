import org.example.pg.Grammar
import org.example.pg.compose

object Grammars {
    const val RockPaperScissors = "RockPaperScissors"
    const val Countdown = "Countdown"
    const val Reveal = "Reveal"
    const val ResolveOutcome = "ResolveOutcome"
    const val Win = "Win"
    const val Lose = "Lose"

    // Terminal objects
    val rock = "rock"
    val paper = "paper"
    val scissors = "scissors"
    val shoot = "shoot"
    val win = "win"
    val lose = "lose"
    val celebrate = "celebrate"
    val groan = "groan"
    val tie = "tie"

    // Full rock paper scissors grammar WITH builder
    val RPSGrammar = Grammar.compose {
        RockPaperScissors {
            Countdown then Reveal then ResolveOutcome
        }

        Countdown {
            rock then paper then scissors then shoot
        }

        Reveal {
            rock or paper or scissors
        }

        ResolveOutcome {
            Win or Lose or tie
        }

        Win {
            win then { celebrate }
        }

        Lose {
            lose then { groan }
        }
    }
}
