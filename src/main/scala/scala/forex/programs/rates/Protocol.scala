package scala.forex.programs.rates

import scala.forex.domain.Currency

object Protocol {

  final case class GetRatesRequest(
      from: Currency,
      to: Currency
  )
}
