package me.gonzih.nats

import cats.effect.IO
import cats.effect.SyncIO
import munit.CatsEffectSuite

import java.time.Duration
import scala.concurrent.duration.Duration.apply

val url = System.getenv("NATS_URL")

class HelloWorldSuite extends CatsEffectSuite {

  test("Push based pub/sub") {
    val payload = "hello world"
    val stream = "test-stream"
    val subj = "test-topic.123.hi"
    val durable = "this-test-machine-123"
    Nats
      .connect(url)
      .use({ case nc =>
        for
          _ <- nc.addStream(stream, subj)
          js <- nc.js
          sub <- js.subscribe(stream, subj, durable, true)
          _ <- js.publish(subj, payload.getBytes)
          msg <- sub.take
          _ <- sub.unsubscribe
        yield assertEquals(String(msg.getData()), payload)
      })
  }

  test("Pull based pub/sub") {
    val payload = "hello world 2"
    val stream = "test-stream-pull"
    val subj = "test-topic.123.hi-pull"
    val durable = "this-test-machine-123-pull"
    Nats
      .connect(url)
      .use({ case nc =>
        for
          _ <- nc.addStream(stream, subj)
          js <- nc.js
          sub <- js.pullSubscribe(stream, subj, durable)
          _ <- js.publish(subj, payload.getBytes)
          msgs <- sub.fetch(1, 10000)
          _ <- sub.unsubscribe
        yield (
          assertEquals(msgs.length, 1),
          assertEquals(String(msgs.head.getData()), payload)
        )
      })
  }

  test("Pull based pub/sub with Duration") {
    val payload = "hello world 2"
    val stream = "test-stream-pull"
    val subj = "test-topic.123.hi-pull"
    val durable = "this-test-machine-123-pull"
    Nats
      .connect(url)
      .use({ case nc =>
        for
          _ <- nc.addStream(stream, subj)
          js <- nc.js
          sub <- js.pullSubscribe(stream, subj, durable)
          _ <- js.publish(subj, payload.getBytes)
          msgs <- sub.fetch(1, Duration.ofSeconds(10))
          _ <- sub.unsubscribe
        yield (
          assertEquals(msgs.length, 1),
          assertEquals(String(msgs.head.getData()), payload)
        )
      })
  }
}
