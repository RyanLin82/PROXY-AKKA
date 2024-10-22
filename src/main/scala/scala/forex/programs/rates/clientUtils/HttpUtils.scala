package scala.forex.programs.rates.clientUtils

import akka.actor.ActorSystem
import akka.pattern.{CircuitBreaker, CircuitBreakerOpenException, after}
import org.slf4j.LoggerFactory

import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.concurrent.duration._

case class RetrySettings(maxRetries: Int, delay: FiniteDuration)

/**
 * Utility object providing methods for handling retries and circuit breaking.
 */
object HttpUtils {

  // Initialize logger
  private val logger = LoggerFactory.getLogger(getClass)

  implicit val system: ActorSystem = ActorSystem("HttpUtilsSystem")
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  // Define a Circuit Breaker with custom settings and logging hooks
  private val breaker = new CircuitBreaker(
    scheduler = system.scheduler,
    maxFailures = 5,
    callTimeout = 1.second,
    resetTimeout = 20.seconds
  )
    .onOpen(logger.warn("Circuit Breaker opened: Too many failures."))
    .onHalfOpen(logger.info("Circuit Breaker half-open: Testing if it's ready to close."))
    .onClose(logger.info("Circuit Breaker closed: Ready for normal operation."))

  /**
   * Helper method to perform an operation with retry logic.
   *
   * @param times Number of retry attempts.
   * @param delay Delay between retries.
   * @param block The operation to execute.
   * @return A Future containing the result of the operation.
   */
  private def retry[T](times: Int, delay: FiniteDuration)(block: => Future[T]): Future[T] = {
    logger.info("Attempting retry: {} attempts left", times)
    block.recoverWith {
      case ex if times > 0 =>
        logger.warn("Operation failed. Retrying in {} seconds. Error: {}", delay.toSeconds, ex.getMessage)
        after(delay, system.scheduler)(retry(times - 1, delay)(block))
    }
  }

  /**
   * Executes an operation with retry logic based on dynamic settings.
   *
   * @param operation The operation to execute.
   * @return A Future containing the result of the operation.
   */
  def withDynamicRetry[T](operation: => Future[T]): Future[T] = {
    val settings = DynamicSettings.getRetrySettings
    withRetry(settings.maxRetries, settings.delay)(operation)
  }

  /**
   * Executes an operation with a specified number of retries and delay between attempts.
   *
   * @param maxRetries The maximum number of retries.
   * @param delay The delay between retries.
   * @param operation The operation to execute.
   * @return A Future containing the result of the operation.
   */
  def withRetry[T](maxRetries: Int, delay: FiniteDuration)(operation: => Future[T]): Future[T] = {
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

  def withRetryAndCircuitBreaker[T](operation: => Future[T]): Future[T] = {
    val settings = DynamicSettings.getRetrySettings
    breaker.withCircuitBreaker {
      withRetry(settings.maxRetries, settings.delay)(operation)
    }.recoverWith {
      case ex: CircuitBreakerOpenException =>
        logger.error("Circuit breaker is open: Operation cannot proceed.", ex)
        Future.failed(new Exception("Service is temporarily unavailable due to circuit breaker being open."))
    }
  }
}

/**
 * Object for managing dynamic retry settings.
 */
object DynamicSettings {
  private val retrySettingsRef = new AtomicReference(RetrySettings(maxRetries = 3, delay = 1.second))

  /**
   * Retrieves the current retry settings.
   *
   * @return The current RetrySettings.
   */
  def getRetrySettings: RetrySettings = retrySettingsRef.get()

  /**
   * Updates the retry settings.
   *
   * @param newSettings The new RetrySettings to apply.
   */
  def setRetrySettings(newSettings: RetrySettings): Unit = retrySettingsRef.set(newSettings)
}