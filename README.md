![Cats went Nats](/cats-nats.jpeg)

| CI | Release | Snapshot |
| --- | --- | --- |
| ![CI][Badge-CI] | [![Release Artifacts][Badge-SonatypeReleases]][Link-SonatypeReleases] | [![Snapshot Artifacts][Badge-SonatypeSnapshots]][Link-SonatypeSnapshots] |

[Badge-CI]: https://github.com/zio/zio/workflows/CI/badge.svg
[Badge-SonatypeReleases]: https://img.shields.io/nexus/r/https/s01.oss.sonatype.org/io.github.gonzih/cats-nats_3.svg "Sonatype Releases"
[Badge-SonatypeSnapshots]: https://img.shields.io/nexus/s/https/s01.oss.sonatype.org/io.github.gonzih/cats-nats_3.svg "Sonatype Snapshots"
[Link-SonatypeReleases]: https://s01.oss.sonatype.org/content/repositories/releases/io/github/gonzih/cats-nats_3/ "Sonatype Releases"
[Link-SonatypeSnapshots]: https://s01.oss.sonatype.org/content/repositories/snapshots/io/github/gonzih/cats-nats_3/ "Sonatype Snapshots"


# Cats Nats

Cats Effect friendly wrapper for Nats

## Usage

Add **cats-nats** to your `build.sbt` `libraryDependencies` with

```sbt
"io.github.gonzih" %% "cats-nats" % "0.1.0"
```

Then just use it:

```scala
import cats.effect.IOApp
import cats.effect.IO
import io.github.gonzih.nats.Nats

object Main extends IOApp.Simple {

  def run: IO[Unit] =
    val payload = "hello world"
    val stream = "my-persistent-stream"
    val subj = "my-subject"
    val durable = "my-durable-id"

    // connect to nats via Cats Effect Resource
    Nats
      .connect("nats://localhost:4222")
      .use({ case nc =>
        for
          // stream creation can be done externally or via cats-nats API
          _ <- nc.addStream(stream, subj)
          // create JetStream instance
          js <- nc.js
          // subscribe to subject on stream, this is backed by unbound Cats Effect Queue
          sub <- js.subscribe(stream, subj, durable, true)
          // publish your message
          _ <- js.publish(subj, payload.getBytes)
          // wait for message to be received
          msg <- sub.take
          // unsubscribe
          _ <- sub.unsubscribe
          // print result
          IO.println(String(msg.getData()))
        yield ()
      })
}
```


Key Value storage example:


```scala
import cats.effect.IOApp
import cats.effect.IO
import io.github.gonzih.nats.Nats

object Main extends IOApp.Simple {

  def run: IO[Unit] =
    val bucket = "kv-bucket"
    val key = "kv-object-key"
    val value = "object contents"

    // connect to nats via Cats Effect Resource
    Nats
      .connect("nats://localhost:4222")
      .use({ case nc =>
        for
          // create bucket
          __<- nc.kvManagement.create(bucket)
          // get bucket KV instance
          kv <- nc.kv(bucket)
          // create new object
          version <- kv.create(key, value.getBytes)
          // read object by key
          ve <- kv.get(key)
          // read content of an object
          v <- ve.value
          // delete object by key
          _ <- kv.delete(key)
          // purge deletes in bucket
          _ <- kv.purgeDeletes
          // delete bucket
          _ <- nc.kvManagement.delete(bucket)
          // print result
          _ <-_IO.println(String(v))
        yield ()
      })
}
```
