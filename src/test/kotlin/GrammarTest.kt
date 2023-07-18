import kotlin.test.assertEquals
import org.example.pg.*
// import org.example.pg.Grammar
// import org.example.pg.fromDsl
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
  fun testCanonical() {
    assertEquals("abc", Terminal("abc").canonical())
    assertEquals("[ a ]", Optional(Terminal("a")).canonical())
    assertEquals("a > b", Binary.Sequence(Terminal("a"), Terminal("b")).canonical())
    assertEquals("a | b", Binary.Decision(Terminal("a"), Terminal("b")).canonical())
    assertEquals("a & b", Binary.Eclipse(Terminal("a"), Terminal("b")).canonical())

    val p1 = Defined(
      Process.Name("Process1"),
      Binary.Sequence(
        Terminal("a"),
        Terminal("b")))
    assertEquals("""
      Process1
        : a > b
    """.trimIndent(), p1.canonical())

    val p2 = Defined(
      Process.Name("Process2"),
      Reference(Process.Name("Process1")))
    assertEquals("""
      Process2
        : Process1
    """.trimIndent(), p2.canonical())

    val grammar = Grammar(listOf(p1, p2))
    assertEquals("""
      Process1
        : a > b""".trimIndent() + "\n\n" + """
      Process2
        : Process1
    """.trimIndent(), grammar.canonical())
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
    assert(process is Binary.Eclipse)
    val parallel = process as Binary.Eclipse
    assertEquals("win", parallel.foreground.canonical())
    assertEquals("celebrate", parallel.background.canonical())
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