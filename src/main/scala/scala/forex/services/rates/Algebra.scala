package scala.forex.services.rates

import scala.concurrent.Future
import scala.forex.domain.Rate
import scala.forex.programs.rates.domain.ForexApiResponse

trait Algebra[F[_]] {
  def fetchRatesForMultiplePairs(pair: Rate.Pair): Future[Option[ForexApiResponse]]
}
