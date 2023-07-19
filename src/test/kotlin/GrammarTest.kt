import kotlin.test.assertEquals
import org.example.pg.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import Grammars.RPSGrammar
import Grammars.RockPaperScissors
import Grammars.celebrate
import Grammars.groan
import Grammars.paper
import Grammars.rock
import Grammars.scissors
import Grammars.shoot
import Grammars.win

class GrammarTest {

  @Test
  fun testCanonical() {
    assertEquals("abc", Expanding("abc").canonical())
    assertEquals("[ a ]", Optional(Expanding("a")).canonical())
    assertEquals("a > b", Dimension.Time(Expanding("a"), Expanding("b")).canonical())
    assertEquals("a | b", Dimension.Choice(Expanding("a"), Expanding("b")).canonical())
    assertEquals("a & b", Dimension.Space(Expanding("a"), Expanding("b")).canonical())

    val p1 = Defined(
      Name.Defined("Process1"),
      Dimension.Time(
        Expanding("a"),
        Expanding("b")))
    assertEquals("""
      Process1
        : a > b
    """.trimIndent(), p1.canonical())

    val p2 = Defined(
      Name.Defined("Process2"),
      Reference(Name.Defined("Process1")))
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
        { "a" } then "b"
      }
    }

    val g2 = Grammar.fromDsl {
      "MaybeB" {
        "a" then { "b" }
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
        win and celebrate
      }
    }
    val process = parallelGrammar.processes.first().process
    assert(process is Dimension.Space)
    val parallel = process as Dimension.Space
    assertEquals("win", parallel.back.canonical())
    assertEquals("celebrate", parallel.fore.canonical())
  }

  @Test
  fun testCompiledFailsOnInvalid() {
    val comp = Program.from(RPSGrammar) { word ->
      if ("$this" != word) throw RuntimeException("Invalid") else true }

    comp.begin(RockPaperScissors)
    comp.invoke(rock)(paper)(scissors)
    comp.invoke(shoot)(rock)(win)
    assertThrows<RuntimeException> {
      comp.invoke(groan)
    }
  }

  @Test
  fun testTimings() {
    val timings = mutableListOf<Pair<Name, Double>>()
    var start = System.nanoTime()
    Program
      .from(RPSGrammar) { word ->
        val now = System.nanoTime()
        val dur = now - start
        start = now
        timings.add(Pair(this, dur / 1000.0))
        if ("$this" != word) throw RuntimeException("Invalid!") else true
      }
      .begin(RockPaperScissors)
      .invoke(rock).also { Thread.sleep(42) }
      .invoke(paper).also { Thread.sleep(33) }
      .invoke(scissors)(shoot)(rock)(win)(celebrate)

    println("TIMINGS: $timings")
  }

}