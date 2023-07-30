package org.pareto.music.demo.madlibs

import org.pareto.music.Grammar
import org.pareto.music.canonical
import org.pareto.music.compose
import org.pareto.music.play.RandomChoiceMusician

fun main() {
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
    val noun = "noun"
    val verb = "verb"
    //endregion

    val madLibs = Grammar.compose {

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
            verb then "ed"
        }

        Plural(noun) {
            noun then "s"
        }

        Gerund(verb) {
            verb then "ing"
        }

        Story {
            "It was a " then Adjective then " November day." then
            " I woke up to the " then Adjective then " smell of " then
            Noun then " roasting in the " then Noun then " downstairs." then
            " I " then PastTense(Verb) then " down the stairs" then
            " to see if I could " then Adverb then " help " then Verb then
            " the dinner." then " My mom said, \"See if your " then Noun then
            " needs a fresh " then Noun then ".\" So I carried a tray of " then
            Plural(Noun) then " into the " then Gerund(Verb) then " room." then
            " When I got there, I couldn't believe my " then Plural(Noun) then "!" then
            " There were " then Plural(Noun) then " " then Gerund(Verb) then " on the " then
            Noun then "!"
        }
    }

    val storyCollector = mutableListOf<String>()
    val player = RandomChoiceMusician(madLibs) { storyCollector.add(it.canonical())}
    player.play(Story)
    println(storyCollector.joinToString("").split(" ")
        .chunked(10) { it.joinToString(" ") }.joinToString("\n"))
}