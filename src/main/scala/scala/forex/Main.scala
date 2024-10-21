package scala.forex

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.Materializer

import scala.concurrent.ExecutionContext
import scala.forex.http.rates.RatesHttpRoutes
import scala.forex.programs.forex.ForexClient
import scala.forex.services.RatesService

object Main extends App {
  implicit val system: ActorSystem = ActorSystem("ForexProxySystem")
  implicit val materializer: Materializer = Materializer(system)
  implicit val ec: ExecutionContext = system.dispatcher

  val externalClient = new ForexClient()
  val proxyService = new RatesService(externalClient)
  val ratesHttpRoutes = new RatesHttpRoutes(proxyService)

  val routes: Route = ratesHttpRoutes.routes
  val serverFuture = Http().newServerAt("localhost", 8090).bindFlow(routes)

  serverFuture.onComplete {
    case scala.util.Success(_) =>
      println("Server running at http://localhost:8090/")
    case scala.util.Failure(ex) =>
      println(s"Failed to bind: ${ex.getMessage}")
  }
}
