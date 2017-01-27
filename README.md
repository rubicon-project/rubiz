Rubiz
=======================
[![Build Status](https://travis-ci.org/rubicon-project/rubiz.svg?branch=master)](https://travis-ci.org/rubicon-project/rubiz)
[![codecov.io](https://codecov.io/github/rubicon-project/rubiz/coverage.svg?branch=master)](https://codecov.io/github/rubicon-project/rubiz?branch=master)
[![scaladoc](https://javadoc-badge.appspot.com/com.rubiconproject/rubiz_2.11.svg?label=scaladoc)](https://javadoc-badge.appspot.com/com.rubiconproject/rubiz_2.11)

## Table of Contents
* [Purpose](#purpose)
* [Installation](#installation)
* [Additions](#additions)
  * [Catchable](#catchable)
  * [Task](#task)
  * [Either](#either)
  * [Try](#try)
* [Tests](#tests)
* [Continuous Integration](#continuous-integration)
* [Release](#release)

## Purpose

Provides much needed syntax and missing conversions for Scalaz 7.1.

## Installation

Add the following to your `build.sbt` file:

```
libraryDependencies += "com.rubiconproject" %% "rubiz" % "0.3.+"
```

Import all the additional syntax with `import rubiz.syntax.all._`. Specific subsets of syntax can be
imported if preferred (eg, `import rubiz.syntax.either._`).

## Additions

### Catchable

```scala
import scalaz.Catchable
import scalaz.syntax.catchable._
import scalaz.effect.IO
import rubiz.syntax.catchable._
```

#### ensure
Check the result inside `Catchable` to see if it matches your predicate. If it doesn't, the left becomes your provided value.

```scala
(IO("username")
  .ensure(new Exception("Can't make a user without a name."))(_.nonEmpty)
  .unsafePerformIO)
// res0: String = username
```

### Task

```scala
import scalaz.concurrent.Task
import rubiz.syntax.task._
import scala.concurrent.duration._

```

#### withTiming
Wraps timing information up with the result of the task. If the task
failed, there isn't a result to wrap up with, and no timing information
will be available. This is most useful for local logging of timing
information.

```scala
(Task.delay(List("Australia", "Japan"))
  .withTiming           // Task[(FiniteDuration, List[String])]
  .map {
      case (timing, result) =>
        println(s"${result.length} country names were returned in ${timing.toMillis} ms.")
        result
  }
  .run)
// 2 country names were returned in 1 ms.
// res2: List[String] = List(Australia, Japan)
```

#### withSideEffectTiming
Useful for side effecting the duration of a task to external services,
generally a metrics backend or logging service. This logs the duration
regardless of the success of the task.

```scala
(Task.delay(List("hello", "world"))
  .withSideEffectTiming(timing => println(s"${timing.toMillis} ms run, to the metrics service!"))  // Task[List[String]]
  .run)
// 10 ms run, to the metrics service!
// res3: List[String] = List(hello, world)
```

#### labeledTimeout
Apply a timeout of `time` to `t`; if the timeout occurs, the resulting TimeoutException includes a message including `label` and `time`.
Like `scalaz.concurrent.Task.timed` but with a non-null, useful error message in the exception.

```scala
(Task.delay(Thread.sleep(100.millis.toMillis))
  .labeledTimeout(2.millis, "silly example")
  .attemptRun)
// res4: scalaz.\/[Throwable,Unit] = -\/(java.util.concurrent.TimeoutException: The 'silly example' task timed out after 2 milliseconds.)
```

#### failMap
`leftMap` for a `Task`. Useful for reporting a different exception than the one actually created by the
failure.

```scala
(Task.fail(new Exception("Esoteric nonsense."))
  .failMap(_ => new Exception("Contextual description of what happened."))
  .attemptRun)
// res5: scalaz.\/[Throwable,Nothing] = -\/(java.lang.Exception: Contextual description of what happened.)
```

#### attemptFold
Allows you to handle errors and map the successes to a new value.

```scala
(Task.now("Success")
  .attemptFold(_ => "Failure")(_ ++ "es")
  .run)
// res6: String = Successes
```

```scala
(Task.delay[String](throw new Exception("Explosion"))
  .attemptFold(_ => "The explosion was contained.")(_ ++ "es")
  .run)
// res7: String = The explosion was contained.
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
// res8: Boolean = true
```

#### peekFail
Run your function as a side effect if the task fails and pass the original Throwable through. Particularly useful for
logging.

```scala
(Task.delay[Boolean](throw new Exception("I can't search this list!"))
  .peekFail(_ => println("What is an element, really?"))
  .attemptRun)
// What is an element, really?
// res9: scalaz.\/[Throwable,Boolean] = -\/(java.lang.Exception: I can't search this list!)
```

### Either

```scala
import scalaz.\/
import scalaz.syntax.either._
import rubiz.syntax.either._
```

#### toTask
Turns your `\/[Throwable, A]` into a `Task[A]`.
Useful when you're trying to compose `Tasks` and you want to mix in an `Either`.

```scala
(List("USA", "Canada")
  .right[Throwable]     // \/[Throwable, List[String]]
  .toTask               // Task[List[String]]
  .run)
// res10: List[String] = List(USA, Canada)
```

#### toM
Allows you to convert an `Either` to any `Monad` that has an `Applicative` and `Catchable` instance.
This operates like `toTask` but is more generic.

```scala
// import scalaz.concurrent.Task
("Some Name"
  .right[Throwable] // \/[Throwable, String]
  .toM[Task]        // Task[String]
  .run)
// res12: String = Some Name

// import scalaz.effect.IO
(new Exception("Users do bad things")
  .left[String] // \/[Throwable, String]
  .toM[IO]      // IO[String]
  .attempt      // IO[\/[Throwable, String]]
  .unsafePerformIO)
// res14: scalaz.\/[Throwable,String] = -\/(java.lang.Exception: Users do bad things)
```

### Try
`try` is a reserved word, so we've resorted to backticks. If you've got an alternative suggestion,
we'd love to hear it.

```scala
import scala.util.Try
import rubiz.syntax.`try`._
```

#### toDisjunction
If you're using Scalaz you'd probably rather be working with an `Either`/`\/`/`Disjunction` than
a `Try`.

```scala
val badTry = Try(throw new Exception("No really, users."))
// badTry: scala.util.Try[Nothing] = Failure(java.lang.Exception: No really, users.)

badTry.toDisjunction
// res15: scalaz.\/[Throwable,Nothing] = -\/(java.lang.Exception: No really, users.)
```

#### toTask
If you're in streams-land and want to go directly from a `Try` to a `Task`, this sugars you on over
there. Useful when using non-Scalaz libs with Scalaz streams.

```scala
val okTry = Try("My examples get worse as time goes on")
// okTry: scala.util.Try[String] = Success(My examples get worse as time goes on)

okTry.toTask.run
// res16: String = My examples get worse as time goes on
```

## Tests

Run `sbt test` to build the project and exercise the unit test suite.

## Continuous Integration

[TravisCI](https://travis-ci.org/rubicon-project/rubiz) builds on every published commit on every branch.

## Release

Rubiz is versioned with [semver](http://semver.org/) and released to [sonatype](https://oss.sonatype.org/).
Open an issue on Github if you feel there should be a release published that hasn't been. You can run `sbt tut`
to regenerate documentation locally if you modify the docs in the `/main/tut` directory.

For those with permission to release:
* Install `gpg` and [generate a key](https://www.gnupg.org/gph/en/manual.html#AEN26). Upload that key to a [public key server](http://pgp.mit.edu/).
    * Mac users can `brew install gpg pinentry-mac` to get the tools needed.
* Create a Sonatype [credentials file](http://www.scala-sbt.org/1.0/docs/Using-Sonatype.html#Fourth+-+Adding+credentials).
* Run `sbt release`
* Push the newly created tags and version bump commits to `rubicon-project/rubiz`. 
