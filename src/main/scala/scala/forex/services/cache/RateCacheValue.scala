package scala.forex.services.cache

import scala.forex.domain.{Price, Timestamp}

case class RateCacheValue(
                           price: Price,
                           timestamp: Timestamp
                         )
