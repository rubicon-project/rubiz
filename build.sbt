name := "rubiz"
organization := "com.rubiconproject"

scalaVersion := "2.11.8"

crossScalaVersions := Seq("2.11.8", "2.12.1")

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.0" % "test",
  "org.scalacheck" %% "scalacheck" % "1.13.4" % "test",
  "org.scalaz" %% "scalaz-core" % "7.1.11",
  "org.scalaz" %% "scalaz-concurrent" % "7.1.11",
  "org.typelevel" %% "scalaz-scalatest" % "0.5.1" % "test")

coverageHighlighting := scalaBinaryVersion.value == "2.11"

// Adapted from Rob Norris' post at https://tpolecat.github.io/2014/04/11/scalac-flags.html
scalacOptions ++= Seq(
  "-language:_",
  "-deprecation",
  "-encoding", "UTF-8", // yes, this is 2 args
  "-feature",
  "-unchecked",
  "-Xfatal-warnings",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-dead-code"
)

scalacOptions ++= {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, scalaMajor)) if scalaMajor >= 11 => Seq(
      "-Ywarn-unused-import", // Not available in 2.10
      "-Ywarn-numeric-widen" // In 2.10 this produces a some strange spurious error
    )
    case _ => Seq.empty
  }
}

scalacOptions ++= {
  scalaVersion.value match {
    case "2.11.9" => Seq("-Ypartial-unification")
    case v if v startsWith "2.12" => Seq("-Ypartial-unification")
    case _ => Seq.empty
  }
}

scalacOptions in Test += "-Yrangepos"

scalacOptions in (Compile, console) ~= (_ filterNot (Set("-Xfatal-warnings", "-Ywarn-unused-import").contains))

scalacOptions in (Test, console) := (scalacOptions in (Compile, console)).value
