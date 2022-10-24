ThisBuild / organization := "me.gonzih"
ThisBuild / scalaVersion := "3.2.0"

lazy val root = (project in file(".")).settings(
  name := "cats-nats",
  version := "0.1.0-SNAPSHOT"
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
