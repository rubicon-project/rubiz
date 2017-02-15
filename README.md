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
libraryDependencies += "com.rubiconproject" %% "rubiz" % "0.4.+"
```

Import all the additional syntax with `import rubiz.syntax.all._`. Specific subsets of syntax can be
imported if preferred (eg, `import rubiz.syntax.either._`).

## Additions

### Catchable

```scala
import scalaz.Catchable
import scalaz.syntax.catchable._
import scalaz.effect.IO
import scalaz.concurrent.Task
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

#### attemptSome
`attempt`, but it will only catch/map throwables for which the function is defined. If the function
doesn't match for the throwable it will re-throw. A common use case is to map IO (user or DB)
exceptions that you have a better type or message for on the left, rethrowing ones you didn't expect.

```scala
(IO(throw new java.sql.SQLException).attemptSome {
    case sqlE: java.sql.SQLException => "Computer says no."
  }
  .unsafePerformIO)
// res1: scalaz.\/[String,Nothing] = -\/(Computer says no.)
```

#### except
Lets you define an exception handler on the Catchable that maintains the same type.

```scala
(IO[Int](throw new IllegalArgumentException)
  .except(e => IO(0))
  .unsafePerformIO)
// res2: Int = 0
```

#### exceptSome
Like [`except`](#except) but only executes where the function is defined. Has similar use cases to 
[`attemptSome`](#attemptSome), but when you have a default you want to use instead of a Throwable
transformation.

```scala
(IO[Int](throw new java.sql.SQLException).exceptSome {
    case sqlE: java.sql.SQLException => IO(0)
  }
  .unsafePerformIO)
// res3: Int = 0
```

#### onException
Like `finally`, but only runs when there was an exception.

```scala
(try {
  Task.delay(throw new Exception())
    .onException(Task.delay(println("THERE WAS A FIREFIGHT!")))
    .run
} catch {
  case _: Throwable => println("Or something.")
})
// THERE WAS A FIREFIGHT!
// Or something.
```

#### ensuring
Generalizes `finally` for all `Catchable`, not just `IO`.

```scala
(try {
  Task.delay(throw new Exception())
    .ensuring(Task.delay(println("THERE WAS A FIREFIGHT!")))
    .run
} catch {
  case _: Throwable => println("Or something.")
})
// THERE WAS A FIREFIGHT!
// Or something.

(Task.delay(0)
  .ensuring(Task.delay(println("THERE WAS A FIREFIGHT!")))
  .run)
// THERE WAS A FIREFIGHT!
// res6: Int = 0
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
// res8: List[String] = List(Australia, Japan)
```

#### withSideEffectTiming
Useful for side effecting the duration of a task to external services,
generally a metrics backend or logging service. This logs the duration
regardless of the success of the task.

```scala
(Task.delay(List("hello", "world"))
  .withSideEffectTiming(timing => println(s"${timing.toMillis} ms run, to the metrics service!"))  // Task[List[String]]
  .run)
// 4 ms run, to the metrics service!
// res9: List[String] = List(hello, world)
```

#### labeledTimeout
Apply a timeout of `time` to `t`; if the timeout occurs, the resulting TimeoutException includes a message including `label` and `time`.
Like `scalaz.concurrent.Task.timed` but with a non-null, useful error message in the exception.

```scala
(Task.delay(Thread.sleep(100.millis.toMillis))
  .labeledTimeout(2.millis, "silly example")
  .attemptRun)
// res10: scalaz.\/[Throwable,Unit] = -\/(java.util.concurrent.TimeoutException: The 'silly example' task timed out after 2 milliseconds.)
```

#### failMap
`leftMap` for a `Task`. Useful for reporting a different exception than the one actually created by the
failure.

```scala
(Task.fail(new Exception("Esoteric nonsense."))
  .failMap(_ => new Exception("Contextual description of what happened."))
  .attemptRun)
// res11: scalaz.\/[Throwable,Nothing] = -\/(java.lang.Exception: Contextual description of what happened.)
```

#### attemptFold
Allows you to handle errors and map the successes to a new value.

```scala
(Task.now("Success")
  .attemptFold(_ => "Failure")(_ ++ "es")
  .run)
// res12: String = Successes
```

```scala
(Task.delay[String](throw new Exception("Explosion"))
  .attemptFold(_ => "The explosion was contained.")(_ ++ "es")
  .run)
// res13: String = The explosion was contained.
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
// res14: Boolean = true
```

#### peekFail
Run your function as a side effect if the task fails and pass the original Throwable through. Particularly useful for
logging.

```scala
(Task.delay[Boolean](throw new Exception("I can't search this list!"))
  .peekFail(_ => println("What is an element, really?"))
  .attemptRun)
// What is an element, really?
// res15: scalaz.\/[Throwable,Boolean] = -\/(java.lang.Exception: I can't search this list!)
```

#### using
Ensure that a resource is "closed" when a task completes, regardless of whether it's successful.
A [`CanClose` instance](src/main/scala/rubiz/CanClose.scala) must be scope to call `using`.
If the object to be closed isn't [`java.io.Closeable`](https://docs.oracle.com/javase/8/docs/api/java/io/Closeable.html) then you'll need to define a `CanClose` instance.

```scala
class CloseableThing extends java.io.Closeable { def close: Unit = println("Not so fast! I have been closed.") }
// defined class CloseableThing

Task.delay(new CloseableThing).using { closeableThing =>
  throw new Exception("All your resources are lost to chaos")
}.attemptRun
// Not so fast! I have been closed.
// res16: scalaz.\/[Throwable,Nothing] = -\/(java.lang.Exception: All your resources are lost to chaos)
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
// res17: List[String] = List(USA, Canada)
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
// res19: String = Some Name

// import scalaz.effect.IO
(new Exception("Users do bad things")
  .left[String] // \/[Throwable, String]
  .toM[IO]      // IO[String]
  .attempt      // IO[\/[Throwable, String]]
  .unsafePerformIO)
// res21: scalaz.\/[Throwable,String] = -\/(java.lang.Exception: Users do bad things)
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
// res22: scalaz.\/[Throwable,Nothing] = -\/(java.lang.Exception: No really, users.)
```

#### toTask
If you're in streams-land and want to go directly from a `Try` to a `Task`, this sugars you on over
there. Useful when using non-Scalaz libs with Scalaz streams.

```scala
val okTry = Try("My examples get worse as time goes on")
// okTry: scala.util.Try[String] = Success(My examples get worse as time goes on)

okTry.toTask.run
// res23: String = My examples get worse as time goes on
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
