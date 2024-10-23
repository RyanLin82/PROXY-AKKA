package scala.forex.programs.rates.utils

import com.typesafe.config.ConfigFactory

import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.duration.{DurationLong, FiniteDuration}

case class RetrySetting(enabled: Boolean, maxRetries: Int, delay: FiniteDuration)

/**
 * Object for managing dynamic retry settings.
 */
object RetrySetting {
  private val config = ConfigFactory.load()

  private def loadRetrySetting(): RetrySetting = {
    val retryConfig = config.getConfig("client-utils.retry")
    RetrySetting(
      enabled = retryConfig.getBoolean("enabled"),
      maxRetries = retryConfig.getInt("max-retries"),
      delay = retryConfig.getDuration("delay").toMillis.millis
    )
  }

  private val retrySettingsRef = new AtomicReference(loadRetrySetting())

  /**
   * Retrieves the current retry settings.
   *
   * @return The current RetrySettings.
   */
  def getRetrySettings: RetrySetting = retrySettingsRef.get()

  /**
   * Updates the retry settings.
   *
   * @param newSettings The new RetrySettings to apply.
   */
  def setRetrySettings(newSettings: RetrySetting): Unit = retrySettingsRef.set(newSettings)
}
