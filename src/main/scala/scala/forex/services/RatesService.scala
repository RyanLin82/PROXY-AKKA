package scala.forex.services

import akka.actor.ActorSystem

import scala.concurrent.Future
import scala.forex.domain.Rate
import scala.forex.programs.domain.ForexApiResponse
import scala.forex.programs.forex.ForexClient
import scala.forex.services.cache.{CacheService, PriceInfo}

class RatesService(externalClient: ForexClient) (implicit system: ActorSystem){

  val cacheService = new CacheService()
  def fetchRatesForMultiplePairs(pair: Rate.Pair): Future[Option[ForexApiResponse]] = {
    cacheService.getResourceServerStatus("Forex") match {
      case Some(_) =>
        // Status found in the cache, do nothing
        println("forex unhealth")
        Future.successful(None)

      case None =>
        cacheService.getRateFromCache(pair) match {
          case Some(rate) =>
            println("get from cache")
            Future.successful(Option(generateResponse(pair, rate)))
          case None =>
            // Status not found, perform client lookup
            externalClient.ratesLookup().flatMap {
              case Nil =>
                Future.successful(None) // No results from lookup
              case resList =>
                // Store the results in the cache
                println("store the cache")
                cacheService.storeInRatesCache(resList)
                Future.successful(None)

            }(system.dispatcher)
            externalClient.ratesLookupPair(pair).flatMap(
              res =>
                {println("resSucc" + res)

                  Future.successful(Option(res))}
            )(system.dispatcher)
        }
    }
  }

  private def generateResponse(pair: Rate.Pair, rateInfo: PriceInfo): ForexApiResponse = {
    ForexApiResponse(pair = pair, price = rateInfo.price, timestamp = rateInfo.timestamp)
  }
}