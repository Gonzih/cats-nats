package me.gonzih.nats

import cats.effect.IO
import cats.effect.Resource
import cats.effect.std.Queue
import cats.effect.unsafe.implicits.global
import io.nats.client.Connection
import io.nats.client.Dispatcher
import io.nats.client.JetStream
import io.nats.client.JetStreamSubscription
import io.nats.client.Message
import io.nats.client.MessageHandler
import io.nats.client.PullSubscribeOptions
import io.nats.client.PushSubscribeOptions
import io.nats.client.api.StorageType
import io.nats.client.api.StreamConfiguration
import io.nats.client.{Nats => JClient}
import java.time.Duration

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
  def publish(subject: String, body: Array[Byte]): IO[Unit] =
    IO.blocking(js.publish(subject, body))

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

  def close: IO[Unit] =
    IO.blocking(nc.close())
end NatsConnection

object Nats:
  def connect(url: String): Resource[IO, NatsConnection] =
    Resource.make(IO.blocking(NatsConnection(JClient.connect(url))))(nc =>
      nc.close
    )
end Nats
