# process-grammar

ProcessGrammar is a high-level language for modeling processes. 

A **process** is something that... happens. It could be (and likely will be) something that happens in software. And we can model this "happening" along three dimensions:

### Dimensions of process space

1. **Time**: Processes can have steps which follow each other sequentially. In PG, we represent the process "a then b" as 
```
a > b
```
2. **Space**: Processes can have subprocesses which happen concurrently and each takes up computational space (memory). In PG we represent "a and b" as 
```
a & b
```
3. **Choice**: Processes can branch. In PG we represent "a or b" as
```
a | b
```

### Names and References

The simplest abstraction that PG affords is... naming! We can name a process as follows:
```
CoinFlip
  : heads | tails
```
Here we've modeled a `CoinFlip` process as "heads OR tails". Let's use this as the basis for two new processes!

```
FlipTwice
  : CoinFlip > CoinFlip
  
FlipTwoCoins
  : CoinFlip & CoinFlip 
```

### Self-reference
A process can run itself within itself. Let's model a heart which beats forever:

```
Heart
  : beat > Heart
```

A heart *beats* and then... it *hearts* <3

But really, this infinite repetition is a more general pattern of which "hearts" are only an example...

### Functions

``` 
Repeated(process)
  : process > Repeated(process)

Heart
  : Repeated(beat)
```

Fun!

