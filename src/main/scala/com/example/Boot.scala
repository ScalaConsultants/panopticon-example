package com.example

import akka.actor.ActorSystem
import com.example.api._
import com.example.api.graphql.GraphQLApi
import com.example.config.AppConfig
import com.example.domain.ItemRepository
import com.example.infrastructure._
import slick.interop.zio.DatabaseProvider
import akka.http.scaladsl.server.Route
import com.typesafe.config.{ Config, ConfigFactory }
import zio.clock.Clock
import zio.config.typesafe.TypesafeConfig
import zio.console._
import zio.logging._
import zio.logging.slf4j._
import zio._
import akka.http.interop._
import akka.http.scaladsl.server.RouteConcatenation._

object Boot extends App {

  def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] =
    ZIO(ConfigFactory.load.resolve)
      .flatMap(rawConfig => program.provideCustomLayer(prepareEnvironment(rawConfig)))
      .as(0)
      .catchAll(error => putStrLn(error.getMessage).as(1))

  private val program: ZIO[HttpServer with Console, Throwable, Unit] =
    HttpServer.start.use(_ => putStrLn(s"Server online. Press RETURN to stop...") <* getStrLn)

  private def prepareEnvironment(rawConfig: Config): TaskLayer[HttpServer] = {
    val configLayer = TypesafeConfig.fromTypesafeConfig(rawConfig, AppConfig.descriptor)

    // using raw config since it's recommended and the simplest to work with slick
    val dbConfigLayer = ZLayer.fromEffect(ZIO(rawConfig.getConfig("db")))
    val dbBackendLayer = ZLayer.succeed(slick.jdbc.H2Profile.backend)

    // narrowing down to the required part of the config to ensure separation of concerns
    val apiConfigLayer = configLayer.map(c => Has(c.get.api))

    val actorSystemLayer: TaskLayer[Has[ActorSystem]] = ZLayer.fromManaged {
      ZManaged.make(ZIO(ActorSystem("panopticon-example-system")))(s => ZIO.fromFuture(_ => s.terminate()).either)
    }

    val loggingLayer: ULayer[Logging] = Slf4jLogger.make { (context, message) =>
      val logFormat = "[correlation-id = %s] %s"
      val correlationId = LogAnnotation.CorrelationId.render(
        context.get(LogAnnotation.CorrelationId)
      )
      logFormat.format(correlationId, message)
    }

    val dbLayer: TaskLayer[ItemRepository] =
      (((dbConfigLayer ++ dbBackendLayer) >>> DatabaseProvider.live) ++ loggingLayer) >>> SlickItemRepository.live

    val apiLayer: TaskLayer[Api] = (apiConfigLayer ++ dbLayer) >>> Api.live

    val graphQLApiLayer: TaskLayer[GraphQLApi] =
      (dbLayer ++ actorSystemLayer ++ loggingLayer ++ Clock.live) >>> GraphQLApi.live

    val routesLayer: ZLayer[Api with GraphQLApi, Nothing, Has[Route]] =
      ZLayer.fromServices[Api.Service, api.graphql.GraphQLApi.Service, Route]{ (api, gApi) => api.routes ~ gApi.routes }
    (actorSystemLayer ++ apiConfigLayer ++ (apiLayer  ++ graphQLApiLayer >>> routesLayer)) >>> HttpServer.live
  }
}
