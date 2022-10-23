package me.gonzih.nats

import cats.effect.IO
import cats.effect.Resource
import cats.effect.std.Queue
import cats.effect.unsafe.implicits.global
import io.nats.client.Connection
import io.nats.client.Dispatcher
import io.nats.client.JetStream
import io.nats.client.Message
import io.nats.client.MessageHandler
import io.nats.client.PushSubscribeOptions
import io.nats.client.Subscription
import io.nats.client.api.StorageType
import io.nats.client.api.StreamConfiguration
import io.nats.client.{Nats => JClient}

def f2messageHandler(f: Message => Unit): MessageHandler =
  new MessageHandler:
    override def onMessage(msg: Message): Unit =
      f(msg)

class NatsPushSubscription(sub: Subscription, q: Queue[IO, Message]):
  def unsubscribe: IO[Unit] =
    IO.blocking(sub.unsubscribe())

  def take: IO[Message] =
    q.take

end NatsPushSubscription

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
      .build();

    for
      dispatcher <- IO.pure(nc.createDispatcher)
      q <- Queue.unbounded[IO, Message]
      sub <- IO(
        js.subscribe(
          subject,
          dispatcher,
          f2messageHandler(msg =>
            println("IN HANDLER")
            q.offer(msg).unsafeRunSync()
          ),
          autoack,
          so
        )
      )
    yield NatsPushSubscription(sub, q)

end NatsJetStream

class NatsConnection(nc: Connection):
  def js: IO[NatsJetStream] =
    IO(NatsJetStream(nc, nc.jetStream()))

  def addStream(
      stream: String,
      subject: String
  ): IO[Unit] =
    val jsm = nc.jetStreamManagement()
    val sc = StreamConfiguration
      .builder()
      .name(stream)
      .subjects(subject)
      // .retentionPolicy(...)
      // .maxConsumers(...)
      // .maxBytes(...)
      // .maxAge(...)
      // .maxMsgSize(...)
      .storageType(StorageType.Memory)
      // .replicas(...)
      // .noAck(...)
      // .template(...)
      // .discardPolicy(...)
      .build();
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
