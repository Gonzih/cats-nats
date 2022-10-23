package me.gonzih.nats

import cats.effect.IO
import cats.effect.SyncIO
import munit.CatsEffectSuite

val url = System.getenv("NATS_URL")

class HelloWorldSuite extends CatsEffectSuite {

  test("simple pub sub") {
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
        yield assertEquals(String(msg.getData()), payload)
      })
  }
}
