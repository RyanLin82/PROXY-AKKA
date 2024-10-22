package scala.forex.config

import com.typesafe.config.{Config, ConfigFactory}

/**
 * Configuration class for the Forex Proxy Server. It reads the configuration
 * from the application configuration file (e.g., application.conf) under the `forex-proxy-server` path.
 *
 * @constructor Creates a new instance of ApplicationConfig.
 */
class ApplicationConfig {

  private val config: Config = ConfigFactory.load().getConfig("forex-proxy-server")

  val host: String = config.getString("host")
  val port: Int = config.getInt("port")
}

object ApplicationConfig {
  /**
   * Factory method to create a new instance of ServerConfig.
   *
   * @return A new instance of ServerConfig.
   */
  def apply(): ApplicationConfig = new ApplicationConfig()
}
