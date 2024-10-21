package scala.forex.services.rates

import scala.forex.domain.Rate
import scala.forex.services.rates.errors._

trait Algebra[F[_]] {
  def get(pair: Rate.Pair): F[Error Either Rate]
}
