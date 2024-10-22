package scala.forex.programs.clientUtils

import akka.actor.ActorSystem
import akka.pattern.{CircuitBreaker, after}

import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.concurrent.duration._

case class RetrySettings(maxRetries: Int, delay: FiniteDuration)

object HttpUtils {

  implicit val system: ActorSystem = ActorSystem("HttpUtilsSystem")
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  private val breaker = new CircuitBreaker(
    scheduler = system.scheduler,
    maxFailures = 5,
    callTimeout = 1.seconds,
    resetTimeout = 20.seconds
  )
    .onOpen(println("Circuit Breaker opened: Too many failures."))
    .onHalfOpen(println("Circuit Breaker half-open: Testing if it's ready to close."))
    .onClose(println("Circuit Breaker closed: Ready for normal operation."))

  // Helper method for retry logic
  private def retry[T](times: Int, delay: FiniteDuration)(block: => Future[T]): Future[T] = {
    println(times)
    block.recoverWith {
      case _ if times > 0 =>
        after(delay, system.scheduler)(retry(times - 1, delay)(block))
    }
  }


  def withDynamicRetry[T](operation: => Future[T]): Future[T] = {
    val settings = DynamicSettings.getRetrySettings
    withRetry(settings.maxRetries, settings.delay)(operation)
  }

  def withRetry[T](maxRetries: Int, delay: FiniteDuration)(operation: => Future[T]): Future[T] = {
    def retry(attempt: Int): Future[T] = {
      println(attempt)
      operation.recoverWith {
        case _ if attempt < maxRetries => {
          println(attempt)
          after(delay, system.scheduler)(retry(attempt + 1))
        }
      }
    }

    retry(0)
  }

  // Utility method to combine retry and circuit breaker
  def withRetryAndCircuitBreaker[T](operation: => Future[T]): Future[T] = {
    val settings = DynamicSettings.getRetrySettings
    breaker.withCircuitBreaker {
      withRetry(settings.maxRetries, settings.delay)(operation)
    }
  }
}

object DynamicSettings {
  private val retrySettingsRef = new AtomicReference(RetrySettings(maxRetries = 3, delay = 1.second))

  def getRetrySettings: RetrySettings = retrySettingsRef.get()
  def setRetrySettings(newSettings: RetrySettings): Unit = retrySettingsRef.set(newSettings)

}