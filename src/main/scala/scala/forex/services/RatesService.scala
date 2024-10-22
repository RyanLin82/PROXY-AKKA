package scala.forex.services

import akka.actor.ActorSystem
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.forex.domain.{Price, Rate, Timestamp}
import scala.forex.programs.rates.domain.ForexApiResponse
import scala.forex.programs.rates.forex.ForexClient
import scala.forex.services.cache.{CacheService, RateCacheValue}


/**
 * Service for fetching Forex rates, utilizing an external client and caching mechanism.
 *
 * @param externalClient The client responsible for making requests to the Forex API.
 * @param system The ActorSystem for managing asynchronous operations.
 */
class RatesService(externalClient: ForexClient)(implicit system: ActorSystem) {

  private val logger = LoggerFactory.getLogger(classOf[RatesService])
  private val cacheService = new CacheService()
  private implicit val ec: ExecutionContext = system.dispatcher

  /**
   * Fetches Forex rates for a specific currency pair.
   * It checks the cache first, retrieves from the external service if needed, and updates the cache.
   *
   * @param pair The currency pair to fetch rates for.
   * @return A Future containing an optional ForexApiResponse.
   */
  def fetchRatesForMultiplePairs(pair: Rate.Pair): Future[Option[ForexApiResponse]] = {

    if (pair.to == pair.from) {
      val response = generateResponse(pair, RateCacheValue(Price(BigDecimal(1.0)), Timestamp.now))
      logger.info("Identical currency pair: {}. Returning default rate of 1.0.", pair)
      return Future.successful(Some(response))
    }

    cacheService.getResourceServerStatus("Forex") match {
      case Some(_) =>
        logger.warn("Resource server status indicates unavailability. Skipping external lookup.")
        Future.successful(None)

      case None =>
        cacheService.getRateFromCache(pair) match {
          case Some(rate) =>
            Future.successful(Some(generateResponse(pair, rate)))

          case None =>

            externalClient.ratesLookup().flatMap {
              case Nil =>
                Future.successful(None) // No results from lookup
              case resList =>
                cacheService.storeInRatesCache(resList)
                Future.successful(None)
            }.recoverWith {
              case ex: Exception =>
                logger.error("Failed to fetch multiple rates for caching: {}", ex.getMessage)
                Future.successful(None)
            }

            externalClient.ratesLookupPair(pair).map { res =>
              logger.info("Fetched rate from external service for pair: {} -> {}", pair.from, pair.to)
              Some(res)
            }.recover {
              case ex: Exception =>
                logger.error("Failed to fetch rate for pair {} -> {}: {}", pair.from, pair.to, ex.getMessage)
                None
            }
        }
    }
  }

  /**
   * Generates a ForexApiResponse from a given RateCacheValue.
   *
   * @param pair The currency pair.
   * @param rateInfo The cached rate information.
   * @return A ForexApiResponse containing the rate information.
   */
  private def generateResponse(pair: Rate.Pair, rateInfo: RateCacheValue): ForexApiResponse = {
    ForexApiResponse(pair = pair, price = rateInfo.price, timestamp = rateInfo.timestamp)
  }
}