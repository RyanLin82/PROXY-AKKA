package scala.forex.programs.rates.utils

import com.typesafe.config.ConfigFactory

import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.duration.{DurationLong, FiniteDuration}

case class CircuitBreakerSettings(
                                   enabled: Boolean,
                                   maxFailures: Int,
                                   callTimeout: FiniteDuration,
                                   resetTimeout: FiniteDuration
                                 )

/**
 * Object for managing dynamic retry settings.
 */
object CircuitBreakerSettings {
  private val config = ConfigFactory.load()

  private val circuitBreakerSettingsRef = new AtomicReference(loadCircuitBreakerSettings())

  def getCircuitBreakerSettings: CircuitBreakerSettings = circuitBreakerSettingsRef.get()

  def setCircuitBreakerSettings(newSettings: CircuitBreakerSettings): Unit = circuitBreakerSettingsRef.set(newSettings)

  private def loadCircuitBreakerSettings(): CircuitBreakerSettings = {
    val cbConfig = config.getConfig("client-utils.circuit-breaker")
    CircuitBreakerSettings(
      enabled = cbConfig.getBoolean("enabled"), // Read the enabled status from config
      maxFailures = cbConfig.getInt("max-failures"),
      callTimeout = cbConfig.getDuration("call-timeout").toMillis.millis,
      resetTimeout = cbConfig.getDuration("reset-timeout").toMillis.millis
    )
  }

}
