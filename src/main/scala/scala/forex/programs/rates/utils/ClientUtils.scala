package scala.forex.programs.rates.utils

import akka.actor.ActorSystem
import akka.pattern.{CircuitBreaker, CircuitBreakerOpenException, after}
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.forex.domain.ProxyException


/**
 * Utility object providing methods for handling retries and circuit breaking.
 */
object ClientUtils {

  private val logger = LoggerFactory.getLogger(getClass)
  implicit val system: ActorSystem = ActorSystem("HttpUtilsSystem")
  implicit val ec: ExecutionContextExecutor = system.dispatcher
  private val breaker: CircuitBreaker = createCircuitBreaker(CircuitBreakerSettings.getCircuitBreakerSettings)
  private val retrySettings = RetrySetting.getRetrySettings

  /**
   * Executes an operation with retry logic and a dynamically-configurable circuit breaker.
   *
   * @param operation The operation to execute.
   * @return A Future containing the result of the operation.
   */
  def withRetryAndCircuitBreaker[T](operation: => Future[T]): Future[T] = {
    breaker.withCircuitBreaker {
      withRetry(retrySettings.maxRetries, retrySettings.delay)(operation)
    }.recoverWith {
      case ex: CircuitBreakerOpenException =>
        logger.error("Circuit breaker is open: Operation cannot proceed.", ex)
        Future.failed(new ProxyException(503, "Service is temporarily unavailable due to circuit breaker being open."))
    }
  }

  /**
   * Executes an operation with a specified number of retries and delay between attempts.
   */
  private def withRetry[T](maxRetries: Int, delay: FiniteDuration)(operation: => Future[T]): Future[T] = {
    def retry(attempt: Int): Future[T] = {
      logger.info("Attempting operation, attempt number: {}", attempt + 1)
      operation.recoverWith {
        case ex if attempt < maxRetries =>
          logger.warn("Operation failed on attempt {}. Retrying in {} seconds. Error: {}", attempt + 1, delay.toSeconds, ex.getMessage)
          after(delay, system.scheduler)(retry(attempt + 1))
      }
    }

    retry(0)
  }

  private def createCircuitBreaker(settings: CircuitBreakerSettings): CircuitBreaker = {
    new CircuitBreaker(
      scheduler = system.scheduler,
      maxFailures = settings.maxFailures,
      callTimeout = settings.callTimeout,
      resetTimeout = settings.resetTimeout
    )
      .onOpen(logger.warn("Circuit Breaker opened: Too many failures."))
      .onHalfOpen(logger.info("Circuit Breaker half-open: Testing if it's ready to close."))
      .onClose(logger.info("Circuit Breaker closed: Ready for normal operation."))

  }
}
