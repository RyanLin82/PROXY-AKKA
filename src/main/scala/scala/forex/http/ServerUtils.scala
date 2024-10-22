package scala.forex.http

import akka.http.scaladsl.model.{StatusCode, StatusCodes, Uri}
import akka.http.scaladsl.server.Directives.{complete, extractUri, optionalHeaderValueByName, provide, redirect}
import akka.http.scaladsl.server.{Directive1, ExceptionHandler, Route}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.syntax.EncoderOps
import org.slf4j.LoggerFactory

import scala.concurrent.TimeoutException
import scala.forex.domain.ProxyException
import scala.forex.http.domain.ProxyErrorResponse


/**
 * Utility object for server-related functionalities, including exception handling,
 * route adjustments, and security checks.
 */
object ServerUtils {

  private val logger = LoggerFactory.getLogger(getClass)

  /**
   * Implicit exception handler for handling common server exceptions.
   *
   * @return An instance of ExceptionHandler.
   */
  implicit def exceptionHandler: ExceptionHandler = ExceptionHandler {
    case ex: TimeoutException =>
      logger.warn("Handler exception: timeout", ex)
      val errorResponse = ProxyErrorResponse(
        status = StatusCodes.RequestTimeout.intValue,
        errorCode = "408",
        message = "Request timed out"
      )
      complete(StatusCodes.RequestTimeout -> errorResponse.asJson)
    case ex: IllegalArgumentException =>
      logger.warn("Handler exception: invalid argument", ex)
      val errorResponse = ProxyErrorResponse(
        status = StatusCodes.BadRequest.intValue,
        errorCode = "400",
        message = ex.getMessage
      )
      complete(StatusCodes.BadRequest -> errorResponse.asJson)

    case ex: ProxyException =>
      logger.warn("Handler exception: circuit breaker open", ex)
      val errorResponse = ProxyErrorResponse(
        status = StatusCodes.TooManyRequests.intValue,
        errorCode = ex.getCode.toString,
        message = ex.getMessage
      )
      complete(StatusCode.int2StatusCode(ex.getCode) -> errorResponse.asJson)

    case ex: Exception =>
      logger.error("Handler exception", ex)
      val errorResponse = ProxyErrorResponse(
        status = StatusCodes.InternalServerError.intValue,
        errorCode = "500",
        message = "Internal server error"
      )
      complete(StatusCodes.InternalServerError -> errorResponse.asJson)
  }

  /**
   * Removes trailing slashes from URIs and redirects to the cleaned path if necessary.
   *
   * @param route The original route.
   * @return A modified route that handles trailing slashes.
   */
  def removeTrailingSlash(route: Route): Route = {
    extractUri { path =>
      val cleanedPath = path.toString().stripSuffix("/")
      if (cleanedPath == path.toString()) {
        route
      } else {
        logger.info("Redirecting to path without trailing slash: {}", cleanedPath)
        redirect(Uri(cleanedPath), StatusCodes.PermanentRedirect)
      }
    }
  }

  /**
   * Checks the `Authorization` header for a valid token.
   *
   * @return A directive that provides the token if it is valid, or responds with Unauthorized.
   */
  def securityCheck: Directive1[String] = {
    optionalHeaderValueByName("Authorization").flatMap {
      case Some(token) if token == "Bearer test-token" =>
        logger.info("Authorization successful for token: {}", token)
        provide(token)
      case _ =>
        logger.warn("Authorization failed: Missing or invalid token")
        complete(StatusCodes.Unauthorized -> "Missing or invalid token")
    }
  }
}
