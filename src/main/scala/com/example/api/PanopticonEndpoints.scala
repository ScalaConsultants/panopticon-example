package com.example.api

import akka.actor.ActorSystem
import akka.http.interop._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import io.scalac.periscope.akka.http.{ ActorSystemStatusRoute, ActorTreeRoute, DeadLettersMonitorRoute }
import zio._

import scala.concurrent.ExecutionContext

object PanopticonEndpoints {

  trait Service {
    def routes: Route
  }

  val live: ZLayer[Has[ActorSystem], Nothing, PanopticonEndpoints] = ZLayer.fromFunction(env =>
    new Service with ZIOSupport {

      implicit val system: ActorSystem  = env.get
      implicit val ec: ExecutionContext = system.dispatcher

      val deadLetters: Route = DeadLettersMonitorRoute()
      def routes: Route =
        pathPrefix("panopticon") {
          pathPrefix("actor-tree") {
            ActorTreeRoute(env.get)
          } ~
          pathPrefix("actor-system-status") {
            ActorSystemStatusRoute(env.get)
          } ~
          pathPrefix("dead-letters")(deadLetters)
        }
    }
  )

  // accessors
  val routes: URIO[Api, Route] = ZIO.access[Api](a => Route.seal(a.get.routes))
}
