import org.example.pg.Grammar
import org.example.pg.Program
import org.example.pg.UnrunnableProcess
import org.example.pg.fromDsl
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test

class ProgramSpecTest {

    private val optionalAThenB = Grammar.fromDsl {
        "OptionalAThenB" {
            { "a" } then "b"
        }
    }

    private val aThenOptionalB = Grammar.fromDsl {
        "AThenOptionalB" {
            "a" then { "b" }
        }
    }

    private val optionalMiddle = Grammar.fromDsl {
        "OptionalMiddle" {
            "a" then { "b" } then "c"
        }
    }

    private val ab = Grammar.fromDsl {
        "AB" {
            "a" then "b"
        }
    }

    private val abc = Grammar.fromDsl {
        "ABC" {
            "a" then "b" then "c"
        }
    }

    @Test
    fun testSequences() {
        Program.from(ab).begin("AB")("a")("b")

        assertThrows<UnrunnableProcess> {
            Program.from(ab).begin("AB")("a")("b")("c")
        }

        Program.from(abc).begin("ABC")("a")("b")("c")

        assertThrows<UnrunnableProcess> {
            Program.from(abc).begin("ABC")("a")("b")("c")("d")
        }

        assertThrows<UnrunnableProcess> {
            Program.from(abc).begin("ABC")("a")("b")("d")
        }
    }

    @Test
    fun testOptionalAtBeginningOfSequenceIsOptional() {
        Program.from(optionalAThenB)
            .begin("OptionalAThenB")
            .invoke("a")("b")

        Program.from(optionalAThenB)
            .begin("OptionalAThenB")
            .invoke("b")

        assertThrows<UnrunnableProcess> {
            Program.from(optionalAThenB)
                .begin("OptionalAThenB")
                .invoke("fail")
        }
    }

    @Test
    fun testOptionalAtEndOfSequenceIsOptional() {
        Program.from(aThenOptionalB)
            .begin("AThenOptionalB")
            .invoke("a")("b")

        assertThrows<UnrunnableProcess> {
            Program.from(aThenOptionalB)
                .begin("AThenOptionalB")
                .invoke("b")
        }
    }

    @Test
    fun testOptionalInMiddle() {
        Program.from(optionalMiddle)
            .begin("OptionalMiddle")
            .invoke("a")("b")("c")

        Program.from(optionalMiddle)
            .begin("OptionalMiddle")
            .invoke("a")("c")

        assertThrows<UnrunnableProcess> {
            Program.from(optionalMiddle)
                .begin("OptionalMiddle")
                .invoke("a")("d")("c")
        }

        assertThrows<UnrunnableProcess> {
            Program.from(optionalMiddle)
                .begin("OptionalMiddle")
                .invoke("a")("b")("f")
        }

        assertThrows<UnrunnableProcess> {
            Program.from(optionalMiddle)
                .begin("OptionalMiddle")
                .invoke("a")("c")("f")
        }
    }
}