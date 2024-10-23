package forex.services.cache

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.OffsetDateTime
import scala.forex.domain.{Currency, Price, Rate, Timestamp}
import scala.forex.programs.rates.domain.ForexApiResponse
import scala.forex.services.cache.CacheService

class CacheServiceSpec extends AnyFlatSpec with Matchers {
  val cacheService = new CacheService()

  "CacheService" should "store and retrieve Forex rates from the rates cache" in {
    val pair = Rate.Pair(Currency.USD, Currency.EUR)
    val rate = ForexApiResponse(pair, Price(BigDecimal(1.12)), Timestamp(OffsetDateTime.now))

    cacheService.storeInRatesCache(List(rate))

    val cachedRate = cacheService.getRateFromCache(pair)

    cachedRate should not be empty
    cachedRate.get.price.value shouldEqual BigDecimal(1.12)
  }

  it should "return None for a cache miss on the rates cache" in {
    val pair = Rate.Pair(Currency.GBP, Currency.JPY)
    val cachedRate = cacheService.getRateFromCache(pair)

    cachedRate shouldBe None
  }

  it should "store and retrieve resource server statuses" in {
    cacheService.storeResourceServerStatus("server-1")

    val serverStatus = cacheService.getResourceServerStatus("server-1")

    serverStatus shouldBe Some(true)
  }

  it should "evict entries from the resource server cache" in {

    cacheService.storeResourceServerStatus("server-1")
    cacheService.evictResourceServerCache("server-1")
    val serverStatus = cacheService.getResourceServerStatus("server-1")
    serverStatus shouldBe None
  }
}


