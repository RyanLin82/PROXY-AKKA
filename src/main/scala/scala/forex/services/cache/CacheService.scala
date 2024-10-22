package scala.forex.services.cache

import com.github.benmanes.caffeine.cache.{Cache, Caffeine}
import org.slf4j.LoggerFactory

import java.util.concurrent.TimeUnit
import scala.forex.domain.Price
import scala.forex.domain.Rate.Pair
import scala.forex.programs.rates.domain.ForexApiResponse


/**
 * Service for caching Forex rates and resource server statuses.
 *
 * @param cacheConfig Configuration for cache settings.
 */
class CacheService(cacheConfig: CacheConfig = CacheConfig()) {

  // Initialize logger
  private val logger = LoggerFactory.getLogger(classOf[CacheService])

  // Cache for storing Forex rates, keyed by currency pair
  private val ratesCache: Cache[Pair, RateCacheValue] = Caffeine.newBuilder()
    .expireAfterWrite(cacheConfig.RatesCache.expireAfterWriteMinutes, TimeUnit.MINUTES)
    .maximumSize(cacheConfig.RatesCache.maximumSize)
    .build[Pair, RateCacheValue]()

  // Cache for storing resource server statuses, keyed by server name
  private val resourceServerCache: Cache[String, Boolean] = Caffeine.newBuilder()
    .expireAfterWrite(cacheConfig.ResourceServerCache.expireAfterWriteDays, TimeUnit.DAYS)
    .maximumSize(cacheConfig.ResourceServerCache.maximumSize)
    .build[String, Boolean]()

  /**
   * Stores a list of Forex rates in the cache.
   *
   * @param rates The list of Forex rates to be cached.
   */
  def storeInRatesCache(rates: List[ForexApiResponse]): Unit = {
    rates.foreach { rate =>
      val priceInfo = RateCacheValue(Price(rate.price.value), rate.timestamp)
      ratesCache.put(rate.pair, priceInfo)
    }
    logger.info("Stored {} pairs of rates in cache.", rates.size)
  }

  /**
   * Retrieves a Forex rate from the cache based on the currency pair.
   *
   * @param pair The currency pair to look up.
   * @return An optional RateCacheValue if the pair is found in the cache.
   */
  def getRateFromCache(pair: Pair): Option[RateCacheValue] = {
    val cachedRate = Option(ratesCache.getIfPresent(pair))
    cachedRate match {
      case Some(_) => logger.info("Cache hit for pair: {} -> {}", pair.from, pair.to)
      case None => logger.warn("Cache miss for pair: {} -> {}", pair.from, pair.to)
    }
    cachedRate
  }

  /**
   * Stores the status of a resource server in the cache.
   *
   * @param serverName The name of the resource server.
   */
  def storeResourceServerStatus(serverName: String): Unit = {
    resourceServerCache.put(serverName, true)
    logger.info("Stored resource server status for: {}", serverName)
  }

  /**
   * Retrieves the status of a resource server from the cache.
   *
   * @param serverName The name of the resource server.
   * @return An optional Boolean indicating the status of the server.
   */
  def getResourceServerStatus(serverName: String): Option[Boolean] = {
    Option(resourceServerCache.getIfPresent(serverName))
  }
}
