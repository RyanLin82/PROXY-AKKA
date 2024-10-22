package scala.forex.programs.rates

import scala.concurrent.Future
import scala.forex.domain.Rate.Pair
import scala.forex.programs.rates.domain.ForexApiResponse

trait Algebra {
  /**
   * Fetches rates for all possible currency pairs.
   *
   * @return Future containing a list of Forex API responses.
   */
  def ratesLookup(): Future[List[ForexApiResponse]]

  /**
   * Fetches rates for a specific currency pair.
   *
   * @param pair The currency pair to look up.
   * @return Future containing a Forex API response.
   */
  def ratesLookupPair(pair: Pair): Future[ForexApiResponse]
}
