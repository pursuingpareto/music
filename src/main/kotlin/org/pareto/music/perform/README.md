# `music.perform`

This package contains tools for directly running (or "performing") `music` programs without first compiling them.

If you wanted to assign a thematically-correct type signature to this package, it would be:

```kotlin
typealias Performable = (Music) -> Any?
```

You can contrast this with the [music.rehearse](../rehearse) package, which contains an intermediate compilation step before running `music`:

```kotlin
typealias Rehearsable<T> = (Music) -> (T) -> Any?
```