# `music.rehearse`

This package contains tools for compiling `Music` into intermediate representations.

If you wanted to assign a thematically-correct type signature to this package, it would be:


```kotlin
typealias Rehearsable<T> = (Music) -> (T) -> Any?
```


You can contrast this with the [music.perform](../perform) package, which contains tools for directly running `Music`:

```kotlin
typealias Performable = (Music) -> Any?
```