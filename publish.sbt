licenses := Seq("BSD-2-Clause" -> url("https://opensource.org/licenses/BSD-2-Clause"))

homepage := Some(url("http://github.com/rubicon-project/rubiz"))

publishMavenStyle := true

releaseCrossBuild := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

useGpg := true

pomExtra := (
    <scm>
      <url>git@github.com:rubicon-project/rubiz.git</url>
      <connection>scm:git@github.com:rubicon-project/rubiz.git</connection>
    </scm>
    <developers>
      {
      Seq(
        ("coltfred", "Colt Frederickson"),
        ("skeet70", "Murph Murphy")
      ).map {
        case (id, name) =>
          <developer>
            <id>{id}</id>
            <name>{name}</name>
            <url>http://github.com/{id}</url>
          </developer>
      }
    }
    </developers>
  )

import ReleaseTransformations._

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  ReleaseStep(action = Command.process("publishSigned", _), enableCrossBuild = true),
  setNextVersion,
  commitNextVersion,
  ReleaseStep(action = Command.process("sonatypeReleaseAll", _), enableCrossBuild = true),
  pushChanges
)
