package scala.forex.programs.rates

import scala.forex.domain.Rate
import scala.forex.programs.rates.errors._

trait Algebra[F[_]] {
  def ratesLookup(request: Protocol.GetRatesRequest): F[Error Either Rate]
}
