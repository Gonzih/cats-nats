package me.gonzih.nats

import cats.effect.IO
import cats.effect.Resource
import cats.effect.std.Queue
import cats.effect.unsafe.implicits.global
import io.nats.client.Connection
import io.nats.client.Dispatcher
import io.nats.client.JetStream
import io.nats.client.JetStreamSubscription
import io.nats.client.KeyValue
import io.nats.client.KeyValueManagement
import io.nats.client.KeyValueOptions
import io.nats.client.Message
import io.nats.client.MessageHandler
import io.nats.client.PublishOptions
import io.nats.client.PullSubscribeOptions
import io.nats.client.PushSubscribeOptions
import io.nats.client.Subscription
import io.nats.client.api.KeyValueConfiguration
import io.nats.client.api.KeyValueEntry
import io.nats.client.api.KeyValueStatus
import io.nats.client.api.StorageType
import io.nats.client.api.StreamConfiguration
import io.nats.client.{Nats => JClient}

import java.time.Duration
import java.time.ZonedDateTime
import scala.collection.JavaConverters._

def f2messageHandler(f: Message => Unit): MessageHandler =
  new MessageHandler:
    override def onMessage(msg: Message): Unit =
      f(msg)

class NatsPushSubscription(
    sub: JetStreamSubscription,
    q: Queue[IO, Message],
    dispatcher: Dispatcher
):
  def unsubscribe: IO[Unit] =
    IO.blocking(dispatcher.unsubscribe(sub))

  def take: IO[Message] =
    q.take
end NatsPushSubscription

class NatsPullSubscription(sub: JetStreamSubscription):
  def unsubscribe: IO[Unit] =
    IO.blocking(sub.unsubscribe())

  def fetch(batchsize: Int, wait: Long): IO[List[Message]] =
    IO.blocking(sub.fetch(batchsize, wait).asScala.toList)

  def fetch(batchsize: Int, wait: Duration): IO[List[Message]] =
    IO.blocking(sub.fetch(batchsize, wait).asScala.toList)
end NatsPullSubscription

class NatsJetStream(nc: Connection, js: JetStream):
  def publish(msg: Message): IO[Unit] =
    IO.blocking(js.publish(msg))

  def publish(msg: Message, po: PublishOptions): IO[Unit] =
    IO.blocking(js.publish(msg, po))

  def publish(subject: String, body: Array[Byte]): IO[Unit] =
    IO.blocking(js.publish(subject, body))

  def publish(
      subject: String,
      body: Array[Byte],
      po: PublishOptions
  ): IO[Unit] =
    IO.blocking(js.publish(subject, body, po))

  def subscribe(
      stream: String,
      subject: String,
      durable: String,
      autoack: Boolean
  ): IO[NatsPushSubscription] =
    val so = PushSubscribeOptions
      .builder()
      .stream(stream)
      .durable(durable)
      .build()
    subscribe(stream, subject, durable, autoack, so)

  def subscribe(
      stream: String,
      subject: String,
      durable: String,
      autoack: Boolean,
      so: PushSubscribeOptions
  ): IO[NatsPushSubscription] =
    for
      dispatcher <- IO.pure(nc.createDispatcher)
      q <- Queue.unbounded[IO, Message]
      sub <- IO(
        js.subscribe(
          subject,
          dispatcher,
          f2messageHandler(msg => q.offer(msg).unsafeRunSync()),
          autoack,
          so
        )
      )
    yield NatsPushSubscription(sub, q, dispatcher)

  def pullSubscribe(
      stream: String,
      subject: String,
      durable: String
  ): IO[NatsPullSubscription] =
    val so = PullSubscribeOptions
      .builder()
      .stream(stream)
      .durable(durable)
      .build()
    pullSubscribe(stream, subject, durable, so)

  def pullSubscribe(
      stream: String,
      subject: String,
      durable: String,
      so: PullSubscribeOptions
  ): IO[NatsPullSubscription] =
    for sub <- IO(js.subscribe(subject, so))
    yield NatsPullSubscription(sub)
end NatsJetStream

class NatsConnection(nc: Connection):
  def js: IO[NatsJetStream] =
    IO(NatsJetStream(nc, nc.jetStream()))

  def addStream(
      stream: String,
      subject: String
  ): IO[Unit] =
    val sc = StreamConfiguration
      .builder()
      .name(stream)
      .subjects(subject)
      .storageType(StorageType.Memory)
      .build()
    addStream(stream, subject, sc)

  def addStream(
      stream: String,
      subject: String,
      sc: StreamConfiguration
  ): IO[Unit] =
    val jsm = nc.jetStreamManagement()
    IO.blocking(jsm.addStream(sc))

  def publish(msg: Message): IO[Unit] =
    IO.blocking(nc.publish(msg))

  def publish(subject: String, msg: Array[Byte]): IO[Unit] =
    IO.blocking(nc.publish(subject, msg))

  def publish(subject: String, replyTo: String, msg: Array[Byte]): IO[Unit] =
    IO.blocking(nc.publish(subject, replyTo, msg))

  def subscribe(subject: String): IO[NatsSubscription] =
    IO.blocking(NatsSubscription(nc.subscribe(subject)))

  def subscribe(subject: String, queue: String): IO[NatsSubscription] =
    IO.blocking(NatsSubscription(nc.subscribe(subject, queue)))

  def kv(bucket: String): IO[NatsKeyValue] =
    IO.blocking(NatsKeyValue(nc.keyValue(bucket)))

  def kv(bucket: String, kvo: KeyValueOptions): IO[NatsKeyValue] =
    IO.blocking(NatsKeyValue(nc.keyValue(bucket, kvo)))

  def kvManagement: NatsKeyValueManagement =
    NatsKeyValueManagement(nc.keyValueManagement())

  def kvManagement(kvo: KeyValueOptions): NatsKeyValueManagement =
    NatsKeyValueManagement(nc.keyValueManagement(kvo))

  def close: IO[Unit] =
    IO.blocking(nc.close())
end NatsConnection

class NatsKeyValueManagement(kvm: KeyValueManagement):
  def create(bucket: String): IO[KeyValueStatus] =
    val kvc = KeyValueConfiguration.builder().name(bucket).build()
    create(kvc)

  def create(kvc: KeyValueConfiguration): IO[KeyValueStatus] =
    IO.blocking(kvm.create(kvc))

  def delete(bucket: String): IO[Unit] =
    IO.blocking(kvm.delete(bucket))

  def update(kvc: KeyValueConfiguration): IO[KeyValueStatus] =
    IO.blocking(kvm.update(kvc))

  def status(bucket: String): IO[KeyValueStatus] =
    IO.blocking(kvm.getStatus(bucket))

  def buckets: IO[List[String]] =
    IO.blocking(kvm.getBucketNames().asScala.toList)

  def statuses: IO[List[KeyValueStatus]] =
    IO.blocking(kvm.getStatuses().asScala.toList)

end NatsKeyValueManagement

class NatsKeyValue(kv: KeyValue):
  def create(key: String, value: Array[Byte]): IO[Long] =
    IO.blocking(kv.create(key, value))

  def put(key: String, value: Array[Byte]): IO[Long] =
    IO.blocking(kv.put(key, value))

  def update(key: String, value: Array[Byte], revision: Long): IO[Long] =
    IO.blocking(kv.update(key, value, revision))

  def get(key: String): IO[NatsKeyValueEntry] =
    IO.blocking(NatsKeyValueEntry(kv.get(key)))

  def get(key: String, revision: Long): IO[NatsKeyValueEntry] =
    IO.blocking(NatsKeyValueEntry(kv.get(key, revision)))

  def delete(key: String): IO[Unit] =
    IO.blocking(kv.delete(key))

  def keys: IO[List[String]] =
    IO.blocking(kv.keys().asScala.toList)

  def history(key: String): IO[List[KeyValueEntry]] =
    IO.blocking(kv.history(key).asScala.toList)

  def purge(key: String): IO[Unit] =
    IO.blocking(kv.purge(key))

  def purgeDeletes: IO[Unit] =
    IO.blocking(kv.purgeDeletes())
end NatsKeyValue

class NatsKeyValueEntry(kve: KeyValueEntry):
  def bucket: IO[String] =
    IO.blocking(kve.getBucket())

  def key: IO[String] =
    IO.blocking(kve.getKey())

  def created: IO[ZonedDateTime] =
    IO.blocking(kve.getCreated())

  def len: IO[Long] =
    IO.blocking(kve.getDataLen())

  def delta: IO[Long] =
    IO.blocking(kve.getDelta())

  def revision: IO[Long] =
    IO.blocking(kve.getRevision())

  def value: IO[Array[Byte]] =
    IO.blocking(kve.getValue())
end NatsKeyValueEntry

class NatsSubscription(sub: Subscription):
  def unsubscribe: IO[Unit] =
    IO(sub.unsubscribe)

  def next(timeout: Long): IO[Message] =
    IO.blocking(sub.nextMessage(timeout))

  def next(timeout: Duration): IO[Message] =
    IO.blocking(sub.nextMessage(timeout))
end NatsSubscription

object Nats:
  def connect(url: String): Resource[IO, NatsConnection] =
    Resource.make(IO.blocking(NatsConnection(JClient.connect(url))))(nc =>
      nc.close
    )
end Nats
