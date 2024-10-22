package scala.forex.http.rates

import io.circe._
import io.circe.generic.semiauto.deriveEncoder

import java.time.OffsetDateTime
import scala.forex.programs.rates.domain.ForexApiResponse

object Protocol {

  final case class ProxyServiceResponse(
      from: String,
      to: String,
      price: BigDecimal,
      timestamp: OffsetDateTime
  )

  def fromForexApiResponse(forexApiResponse: ForexApiResponse): ProxyServiceResponse = {
    ProxyServiceResponse(
      from = forexApiResponse.pair.from.toString,
      to = forexApiResponse.pair.to.toString,
      price = forexApiResponse.price.value,
      timestamp = forexApiResponse.timestamp.value
    )
  }

  implicit val proxyServiceResponseEncoder: Encoder[ProxyServiceResponse] = deriveEncoder[ProxyServiceResponse]

}
