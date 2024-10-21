package scala.forex.programs.forex

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri, headers}
import scala.concurrent.Future
import scala.forex.domain._


class ForexClient(implicit system: ActorSystem) {


  def ratesLookup(pairs: Rate.Pair): Future[HttpResponse] = {
    val pairString = s"${pairs.from}${pairs.to}"
    val uri = Uri("http://localhost:8080/rates").withQuery(Uri.Query("pair" -> pairString))

    val request = HttpRequest(uri = uri)
      .withHeaders(
        headers.RawHeader("token", "10dc303535874aeccc86a8251e6992f5")
      )
    Http().singleRequest(request)

  }

}


