package scala.forex.programs.rates.forex

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri, headers}
import akka.http.scaladsl.unmarshalling.Unmarshal
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.forex.domain.Rate.Pair
import scala.forex.domain._
import scala.forex.programs.rates.RateClient
import scala.forex.programs.rates.domain.ForexApiResponse
import scala.forex.programs.rates.utils.ClientUtils


/**
 * Implementation of the RatesLookupService trait that interacts with an external Forex API.
 *
 * @param config The configuration for the Forex client.
 * @param system The ActorSystem to be used for making HTTP requests.
 */
class ForexClient(config: ForexClientConfig = new ForexClientConfig())(implicit system: ActorSystem) extends RateClient[Future] {

  private val logger = LoggerFactory.getLogger(classOf[ForexClient])
  private implicit val ec: ExecutionContext = system.dispatcher

  /**
   * Fetches rates for all currency pairs.
   *
   * @return Future containing a list of Forex API responses.
   */
  override def ratesLookup(): Future[List[ForexApiResponse]] = {
    val uri = buildUriForAllPairs()
    executeRequest(uri).recoverWith(handleError)
  }

  /**
   * Fetches rates for a specific currency pair.
   *
   * @param pair The currency pair to look up.
   * @return Future containing a Forex API response.
   */
  override def ratesLookupPair(pair: Pair): Future[ForexApiResponse] = {
    val uri = buildUriForPair(pair)
    executeRequest(uri).map(_.head).recoverWith(handleError)
  }

  /**
   * Builds the URI for fetching rates for all currency pairs.
   *
   * @return The constructed URI.
   */
  private def buildUriForAllPairs(): Uri = {
    val pairString = generatePairQueryString(Currency.getAllCurrency)
    Uri(config.baseUrl).withQuery(Uri.Query(pairString))
  }

  /**
   * Builds the URI for fetching rates for a specific currency pair.
   *
   * @param pair The currency pair.
   * @return The constructed URI.
   */
  private def buildUriForPair(pair: Pair): Uri = {
    val pairString = s"${pair.from}${pair.to}"
    Uri(config.baseUrl).withQuery(Uri.Query("pair" -> pairString))
  }

  /**
   * Executes an HTTP request and handles the response.
   *
   * @param uri The URI to send the request to.
   * @return Future containing a list of Forex API responses.
   */
  private def executeRequest(uri: Uri): Future[List[ForexApiResponse]] = {
    val request = HttpRequest(uri = uri)
      .withHeaders(headers.RawHeader("token", config.token))

    ClientUtils.withRetryAndCircuitBreaker {
      Http().singleRequest(request).flatMap(handleHttpResponse)
    }
  }

  /**
   * Handles the HTTP response, unmarshalling it to a list of ForexApiResponse.
   *
   * @param res The HTTP response.
   * @return Future containing a list of ForexApiResponse.
   */
  private def handleHttpResponse(res: HttpResponse): Future[List[ForexApiResponse]] = {
    if (res.status.isSuccess()) {
      logger.info("Received successful response: {}", res)
      Unmarshal(res.entity).to[List[ForexApiResponse]]
    } else {
      logger.warn("Received failure response with status: {}", res.status.intValue())
      res.discardEntityBytes()
      Future.failed(new Exception(s"Cannot process request due to ${res.status.intValue()} status"))
    }
  }

  /**
   * Handles errors during the execution of a request, providing appropriate logging and failure handling.
   *
   * @param ex The exception encountered.
   * @return A Future with a failed result containing the handled error.
   */
  private def handleError[T]: PartialFunction[Throwable, Future[T]] = {
    case ex: Exception =>
      logger.error("Operation failed with error: {}", ex.getMessage)
      Future.failed(new ProxyException(503, "The external service did not respond in time. Please try again later."))
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