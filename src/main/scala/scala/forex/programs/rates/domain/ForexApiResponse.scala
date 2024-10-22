package scala.forex.programs.rates.domain

import io.circe.{Decoder, HCursor}

import java.time.OffsetDateTime
import scala.forex.domain._

/**
 * Represents a response from the Forex API containing exchange rate information.
 *
 * @param pair The currency pair.
 * @param price The price of the exchange rate.
 * @param timestamp The timestamp of the rate information.
 */
final case class ForexApiResponse(
                                   pair: Rate.Pair,
                                   price: Price,
                                   timestamp: Timestamp
                                 )

object ForexApiResponse {

  /**
   * Custom decoder for decoding a currency pair from the JSON response.
   */
  implicit val ratePairDecoder: Decoder[Rate.Pair] = new Decoder[Rate.Pair] {
    final def apply(c: HCursor): Decoder.Result[Rate.Pair] = {
      for {
        from <- c.downField("from").as[String].map(Currency.fromString)
        to <- c.downField("to").as[String].map(Currency.fromString)
      } yield Rate.Pair(from, to)
    }
  }

  /**
   * Decoder for ForexApiResponse, transforming the JSON structure into the case class.
   */
  implicit val forexApiResponseDecoder: Decoder[ForexApiResponse] = new Decoder[ForexApiResponse] {
    final def apply(c: HCursor): Decoder.Result[ForexApiResponse] = {
      for {
        from <- c.downField("from").as[String].map(Currency.fromString)
        to <- c.downField("to").as[String].map(Currency.fromString)
        priceValue <- c.downField("price").as[BigDecimal]
        timestamp <- c.downField("time_stamp").as[OffsetDateTime]
      } yield {
        val price = Price(priceValue)
        val pair = Rate.Pair(from, to)
        ForexApiResponse(pair, price, Timestamp(timestamp))
      }
    }
  }
}
