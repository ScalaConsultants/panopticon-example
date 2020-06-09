package com.example.domain

import akka.actor.{ Actor, ActorSystem, PoisonPill, Props }
import zio.clock.Clock
import zio.duration.Duration
import zio.stream.ZStream
import zio.{ Has, ZIO, ZLayer }

// produces some deadletters and unhandled messages for panopticon to pickup
object DeadLettersSimulator {

  trait Service

  val live: ZLayer[Has[ActorSystem] with Clock, Throwable, DeadLettersSimulator] =
    ZLayer.fromEffect(createAndSpamActors.fork.map(_ => new Service {}))

  def createAndSpamActors: ZIO[Clock with Has[ActorSystem], Throwable, Unit] =
    ZStream.unfoldM(0)(tick).runDrain

  private def tick(i: Int): ZIO[Clock with Has[ActorSystem], Throwable, Some[(Int, Int)]] =
    for {
      system <- ZIO.access[Has[ActorSystem]](_.get)
      _      <- ZIO.when(i > 0)(stopActor(system)(i - 1))
      _      <- startActor(system)(i)
      _      <- ZIO.sleep(Duration.fromNanos(100000000L * (i % 10 + 1)))
    } yield Some((i + 1, i + 1))

  private def stopActor(system: ActorSystem)(index: Int) = ZIO.effect {
    val a = system.actorSelection(s"user/a$index")
    a ! PoisonPill
    a ! KnownMessage(s"""after death "$index-1"""")
    a ! KnownMessage(s"""after death "$index-2"""")
  }

  private def startActor(system: ActorSystem)(index: Int) = ZIO.effect {
    val a = system.actorOf(Props(new SampleActor), s"a$index")
    a ! KnownMessage(s"""known message $index"""")
    a ! UnknownMessage(s"""unknown message $index-1"""")
    a ! UnknownMessage(s"""unknown message $index-2"""")
    a ! UnknownMessage(s"""unknown message $index-3"""")
  }

  class SampleActor extends Actor {
    def receive: Receive = {
      case _: KnownMessage => ()
    }
  }

  final case class KnownMessage(text: String)
  final case class UnknownMessage(text: String)
}
