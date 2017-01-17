Rubiz
=======================
[![Build Status](https://travis-ci.org/rubicon-project/rubiz.svg?branch=master)](https://travis-ci.org/rubicon-project/rubiz)
[![codecov.io](https://codecov.io/github/rubicon-project/rubiz/coverage.svg?branch=master)](https://codecov.io/github/rubicon-project/rubiz?branch=master)
[![scaladoc](https://javadoc-badge.appspot.com/com.rubiconproject/rubiz_2.11.svg?label=scaladoc)](https://javadoc-badge.appspot.com/com.rubiconproject/rubiz_2.11)


## Purpose

Provides much needed syntax and missing conversions for Scalaz 7.1.

## Installation

Add the following to your `build.sbt` file:

```
libraryDependencies += "com.rubiconproject" %% "rubiz" % "0.2.+"
```

Import all the additional syntax with `import rubiz.syntax.all._`. Specific subsets of syntax can be
imported if preferred (eg, `import rubiz.syntax.either._`).

## Additions

### Catchable

```scala
import scalaz.Catchable
import rubiz.syntax.catchable._
```

#### ensure



### Either

#### toTask

#### toM

### Task

```scala
import scalaz.concurrent.Task
import rubiz.syntax.task._
```

#### withTiming
Wraps timing information up with the result of the task. If the task
failed, there isn't a result to wrap up with, and no timing information
will be available. This is most useful for local logging of timing
information.

```scala
(Task.now(List("Australia", "Japan"))
  .withTiming           // Task[(FiniteDuration, List[String])]
  .map {
      case (timing, result) =>
        println(s"${result.length} country names were returned by the DB in ${timing.toMillis} ms.")
        result
  }
  .run)
// 2 country names were returned by the DB in 1 ms.
// res0: List[String] = List(Australia, Japan)
```

#### withSideEffectTiming
Useful for side effecting the duration of a task to external services,
generally a metrics backend or logging service. This logs the duration
regardless of the success of the task.

```scala
(Task.now(List("hello", "world"))
  .withSideEffectTiming(timing => println(s"${timing.toMillis} ms run to the metrics service!"))  // Task[List[String]]
  .run)
// 20 ms run to the metrics service!
// res1: List[String] = List(hello, world)
```

#### failMap
`leftMap` for a `Task`. Useful for reporting a different exception than the one actually created by the
failure.

```scala
(Task.fail(new Exception("Esoteric nonsense."))
  .failMap(_ => new Exception("Contextual description of what happened."))
  .attemptRun)
// res2: scalaz.\/[Throwable,Nothing] = -\/(java.lang.Exception: Contextual description of what happened.)
```

#### attemptFold
Allows you to handle errors and map the successes to a new value.

```scala
(Task.now("Success")
  .attemptFold(_ => "Failure")(_ ++ "es")
  .run)
// res3: String = Successes
```

```scala
(Task.delay[String](throw new Exception("Explosion"))
  .attemptFold(_ => "The explosion was contained.")(_ ++ "es")
  .run)
// res4: String = The explosion was contained.
```

#### peek
Run your function as a side effect if the task is successful and pass the original return value through. Particularly
useful for logging.

```scala
(Task.now(true)
  .peek(b => b match {
    case true => println("Element was found.")
    case false => println("Element wasn't found.")
  })
  .run)
// Element was found.
// res5: Boolean = true
```

#### peekFail
Run your function as a side effect if the task fails and pass the original Throwable through. Particularly useful for
logging.

```scala
(Task.delay[Boolean](throw new Exception("I can't search this list!"))
  .peekFail(_ => println("What is an element, really?"))
  .attemptRun)
// What is an element, really?
// res6: scalaz.\/[Throwable,Boolean] = -\/(java.lang.Exception: I can't search this list!)
```

### Try

#### toDisjunction

#### toTask

## Tests

Run `sbt test` to build the project and exercise the unit test suite.

## Continuous Integration

[TravisCI](https://travis-ci.org/rubicon-project/rubiz) builds on every published commit on every branch.

## Release

Rubiz is versioned with [semver](http://semver.org/) and released to [sonatype](https://oss.sonatype.org/).
Open an issue on Github if you feel there should be a release published that hasn't been.
