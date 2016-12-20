Rubiz
=======================

## Purpose

Provides much needed syntax and missing conversions for Scalaz 7.1.

## Adding to sbt

Add the following to your `build.sbt` file:
```
libraryDependencies += "com.rubiconproject" %% "rubiz" % "0.2.0"
```

Import all the additional syntax with `import rubiz.syntax.all._`. Specific subsets of syntax can be
imported if preferred (eg, `import rubiz.syntax.either._`).

### Running tests

Run `sbt test` to build the project and exercise the unit test suite.

### Continuous Integration

[TravisCI]() builds on every published commit on every branch.

### Release

Rubiz is versioned with [semver](http://semver.org/) and released to [sonatype](https://oss.sonatype.org/). Open an issue on Github if you feel there should be a release published that hasn't been.
