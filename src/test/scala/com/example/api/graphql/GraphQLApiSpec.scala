package com.example.api.graphql

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import com.example.api.GraphQLApi
import com.example.domain.{ InMemoryItemRepository, Item, ItemId }
import com.example.interop.akka.ZioRouteTest
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import play.api.libs.json._
import zio._
import zio.blocking._
import zio.clock.Clock
import zio.logging.Logging
import zio.test.Assertion._
import zio.test._

trait GraphQLApiSpecJsonProtocol extends PlayJsonSupport {
  implicit val itemIdFormat = Json.format[ItemId]
  implicit val itemFormat   = Json.format[Item]
}

object GraphQLApiSpec extends ZioRouteTest with GraphQLApiSpecJsonProtocol {

  def spec =
    suite("GraphQLApi must")(
      testM("Add item on call to 'addItem'") {
        val query =
          """
            |{
            |	"query": "mutation($name: String!, $price: BigDecimal!) { addItem(name: $name, price: $price) { value } }",
            | "variables": {
            |   "name": "Test item",
            |   "price": 10
            | }
            |}
            |""".stripMargin

        sendQueryAndCheckResult(query) {
          (statusCode, contentType, body) =>
            val itemId =
              for {
                jsonData        <- body.value.get("data")
                jsObjectData    <- jsonData.asOpt[JsObject]
                jsonAddItem     <- jsObjectData.value.get("addItem")
                jsObjectAddItem <- jsonAddItem.asOpt[JsObject]
                jsonId          <- jsObjectAddItem.value.get("value")
                id              <- jsonId.asOpt[BigDecimal]
              } yield id
            assert(statusCode)(equalTo(StatusCodes.OK)) &&
            assert(contentType)(equalTo(ContentTypes.`application/json`)) &&
            assert(itemId)(isSome(equalTo(BigDecimal(0))))
        }
      },
      testM("Return all items on call to 'allItems'") {
        val query =
          """
            |{
            |	"query": "{ allItems { id { value } name price } }" 
            |}
            |""".stripMargin
        sendQueryAndCheckResult(query) { (statusCode, contentType, body) =>
          val allItems = getItemsFromBody(body, "allItems")
          assert(statusCode)(equalTo(StatusCodes.OK)) &&
          assert(contentType)(equalTo(ContentTypes.`application/json`)) &&
          assert(allItems)(isSome(equalTo(List(Item(ItemId(0), "Test item", 10)))))
        }
      },
      testM("Return an item, given its ItemId, on call to 'item'") {
        val query =
          """
            |{
            |	"query": "query($itemId: Long!) { item(value: $itemId) { id { value } name price } }",
            | "variables": { "itemId": 0 }
            |}
            |""".stripMargin

        sendQueryAndCheckResult(query) { (statusCode, contentType, body) =>
          val item =
            for {
              jsonData     <- body.value.get("data")
              jsObjectData <- jsonData.asOpt[JsObject]
              jsonItem     <- jsObjectData.value.get("item")
              item         <- jsonItem.asOpt[Item]
            } yield item
          assert(statusCode)(equalTo(StatusCodes.OK)) &&
          assert(contentType)(equalTo(ContentTypes.`application/json`)) &&
          assert(item)(isSome(equalTo(Item(ItemId(0), "Test item", 10))))
        }
      },
      testM("Return null on call to 'item', for an itemId that does not exist") {
        val query =
          """
            |{
            |	"query": "query($itemId: Long!) { item(value: $itemId) { id { value } name price } }",
            | "variables": { "itemId": 1 }
            |}
            |""".stripMargin

        sendQueryAndCheckResult(query) { (statusCode, contentType, body) =>
          val jsonItem =
            for {
              jsonData     <- body.value.get("data")
              jsObjectData <- jsonData.asOpt[JsObject]
              jsonItem     <- jsObjectData.value.get("item")
            } yield jsonItem
          assert(statusCode)(equalTo(StatusCodes.OK)) &&
          assert(contentType)(equalTo(ContentTypes.`application/json`)) &&
          assert(jsonItem)(isSome(equalTo(JsNull)))
        }
      },
      testM("Return all items with the given name, on call to 'itemByName'") {
        val query =
          """
            |{
            |	"query": "query ($name: String!) { itemByName(name: $name) { id { value } name price } }",
            | "variables": { "name": "Test item" }
            |}
            |""".stripMargin

        sendQueryAndCheckResult(query) { (statusCode, contentType, body) =>
          val items = getItemsFromBody(body, "itemByName")
          assert(statusCode)(equalTo(StatusCodes.OK)) &&
          assert(contentType)(equalTo(ContentTypes.`application/json`)) &&
          assert(items)(isSome(equalTo(List(Item(ItemId(0), "Test item", 10)))))
        }
      },
      testM("Return an empty list on call to 'itemByName', if there are no items with the given name") {
        val query =
          """
            |{
            |	"query": "query ($name: String!) { itemByName(name: $name) { id { value } name price } }",
            | "variables": { "name": "Another item" }
            |}
            |""".stripMargin

        sendQueryAndCheckResult(query) { (statusCode, contentType, body) =>
          val items = getItemsFromBody(body, "itemByName")
          assert(statusCode)(equalTo(StatusCodes.OK)) &&
          assert(contentType)(equalTo(ContentTypes.`application/json`)) &&
          assert(items)(isSome(isEmpty))
        }
      },
      testM("Return all items cheaper than the given price, on call to 'cheaperThan'") {
        val query =
          """
            |{
            |	"query": "query ($price: BigDecimal!) { cheaperThan(price: $price) { id { value } name price } }",
            | "variables": { "price": 100 }
            |}
            |""".stripMargin

        sendQueryAndCheckResult(query) { (statusCode, contentType, body) =>
          val items = getItemsFromBody(body, "cheaperThan")
          assert(statusCode)(equalTo(StatusCodes.OK)) &&
          assert(contentType)(equalTo(ContentTypes.`application/json`)) &&
          assert(items)(isSome(equalTo(List(Item(ItemId(0), "Test item", 10)))))
        }
      },
      testM("Return an empty list on call to 'cheaperThan', if there are no items cheaper than the given price") {
        val query =
          """
            |{
            |	"query": "query ($price: BigDecimal!) { cheaperThan(price: $price) { id { value } name price } }",
            | "variables": { "price": 5 }
            |}
            |""".stripMargin

        sendQueryAndCheckResult(query) { (statusCode, contentType, body) =>
          val items = getItemsFromBody(body, "cheaperThan")
          assert(statusCode)(equalTo(StatusCodes.OK)) &&
          assert(contentType)(equalTo(ContentTypes.`application/json`)) &&
          assert(items)(isSome(isEmpty))
        }
      },
      testM("Delete item on call to 'deleteItem'") {
        val query =
          """
            |{
            |	"query": "mutation($itemId: Long!) { deleteItem(value: $itemId) }",
            | "variables": { "itemId": 0 }
            |}
            |""".stripMargin

        sendQueryAndCheckResult(query) { (statusCode, contentType, body) =>
          val jsObjectDeleteItemKeys = getKeysFromBody(body, "deleteItem")
          assert(statusCode)(equalTo(StatusCodes.OK)) &&
          assert(contentType)(equalTo(ContentTypes.`application/json`)) &&
          assert(jsObjectDeleteItemKeys)(isSome(isEmpty))
        }
      },
      testM("Succeed on call to 'deleteItem', when the given itemId does not exist") {
        val query =
          """
            |{
            |	"query": "mutation($itemId: Long!) { deleteItem(value: $itemId) }",
            | "variables": { "itemId": 1 }
            |}
            |""".stripMargin

        sendQueryAndCheckResult(query) { (statusCode, contentType, body) =>
          val jsObjectDeleteItemKeys = getKeysFromBody(body, "deleteItem")
          assert(statusCode)(equalTo(StatusCodes.OK)) &&
          assert(contentType)(equalTo(ContentTypes.`application/json`)) &&
          assert(jsObjectDeleteItemKeys)(isSome(isEmpty))
        }
      }
    ).provideCustomLayerShared(env)

  private val systemLayer: ULayer[Has[ActorSystem]] = ZLayer.fromManaged {
    ZManaged.make(ZIO.effect(system).orDie)(s => ZIO.fromFuture(_ => s.terminate()).either)
  }

  private val env: ULayer[GraphQLApi] =
    (InMemoryItemRepository.test ++ systemLayer ++ Clock.live ++ Logging.ignore) >>> GraphQLApi.live.orDie

  private def sendQueryAndCheckResult(query: String)(
    assertion: (StatusCode, ContentType, JsObject) => TestResult
  ): ZIO[Blocking with GraphQLApi, Throwable, TestResult] =
    for {
      routes  <- GraphQLApi.routes
      request = Post("/api/graphql").withEntity(HttpEntity(ContentTypes.`application/json`, query))
      resultCheck <- effectBlocking {
                      request ~> routes ~> check {
                        val statusCode = status
                        val ct         = contentType
                        val body       = entityAs[JsObject]
                        assertion(statusCode, ct, body)
                      }
                    }
    } yield resultCheck

  private def getItemsFromBody(body: JsObject, key: String): Option[List[Item]] =
    for {
      jsonData     <- body.value.get("data")
      jsObjectData <- jsonData.asOpt[JsObject]
      jsonAllItems <- jsObjectData.value.get(key)
      items        <- jsonAllItems.asOpt[List[Item]]
    } yield items

  private def getKeysFromBody(body: JsObject, key: String): Option[collection.Set[String]] =
    for {
      jsonData           <- body.value.get("data")
      jsObjectData       <- jsonData.asOpt[JsObject]
      jsonDeleteItem     <- jsObjectData.value.get(key)
      jsObjectDeleteItem <- jsonDeleteItem.asOpt[JsObject]
      keys               = jsObjectDeleteItem.keys
    } yield keys
}
