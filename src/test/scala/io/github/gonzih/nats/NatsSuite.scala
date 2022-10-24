package me.gonzih.nats

import cats.effect.IO
import cats.effect.SyncIO
import munit.CatsEffectSuite

import java.time.Duration
import scala.concurrent.duration.Duration.apply

val url = System.getenv("NATS_URL")

class NatsSuite extends CatsEffectSuite {

  test("Nats pub/sub") {
    val payload = "hello world-basic"
    val subj = "test-topic.123.hi-basic"
    Nats
      .connect(url)
      .use({ case nc =>
        for
          sub <- nc.subscribe(subj)
          _ <- nc.publish(subj, payload.getBytes)
          msg <- sub.next(Duration.ofSeconds(10))
          _ <- sub.unsubscribe
        yield assertEquals(String(msg.getData()), payload)
      })
  }

  test("JetStream Push based pub/sub") {
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

  test("JetStream Pull based pub/sub") {
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

  test("JetStream Pull based pub/sub with Duration") {
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

  test("KeyValue write/read") {
    val bucket = "test-bucket-1"
    val key = "inner-key"
    val value = "hello world 2"
    Nats
      .connect(url)
      .use({ case nc =>
        for
          bs <- nc.kvManagement.create(bucket)
          kv <- nc.kv(bucket)
          version <- kv.create(key, value.getBytes)
          ve <- kv.get(key)
          v <- ve.value
          _ <- kv.delete(key)
          _ <- kv.purgeDeletes
          _ <- nc.kvManagement.delete(bucket)
        yield assertEquals(String(v), value)
      })
  }

  test("KeyValueEntry properties test") {
    val bucket = "test-bucket-2"
    val key = "obj-key"
    val value = "hello world 3"
    Nats
      .connect(url)
      .use({ case nc =>
        for
          bs <- nc.kvManagement.create(bucket)
          kv <- nc.kv(bucket)
          _ <- kv.create(key, value.getBytes)
          ve <- kv.get(key)
          v <- ve.value
          k <- ve.key
          r <- ve.revision
          d <- ve.delta
          l <- ve.len
          _ <- kv.delete(key)
          _ <- kv.purgeDeletes
          _ <- nc.kvManagement.delete(bucket)
        yield (
          assertEquals(String(v), value),
          assertEquals(k, key),
          assertEquals(r, 1.longValue),
          assertEquals(d, 0.longValue),
          assertEquals(l, 13.longValue)
        )
      })
  }
}
