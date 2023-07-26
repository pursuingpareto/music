# `music`

`music` is a high-level programming language where programs are *composed* like music. Just like with actual music, programming in `music` has two steps--"composition" and "performance". And this two-step approach to program creation can greatly reduce the overall complexity in software.

### Making `music` part 1: Composition
In the composition phase, the `music` programmer is concerned with properly modeling the "shape" of their program in ever-finer levels of granularity.

Let me walk you through an example of how we might model some processes related to flipping coins using the three core building blocks of `music`: sequencing (melody), concurrency (harmony), and branching (choice).

#### Example 1 (**branching**): Simple coin flip

A coin flip is **either** `heads`or `tails`. In the Kotlin implementation of `music`, we could represent this as:

```kotlin
"CoinFlip" {
    "heads" OR "tails"
}
```

You can read this as "the `CoinFlip` process is either `heads` or `tails`". What we've written here is a function definition. The body of that function is a **decision** process.

#### Example 2 (**sequences**): Flipping a coin twice
Now, what if we wanted to model flipping a coin *twice*? We've already got our `CoinFlip` process, so let's use that to define `FlipTwice`

```kotlin
FlipTwice {
    CoinFlip THEN CoinFlip
}
```

The body of this function is a **sequence** process (also known as **melody**).

#### Example 3 (**concurrency**): Flipping two coins at the same time

We can use a **parallel** process (aka **harmony**) to model two coins flipping *at the same time*:

```kotlin
FlipTwoCoins {
    CoinFlip AND CoinFlip
}
```

#### Example 4a (**recursion**): Flipping one coin *forever*
How would we model a repeated process? Like flipping a single coin forever?

We need to take advantage of recursion!

```kotlin
FlipForever {
    CoinFlip THEN FlipForever
}
```

This `FlipForever` function is a **sequence** with two steps:

1. Do `CoinFlip`
2. Do `FlipForever`, which means...
   
    a. Do `CoinFlip`

    b. Do `FlipForever`, which means...

   1. Do `CoinFlip`
   2. Do `FlipForever`, which means...

And so on. 

But we can actually model this even more cleanly. Because, really, the "do something forever" process has it's own "shape" which can be pulled out and reused.

#### Example 4b (**Functions with arguments**): Flipping a coin forever

```kotlin
Repeated(x) {
    x THEN Repeated(x)
}

FlipForever {
    Repeated(CoinFlip)
}
```

Great! We've modeled some coin flipping processes! But... what do they actually **do**? Like... in the computer? We've certainly gotten the "shape" right, but how do we breathe life into these 
static structures?

The `music` that we composed in this step would look like this in kotlin:

```kotlin
val coinGrammar = Grammar.compose {
    "CoinFlip" {
        "heads" OR "tails"
    }
    
    "FlipTwice" {
        "CoinFlip" THEN "CoinFlip"
    }
    
    "FlipTwoCoins" {
        "CoinFlip" AND "CoinFlip"
    }
    
    "Repeated"("x") {
        "x" THEN "Repeated"("x")
    }
    
    "FlipForever" {
        "Repeated"("CoinFlip")
    }
}
```
At this point we haven't said *what `heads` or `tails` actually **does**,* but that's what we're going to do now. 

### Making `music` part 2: Performance

In order to actually perform a program, we first need to **compile** it. We add customization by passing lambda functions into the compiler.

A "Performance" in `music` can be customized at three levels:

1. Broadly across the whole composition.
2. Narrowly for a specific **terminal** process (terminals are written in lowercase while non-terminals are written in PascalCase).
3. At the function level.

Let's see how we could turn the `coinGrammar` defined above into a compiled program that simply `print`s the string "heads" or "tails" as they are flipped.

```kotlin
val coinGrammar = Grammar.compose {
    "CoinFlip" {
        "heads" OR "tails"
    }

    //...
}

val program = Program(coinGrammar)
    .forFunction { name == "CoinFlip" }
    .onTerminal { println(it) }
    .compile()
```

Now, we can run this program and we will see a sequence of "heads" and "tails" appear in our console!

## Appendix: Class Hierarchy from [Music.kt](./src/main//kotlin/org/pareto/music/Music.kt)

It turns out we can represent data as `music` too. In this case, the **decision** processes (`a OR b`) correspond to a sealed class hierarchy!

```kotlin
Music {
    Sound OR silence
}

Sound {
    Dimension OR Function OR note
}

Dimension {
    Melody or harmony or choice
}

Function { 
    definition or call
}
```
