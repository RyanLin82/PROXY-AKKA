package scala.forex

import akka.actor.{ActorSystem, Scheduler}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{StatusCodes, Uri}
import akka.http.scaladsl.server.Directives.{complete, extractUri, handleExceptions, optionalHeaderValueByName, provide, redirect}
import akka.http.scaladsl.server.{Directive1, ExceptionHandler, Route}
import akka.stream.Materializer

import scala.concurrent.{ExecutionContext, TimeoutException}
import scala.forex.http.rates.RatesHttpRoutes
import scala.forex.programs.forex.ForexClient
import scala.forex.services.RatesService

object Main extends App {
  implicit val system: ActorSystem = ActorSystem("ForexProxySystem")
  implicit val materializer: Materializer = Materializer(system)
  implicit val ec: ExecutionContext = system.dispatcher
  implicit val scheduler: Scheduler = system.scheduler

  val externalClient = new ForexClient()
  val proxyService = new RatesService(externalClient)
  val ratesHttpRoutes = new RatesHttpRoutes(proxyService)

  implicit def exceptionHandler: ExceptionHandler = ExceptionHandler {
    case ex: TimeoutException =>
      println("handler exception timeout")
      complete(StatusCodes.RequestTimeout -> s"Request timed out: ${ex.getMessage}")
    case ex: Exception =>
      println("handler exception")
      complete(StatusCodes.InternalServerError -> s"Internal server error: ${ex.getMessage}")
  }

  // Matches `/rates` or `/rates/` and redirects them to `/rates`
  private def removeTrailingSlash(route: Route): Route = {
    extractUri { path =>
      val cleanedPath = path.toString().stripSuffix("/")
      if (cleanedPath == path.toString()) {
        route
      } else {
        redirect(Uri(cleanedPath), StatusCodes.PermanentRedirect)
      }
    }
  }

  // Custom security check directive to verify token
  def securityCheck: Directive1[String] = {
    optionalHeaderValueByName("Authorization").flatMap {
      case Some(token) if token == "Bearer test-token" => provide(token)
      case _ => complete(StatusCodes.Unauthorized -> "Missing or invalid token")
    }
  }

  val routes: Route = handleExceptions(exceptionHandler) {
    removeTrailingSlash {
      securityCheck { _=>
        ratesHttpRoutes.routes
      }
    }
  }

  val serverFuture = Http().newServerAt("localhost", 8090).bindFlow(routes)

  serverFuture.onComplete {
    case scala.util.Success(_) =>
      println("Server running at http://localhost:8090/")
    case scala.util.Failure(ex) =>
      println(s"Failed to bind: ${ex.getMessage}")
  }
}
