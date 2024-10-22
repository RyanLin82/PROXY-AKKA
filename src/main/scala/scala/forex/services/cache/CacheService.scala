package scala.forex.services.cache

import com.github.benmanes.caffeine.cache.{Cache, Caffeine}

import java.util.concurrent.TimeUnit
import scala.forex.domain.Rate.Pair
import scala.forex.domain.{Price, Timestamp}
import scala.forex.programs.domain.ForexApiResponse

case class PriceInfo(
                      price: Price,
                      timestamp: Timestamp
                    )

class CacheService {


  // Define the cache with a 5-minute expiration time
  val ratesCache: Cache[Pair, PriceInfo] = Caffeine.newBuilder()
    .expireAfterWrite(5, TimeUnit.MINUTES)  // Expire after 5 minutes
    .maximumSize(1000)                      // Set maximum size
    .build[Pair, PriceInfo]()

  val resourceServerCache: Cache[String, Boolean] = Caffeine.newBuilder()
    .expireAfterWrite(1, TimeUnit.DAYS)  // Expire after 5 minutes
    .maximumSize(500)                      // Set maximum size
    .build[String, Boolean]()

  // Methods to interact with the cache
  def storeInRatesCache(rates: List[ForexApiResponse]): Unit = {
    rates.foreach { rate =>
      val priceInfo = PriceInfo(Price(rate.price.value), rate.timestamp)
      ratesCache.put(rate.pair, priceInfo)
    }
  }

  def getRateFromCache(pair: Pair): Option[PriceInfo] = {
    Option(ratesCache.getIfPresent(pair))
  }

  def storeResourceServerStatus(serverName: String): Unit = {
    resourceServerCache.put(serverName, true)
  }

  def getResourceServerStatus(serverName: String): Option[Boolean] = {
    Option(resourceServerCache.getIfPresent(serverName))
  }
}
