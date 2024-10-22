package scala.forex.http.rates

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import io.circe.syntax.EncoderOps

import scala.forex.domain.{Currency, Rate}
import scala.forex.services.RatesService
import scala.util.{Failure, Success}

class RatesHttpRoutes(proxyService: RatesService) {

  val routes: Route = {
    path("rates") {
      get {
        parameters("from".as[String], "to".as[String]) { (from, to) =>
          val ratePair = Rate.Pair(Currency.fromString(from), Currency.fromString(to))
          onComplete(proxyService.fetchRatesForMultiplePairs(ratePair)) {
            case Success(response) =>
              response match {
                case None => complete(StatusCodes.InternalServerError -> "Error: Rate not found")
                case Some(res) =>
                  complete(HttpEntity(ContentTypes.`application/json`, Protocol.fromForexApiResponse(res).asJson.noSpaces))
              }
            case Failure(exception) =>
              complete(StatusCodes.InternalServerError -> s"Error: ${exception.getMessage}")
          }
        }
      }
    }
  }
}
