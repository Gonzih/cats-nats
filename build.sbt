ThisBuild / organization := "me.gonzih"
ThisBuild / organizationHomepage := Some(url("https://gonzih.me/"))
ThisBuild / scalaVersion := "3.1.3"
ThisBuild / versionScheme := Some("early-semver")
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / licenses := Seq("APL2" -> url("https://www.apache.org/licenses/LICENSE-2.0.txt"))

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

lazy val root = (project in file(".")).settings(
  name := "cats-nats",
  libraryDependencies ++= Seq(
    "io.nats" % "jnats" % "2.16.1",
    // "core" module - IO, IOApp, schedulers
    // This pulls in the kernel and std modules automatically.
    "org.typelevel" %% "cats-effect" % "3.3.14",
    // concurrency abstractions and primitives (Concurrent, Sync, Async etc.)
    "org.typelevel" %% "cats-effect-kernel" % "3.3.14",
    // standard "effect" library (Queues, Console, Random etc.)
    "org.typelevel" %% "cats-effect-std" % "3.3.14",
    "org.typelevel" %% "munit-cats-effect-3" % "1.0.7" % Test
  )
)
