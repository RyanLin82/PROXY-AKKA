package scala.forex

import akka.actor.{ActorSystem, Scheduler}
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.handleExceptions
import akka.http.scaladsl.server.Route
import akka.stream.{Materializer, SystemMaterializer}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.forex.config.LoggingDirective.logRequestEntity
import scala.forex.config.{ApplicationConfig, ServerUtils}
import scala.forex.http.rates.RatesHttpRoutes
import scala.forex.programs.rates.forex.ForexClient
import scala.forex.services.rates.RatesService
import scala.util.{Failure, Success}

/**
 * Entry point for the Forex Proxy Server application.
 * Initializes the necessary components and starts the server.
 */
object Main extends App {
  private val logger = LoggerFactory.getLogger(getClass)

  implicit val system: ActorSystem = ActorSystem("ForexProxySystem")
  implicit val materializer: Materializer = SystemMaterializer(system).materializer
  implicit val ec: ExecutionContext = system.dispatcher
  implicit val scheduler: Scheduler = system.scheduler

  // Load configurations
  private val serverConfig = ApplicationConfig()
  val externalClient = new ForexClient()
  val proxyService = new RatesService(externalClient)
  private val ratesHttpRoutes = new RatesHttpRoutes(proxyService)

  private val routes: Route = handleExceptions(ServerUtils.exceptionHandler) {
    ServerUtils.removeTrailingSlash {
      ServerUtils.securityCheck { _ =>
        ratesHttpRoutes.routes
      }
    }
  }
  private val loggedRoute = logRequestEntity(routes, Logging.InfoLevel)

  private val serverFuture: Future[Http.ServerBinding] = Http().newServerAt(serverConfig.host, serverConfig.port).bindFlow(loggedRoute)

  serverFuture.onComplete {
    case Success(_) =>
      logger.info("Server running at http://{}:{}/", serverConfig.host, serverConfig.port)
    case Failure(ex) =>
      logger.error("Failed to bind server. Error: {}", ex.getMessage)
      system.terminate()
  }
}

