package scala.forex.programs.forex

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, Uri, headers}
import akka.http.scaladsl.unmarshalling.Unmarshal
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._

import scala.concurrent.Future
import scala.forex.domain.Rate.Pair
import scala.forex.domain._
import scala.forex.programs.clientUtils.HttpUtils
import scala.forex.programs.domain.ForexApiResponse
import scala.util.{Failure, Success}


class ForexClient(implicit system: ActorSystem) {

  def ratesLookup(): Future[List[ForexApiResponse]] = {
    val pairString = generatePairQueryString(Currency.getAllCurrency)
    val uri = Uri("http://localhost:8080/rates").withQuery(Uri.Query(pairString))

    val request = HttpRequest(uri = uri)
      .withHeaders(
        headers.RawHeader("token", "10dc303535874aeccc86a8251e6992f5")
      )
    val result = HttpUtils.withRetryAndCircuitBreaker{
    Http().singleRequest(request).flatMap {
      case res if res.status.isSuccess() =>
        println(res)
        Unmarshal(res.entity).to[List[ForexApiResponse]].flatMap { temp =>
          Future.successful(temp)
        }(system.dispatcher)
      case res if res.status.isFailure() =>
        println(res)
        res.discardEntityBytes()
        Future.failed(new Exception(s"Cannot process request due to ${res.status.intValue()} status"))
    }(system.dispatcher)}

    result.onComplete {
      case Success(value) => println(s"Operation succeeded with result: $value")
      case Failure(exception) => println(s"Operation failed with error: ${exception.getMessage}")
    }(system.dispatcher)

    result

  }

  def ratesLookupPair(pair: Pair) :Future[ForexApiResponse] = {
    val pairString = s"${pair.from}${pair.to}"
    val uri = Uri("http://localhost:8080/rates").withQuery(Uri.Query("pair" -> pairString))

    val request = HttpRequest(uri = uri)
      .withHeaders(
        headers.RawHeader("token", "10dc303535874aeccc86a8251e6992f5")
      )
    val result = HttpUtils.withRetryAndCircuitBreaker{
      Http().singleRequest(request).flatMap {
        case res if res.status.isSuccess() =>
          println(res)
          Unmarshal(res.entity).to[List[ForexApiResponse]].flatMap { temp =>
            Future.successful(temp.head) // No need for Future wrapping; just return the result
          }(system.dispatcher)
        case res if res.status.isFailure() =>
          println(res)
          res.discardEntityBytes() // Ensure the response body is consumed
          Future.failed(new Exception(s"Cannot process request due to ${res.status.intValue()} status"))
      }(system.dispatcher)}

    result.onComplete {
      case Success(value) => println(s"Operation succeeded with result: $value")
      case Failure(exception) => println(s"Operation failed with error: ${exception.getMessage}")
    }(system.dispatcher)

    result
  }

  private def generatePairQueryString(currencies: List[Currency]): String = {
    val pairs = for {
      from <- currencies
      to <- currencies if from != to
    } yield s"pair=${from.toString}${to.toString}"

    pairs.mkString("&")
  }

}


