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

#### ensure

### Either

#### toTask

#### toM

### Task

#### withTiming
Wraps timing information up with the result of the task. If the task
failed, there isn't a result to wrap up with, and no timing information
will be available. This is most useful for local logging of timing
information.

```scala
sql"select name from country"
  | .query[String]     // Query0[String]
  | .list              // ConnectionIO[List[String]]
  | .transact(xa)      // Task[List[String]]
  | .withTiming        // Task[FiniteDuration, List[String]]
  | .unsafePerformSync // (FiniteDuration, List[String])
  | .map {
      case (timing, result) =>
        logger.info(s"${result.length} country names were returned by the DB in ${timing.toMillis} ms.")
        result
  }
```
*example shamelessly lifted from [doobie](https://tpolecat.github.io/doobie-0.3.0/)*

#### withSideEffectTiming
Useful for side effecting the duration of a task to external services,
generally a metrics backend or logging service. This logs the duration
regardless of the success of the task.

```scala
sql"select name from country"
  |  .query[String]                         // Query0[String]
  |  .list                                  // ConnectionIO[List[String]]
  |  .transact(xa)                          // Task[List[String]]
  |  .withSideEffectTiming(metrics.update)  // Task[List[String]]
  |  .unsafePerformSync                     // List[String]
```
*example shamelessly lifted from [doobie](https://tpolecat.github.io/doobie-0.3.0/)*

#### failMap

#### attemptFold

#### peek

#### peekFail

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
