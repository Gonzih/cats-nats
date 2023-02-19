ThisBuild / organization := "io.github.gonzih"
ThisBuild / organizationHomepage := Some(url("https://github.com/Gonzih"))
ThisBuild / scalaVersion := "3.2.1"
ThisBuild / versionScheme := Some("early-semver")
ThisBuild / licenses := Seq(
  "APL2" -> url("https://www.apache.org/licenses/LICENSE-2.0.txt")
)
ThisBuild / version := "0.1.0"

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/Gonzih/cats-nats"),
    "scm:git@github.com:Gonzih/cats-nats.git"
  )
)
ThisBuild / developers := List(
  Developer(
    id = "Gonzih",
    name = "Max Soltan",
    email = "gonzih@gmail.com",
    url = url("https://gonzih.me/")
  )
)

ThisBuild / description := "Cats friendly wrapper around NATS client"
ThisBuild / homepage := Some(url("https://github.com/Gonzih/cats-nats"))

ThisBuild / pomIncludeRepository := { _ => false }
ThisBuild / publishTo := {
  val nexus = "https://s01.oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}
ThisBuild / publishMavenStyle := true

Global / excludeLintKeys ++= Set(pomIncludeRepository, publishMavenStyle)
// publishConfiguration := publishConfiguration.value.withOverwrite(true)
// publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true)

lazy val root = (project in file(".")).settings(
  name := "cats-nats",
  libraryDependencies ++= Seq(
    "io.nats" % "jnats" % "2.16.5",
    // "core" module - IO, IOApp, schedulers
    // This pulls in the kernel and std modules automatically.
    "org.typelevel" %% "cats-effect" % "3.4.7",
    // concurrency abstractions and primitives (Concurrent, Sync, Async etc.)
    "org.typelevel" %% "cats-effect-kernel" % "3.4.7",
    // standard "effect" library (Queues, Console, Random etc.)
    "org.typelevel" %% "cats-effect-std" % "3.4.7",
    "org.typelevel" %% "munit-cats-effect-3" % "1.0.7" % Test
  )
)
