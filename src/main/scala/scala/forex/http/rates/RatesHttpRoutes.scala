package scala.forex.http.rates

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

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
            case Success(response) => complete(response)
            case Failure(exception) => complete(StatusCodes.InternalServerError -> exception.getMessage)

          }
        }
      }
    }
  }

}
