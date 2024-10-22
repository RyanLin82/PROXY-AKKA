package scala.forex.http.domain

import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder

final case class ProxyErrorResponse (
                           status: Int,
                           errorCode: String,
                           message: String
                         )

object ProxyErrorResponse {
  implicit val proxyErrorResponseEncoder: Encoder[ProxyErrorResponse] = deriveEncoder[ProxyErrorResponse]
}
