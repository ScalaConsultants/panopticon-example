package com.example.api

import akka.actor.ActorSystem
import akka.http.interop._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import io.scalac.panopticon.akka.http.{ ActorCountRoute, ActorTreeRoute }
import zio._

object PanopticonEndpoints {

  trait Service {
    def routes: Route
  }

  val live: ZLayer[Has[ActorSystem], Nothing, PanopticonEndpoints] = ZLayer.fromFunction(env =>
    new Service with ZIOSupport {

      implicit val ec = env.get.dispatcher

      def routes: Route =
        pathPrefix("panopticon") {
          pathPrefix("actor-tree") {
            ActorTreeRoute(env.get)
          } ~
          pathPrefix("actor-count") {
            ActorCountRoute(env.get)
          }
        }
    }
  )

  // accessors
  val routes: URIO[Api, Route] = ZIO.access[Api](a => Route.seal(a.get.routes))
}
