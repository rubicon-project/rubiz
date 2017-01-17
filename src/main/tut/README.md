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

```tut:silent
import scalaz.Catchable
import scalaz.syntax.catchable._
import scalaz.effect.IO
import rubiz.syntax.catchable._
```

#### ensure
Check the result inside `Catchable` to see if it matches your predicate. If it doesn't, the left becomes your provided value.

```tut:book
(IO("username")
  .ensure(new Exception("Can't make a user without a name."))(_.nonEmpty)
  .unsafePerformIO)
```

### Task

```tut:silent
import scalaz.concurrent.Task
import rubiz.syntax.task._
```

#### withTiming
Wraps timing information up with the result of the task. If the task
failed, there isn't a result to wrap up with, and no timing information
will be available. This is most useful for local logging of timing
information.

```tut:book
(Task.now(List("Australia", "Japan"))
  .withTiming           // Task[(FiniteDuration, List[String])]
  .map {
      case (timing, result) =>
        println(s"${result.length} country names were returned by the DB in ${timing.toMillis} ms.")
        result
  }
  .run)
```

#### withSideEffectTiming
Useful for side effecting the duration of a task to external services,
generally a metrics backend or logging service. This logs the duration
regardless of the success of the task.

```tut:book
(Task.now(List("hello", "world"))
  .withSideEffectTiming(timing => println(s"${timing.toMillis} ms run to the metrics service!"))  // Task[List[String]]
  .run)
```

#### failMap
`leftMap` for a `Task`. Useful for reporting a different exception than the one actually created by the
failure.

```tut:book
(Task.fail(new Exception("Esoteric nonsense."))
  .failMap(_ => new Exception("Contextual description of what happened."))
  .attemptRun)
```

#### attemptFold
Allows you to handle errors and map the successes to a new value.

```tut:book
(Task.now("Success")
  .attemptFold(_ => "Failure")(_ ++ "es")
  .run)
```

```tut:book
(Task.delay[String](throw new Exception("Explosion"))
  .attemptFold(_ => "The explosion was contained.")(_ ++ "es")
  .run)
```

#### peek
Run your function as a side effect if the task is successful and pass the original return value through. Particularly
useful for logging.

```tut:book
(Task.now(true)
  .peek(b => b match {
    case true => println("Element was found.")
    case false => println("Element wasn't found.")
  })
  .run)
```

#### peekFail
Run your function as a side effect if the task fails and pass the original Throwable through. Particularly useful for
logging.

```tut:book
(Task.delay[Boolean](throw new Exception("I can't search this list!"))
  .peekFail(_ => println("What is an element, really?"))
  .attemptRun)
```

### Either

```tut:silent
import scalaz.\/
import scalaz.syntax.either._
import rubiz.syntax.either._
```

#### toTask
Turns your `\/[Throwable, A]` into a `Task[A]`.
Useful when you're trying to compose `Tasks` and you want to mix in an `Either`.

```tut:book
(List("USA", "Canada")
  .right[Throwable]     // \/[Throwable, List[String]]
  .toTask               // Task[List[String]]
  .run)
```

#### toM
Allows you to convert an `Either` to any `Monad` that has an `Applicative` and `Catchable` instance.
This operates like `toTask` but is more generic.

```tut:book
// import scalaz.concurrent.Task
("Some Name"
  .right[Throwable] // \/[Throwable, String]
  .toM[Task]        // Task[String]
  .run)
  
// import scalaz.effect.IO
(new Exception("Users do bad things")
  .left[String] // \/[Throwable, String]
  .toM[IO]      // IO[String]
  .attempt      // IO[\/[Throwable, String]]
  .unsafePerformIO)
```

### Try
`try` is a reserved word, so we've resorted to backticks. If you've got an alternative suggestion,
we'd love to hear it.

```tut:silent
import scala.util.Try
import rubiz.syntax.`try`._
```

#### toDisjunction
If you're using Scalaz you'd probably rather be working with an `Either`/`\/`/`Disjunction` than
a `Try`.

```tut:book
val badTry = Try(throw new Exception("No really, users."))

badTry.toDisjunction
```

#### toTask
If you're in streams-land and want to go directly from a `Try` to a `Task`, this sugars you on over
there. Useful when using non-Scalaz libs with Scalaz streams.

```tut:book
val okTry = Try("My examples get worse as time goes on")

okTry.toTask.run
```

## Tests

Run `sbt test` to build the project and exercise the unit test suite.

## Continuous Integration

[TravisCI](https://travis-ci.org/rubicon-project/rubiz) builds on every published commit on every branch.

## Release

Rubiz is versioned with [semver](http://semver.org/) and released to [sonatype](https://oss.sonatype.org/).
Open an issue on Github if you feel there should be a release published that hasn't been.
