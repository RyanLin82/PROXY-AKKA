package scala.forex.http.rates

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import io.circe.syntax.EncoderOps
import org.slf4j.LoggerFactory

import scala.forex.domain.{Currency, Rate}
import scala.forex.http.domain.Protocol
import scala.forex.services.rates.RatesService
import scala.util.{Failure, Success}

/**
 * Defines HTTP routes for fetching currency exchange rates.
 *
 * @param proxyService The service responsible for handling rate fetching.
 */
class RatesHttpRoutes(proxyService: RatesService) {

  // Initialize logger
  private val logger = LoggerFactory.getLogger(classOf[RatesHttpRoutes])

  /**
   * The HTTP routes for the rates endpoint.
   *
   * @return The Akka HTTP Route for handling requests to /rates.
   */
  val routes: Route = {
    path("rates") {
      get {
        parameters("from".as[String], "to".as[String]) { (from, to) =>
          val ratePair = Rate.Pair(Currency.fromString(from), Currency.fromString(to))
          logger.info("Received request to fetch rates for pair: {} -> {}", from, to)

          onComplete(proxyService.fetchRatesForMultiplePairs(ratePair)) {
            case Success(response) =>
              response match {
                case None =>
                  logger.error("Rate not found for pair: {} -> {}", from, to)
                  complete(StatusCodes.InternalServerError -> "Error: Rate not found")
                case Some(res) =>
                  complete(HttpEntity(ContentTypes.`application/json`, Protocol.fromForexApiResponse(res).asJson.noSpaces))
              }
            case Failure(exception) =>
              complete(exception)
          }
        }
      }
    }
  }
}
