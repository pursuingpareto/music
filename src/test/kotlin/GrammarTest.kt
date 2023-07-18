import kotlin.test.assertEquals
import org.example.pg.Program
import org.example.pg.Grammar
import org.example.pg.Parallel
import org.example.pg.fromDsl
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GrammarTest {

  companion object {
    // Defined Process Names
    const val RockPaperScissors = "RockPaperScissors"
    const val Countdown         = "Countdown"
    const val Reveal            = "Reveal"
    const val ResolveOutcome    = "ResolveOutcome"
    const val Win               = "Win"
    const val Lose              = "Lose"

    // Terminal objects
    private const val rock      = "rock"
    private const val paper     = "paper"
    private const val scissors  = "scissors"
    private const val shoot     = "shoot"
    private const val win       = "win"
    private const val lose      = "lose"
    private const val celebrate = "celebrate"
    private const val groan     = "groan"
    private const val tie       = "tie"

    // Full rock paper scissors grammar WITH builder
    val RPSGrammar = Grammar.fromDsl {
      RockPaperScissors {
        Countdown THEN Reveal THEN ResolveOutcome
      }

      Countdown {
        rock THEN paper THEN scissors THEN shoot
      }

      Reveal {
        rock OR paper OR scissors
      }

      ResolveOutcome {
        Win OR Lose OR tie
      }

      Win {
        win THEN {celebrate}
      }

      Lose {
        lose THEN {groan}
      }
    }
  }


  @Test
  fun testExtend() {
    val g1 = Grammar.fromDsl {
      "MaybeA" {
        { "a" } THEN "b"
      }
    }

    val g2 = Grammar.fromDsl {
      "MaybeB" {
        "a" THEN { "b" }
      }
    }

    val g3 = g1.extend(g2)
    assertEquals(2, g3.processes.count())
  }

  @Test
  fun testCompilerTilWin() {
    Program
      .from(RPSGrammar)
      .begin(RockPaperScissors)
      .invoke(rock)(paper)(scissors)(shoot)(rock)(win)(celebrate)
  }

  @Test
  fun testParallel() {
    val parallelGrammar = Grammar.fromDsl {
      "WinAndCelebrate" {
        win AND celebrate
      }
    }
    val process = parallelGrammar.processes.first().process
    assert(process is Parallel)
    val parallel = process as Parallel
    assertEquals("win", parallel.foreground.toString())
    assertEquals("celebrate", parallel.background.toString())
  }

  @Test
  fun testCompiledFailsOnInvalid() {
    val comp = Program.from(RPSGrammar) { word ->
      if (this != word) throw RuntimeException("Invalid") else true }

    comp.begin(RockPaperScissors)
    comp.invoke(rock)(paper)(scissors)
    comp.invoke(shoot)(rock)(win)
    assertThrows<RuntimeException> {
      comp.invoke(groan)
    }
  }

  @Test
  fun testTimings() {
    val timings = mutableListOf<Pair<String, Double>>()
    var start = System.nanoTime()
    Program
      .from(RPSGrammar) { word ->
        val now = System.nanoTime()
        val dur = now - start
        start = now
        timings.add(Pair(this, dur / 1000.0))
        if (this != word) throw RuntimeException("Invalid!") else true
      }
      .begin(RockPaperScissors)
      .invoke(rock).also { Thread.sleep(42) }
      .invoke(paper).also { Thread.sleep(33) }
      .invoke(scissors)(shoot)(rock)(win)(celebrate)

    println("TIMINGS: $timings")
  }

}