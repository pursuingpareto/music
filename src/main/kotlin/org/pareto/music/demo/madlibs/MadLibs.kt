package org.pareto.music.demo.madlibs

import org.pareto.music.Grammar
import org.pareto.music.canon.compose
import org.pareto.music.perform.Decider
import org.pareto.music.perform.StringBuilderPerformer

@Suppress("LocalVariableName")
fun main() {

    fun show(string: String) =
        string.split(" ")
            .chunked(10) { it.joinToString(" ") }
            .joinToString("\n")
            .also { println(it) }

    //region helper strings
    val Adjective = "Adjective"
    val independent = "independent"
    val boring = "boring"
    val furry = "furry"
    val brave = "brave"
    val depressed = "depressed"
    val humble = "humble"
    val loving = "loving"
    val grumpy = "grumpy"
    val silly = "silly"
    val strangely_aggressive = "strangely aggressive"
    val discontent = "discontent"
    val gooey = "gooey"

    val Verb = "Verb"
    val jump = "jump"
    val flail = "flail"
    val laugh = "laugh"
    val crawl = "crawl"
    val stab = "stab"
    val ponder = "ponder"
    val clean = "clean"
    val touch = "touch"
    val yodel = "yodel"
    val whimper = "whimper"

    val Adverb = "Adverb"
    val carefully = "carefully"
    val joyfully = "joyfully"
    val unwittingly = "unwittingly"
    val greedily = "greedily"
    val obediently = "obediently"
    val calmly = "calmly"
    val powerfully = "powerfully"
    val madly = "madly"
    val truly = "truly"
    val courageously = "courageously"
    val lovingly = "lovingly"

    val Noun = "Noun"
    val cat = "cat"
    val trampoline = "trampoline"
    val pirate = "pirate"
    val ship = "ship"
    val superhero = "superhero"
    val castle = "castle"
    val banana = "banana"
    val monkey = "monkey"
    val bouncy_ball = "bouncy ball"
    val stapler = "stapler"
    val zoo = "zoo"
    val monster = "monster"
    val lion = "lion"
    val monster_truck = "monster truck"
    val top_hat = "top hat"
    val bone = "bone"

    val Story = "Story"
    val PastTense = "PastTense"
    val Plural = "Plural"
    val Gerund = "Gerund"
    val Sentence = "Sentence"
    val Exclaim = "Exclaim"
    val Quote = "Quote"
    val State = "State"
    val text = "text"
    val noun = "noun"
    val verb = "verb"
    val punctuation = "punctuation"

    val beginning = "beginning"
    val middle = "middle"
    val end = "end"

    val MadLibs = "MadLibs"
    val MLIntro = "MLIntro"
    val MLMiddle = "MLMiddle"
    val MLEnd = "MLEnd"

    val PlayMadLibs = "PlayMadLibs"
    val macguffin = "macguffin"
    //endregion

    val madLibs = Grammar.compose {

        Story(beginning, middle, end) {
            beginning then middle then end
        }

        // TODO - Ensure function call params get resolved before passing into the
        //  corresponding function. This kind of thing doesn't work when you pass
        //  it a function call. The call is *separately* realized three times.
        MadLibs(macguffin) {
            Story(MLIntro(macguffin), MLMiddle(macguffin), MLEnd(macguffin))
        }

        PlayMadLibs {
            MadLibs(banana)
        }

        MLIntro(macguffin) {
            State("It was a" then Adjective then "November day") then
            State("I woke up to the" then Adjective then "smell of" then macguffin then "roasting in the" then Noun then "downstairs")
        }

        MLMiddle(macguffin) {
            State("So I" then PastTense(Verb) then "down the stairs" then "to see if I could" then Adverb then "help" then Verb then "the" then macguffin) then
            "My mom said," then Quote(State("See if your" then Noun then "needs a fresh" then Noun)) then
            State("So I carried a tray of" then Plural(Noun) then "into the" then Gerund(Verb) then "room")
        }

        MLEnd(macguffin) {
            Exclaim("When I got there, I couldn't believe my" then Plural(Noun)) then
            Exclaim("There were" then Plural(Noun) then Gerund(Verb) then "on the" then macguffin)
        }

        Adjective {
            independent or boring or furry or brave or
            depressed or humble or loving or grumpy or
            silly or strangely_aggressive or
            discontent or gooey
        }

        Verb {
            jump or flail or laugh or crawl or stab or
            ponder or clean or touch or yodel or whimper
        }

        Adverb {
            carefully or joyfully or unwittingly or
            greedily or obediently or calmly or
            powerfully or madly or truly or
            courageously or lovingly
        }

        Noun {
            cat or trampoline or pirate or ship or
            superhero or castle or banana or
            monkey or bouncy_ball or stapler or zoo or
            monster or lion or monster_truck or top_hat or bone
        }

        PastTense(verb) {
            verb and "ed"
        }

        Plural(noun) {
            noun and "s"
        }

        Gerund(verb) {
            verb and "ing"
        }

        Quote(text) {
            "\"" and text and "\""
        }

        Sentence(text, punctuation) {
            text and punctuation
        }

        Exclaim(text) {
            Sentence(text, "!")
        }

        State(text) {
            Sentence(text, ".")
        }
    }

    val player = StringBuilderPerformer(madLibs, Decider.UniformRandom)
    val storyText = player.play(PlayMadLibs)
    show(storyText)
}