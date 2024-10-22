package scala.forex.programs.rates.forex

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, Uri, headers}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.pattern.CircuitBreakerOpenException
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.forex.domain.Rate.Pair
import scala.forex.domain._
import scala.forex.programs.rates.Algebra
import scala.forex.programs.rates.clientUtils.HttpUtils
import scala.forex.programs.rates.domain.ForexApiResponse
import scala.util.{Failure, Success}


class ForexClient(implicit system: ActorSystem) extends Algebra{

  private val logger = LoggerFactory.getLogger(classOf[ForexClient])
  private implicit val dispatcher: ExecutionContextExecutor = system.dispatcher

  private val config = new ForexClientConfig()

  /**
   * Fetches rates for all currency pairs.
   *
   * @return Future containing a list of Forex API responses.
   */
  override def ratesLookup(): Future[List[ForexApiResponse]] = {
    val pairString = generatePairQueryString(Currency.getAllCurrency)
    val uri = Uri(config.baseUrl).withQuery(Uri.Query(pairString))

    val request = HttpRequest(uri = uri)
      .withHeaders(
        headers.RawHeader("token", config.token)
      )

    val result = HttpUtils.withRetryAndCircuitBreaker {
      Http().singleRequest(request).flatMap {
        case res if res.status.isSuccess() =>
          logger.info("Received successful response: {}", res)
          Unmarshal(res.entity).to[List[ForexApiResponse]].flatMap { temp =>
            Future.successful(temp)
          }
        case res if res.status.isFailure() =>
          logger.warn("Received failure response with status: {}", res.status.intValue())
          res.discardEntityBytes()
          Future.failed(new Exception(s"Cannot process request due to ${res.status.intValue()} status"))
      }
    }

    result.recoverWith {
      case ex: CircuitBreakerOpenException =>
        logger.error("Circuit breaker is open: Service temporarily unavailable.", ex)
        Future.failed(new ProxyException(429, "The resource server might not be available, please try it later."))
      case ex: Exception =>
        logger.error("Operation failed with error: {}", ex.getMessage)
        Future.failed(ex)
    }
  }

  /**
   * Fetches rates for a specific currency pair.
   *
   * @param pair The currency pair to look up.
   * @return Future containing a Forex API response.
   */
  override def ratesLookupPair(pair: Pair): Future[ForexApiResponse] = {
    val pairString = s"${pair.from}${pair.to}"
    val uri = Uri(config.baseUrl).withQuery(Uri.Query("pair" -> pairString))

    val request = HttpRequest(uri = uri)
      .withHeaders(
        headers.RawHeader("token", config.token)
      )

    val result = HttpUtils.withRetryAndCircuitBreaker {
      Http().singleRequest(request).flatMap {
        case res if res.status.isSuccess() =>
          logger.info("Received successful response for pair: {} -> {}", pair.from, pair.to)
          Unmarshal(res.entity).to[List[ForexApiResponse]].flatMap { temp =>
            Future.successful(temp.head)
          }
        case res if res.status.isFailure() =>
          logger.warn("Received failure response with status: {} for pair: {} -> {}", res.status.intValue(), pair.from, pair.to)
          res.discardEntityBytes()
          Future.failed(new Exception(s"Cannot process request due to ${res.status.intValue()} status"))
      }
    }

    result.onComplete {
      case Success(value) => logger.info("Operation succeeded with result for pair {}: {}", pair, value)
      case Failure(exception) => logger.error("Operation failed with error for pair {}: {}", pair, exception.getMessage)
    }

    result
  }

  /**
   * Generates a query string for all possible currency pairs.
   *
   * @param currencies The list of currencies to create pairs from.
   * @return A query string representing all currency pairs.
   */
  private def generatePairQueryString(currencies: List[Currency]): String = {
    val pairs = for {
      from <- currencies
      to <- currencies if from != to
    } yield s"pair=${from.toString}${to.toString}"
    pairs.mkString("&")
  }
}