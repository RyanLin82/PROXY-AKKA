package scala.forex.programs.domain

import io.circe.{Decoder, HCursor}

import java.time.OffsetDateTime
import scala.forex.domain._

// Define the case class
final case class ForexApiResponse(
                                   pair: Rate.Pair,
                                   price: Price,
                                   timestamp: Timestamp
                                 )

object ForexApiResponse {

  // Decoder for Rate.Pair
  implicit val ratePairDecoder: Decoder[Rate.Pair] = new Decoder[Rate.Pair] {
    final def apply(c: HCursor): Decoder.Result[Rate.Pair] = {
      for {
        from <- c.downField("from").as[String].map(Currency.fromString)
        to <- c.downField("to").as[String].map(Currency.fromString)
      } yield Rate.Pair(from, to)
    }
  }

  // Implicit decoder for ForexApiResponse
  implicit val forexApiResponseDecoder: Decoder[ForexApiResponse] = new Decoder[ForexApiResponse] {
    final def apply(c: HCursor): Decoder.Result[ForexApiResponse] = {
      for {
        from <- c.downField("from").as[String].map(Currency.fromString)
        to <- c.downField("to").as[String].map(Currency.fromString)
        priceValue <- c.downField("price").as[BigDecimal]
        price = Price(priceValue)
        pair = Rate.Pair(from, to)
        timestamp <- c.downField("time_stamp").as[OffsetDateTime]
      } yield ForexApiResponse(pair, price, Timestamp(timestamp))
    }
  }
}
