package scala.forex.config

import akka.event.Logging.LogLevel
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.{DebuggingDirectives, LoggingMagnet}
import akka.stream.Materializer
import akka.util.ByteString

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object LoggingDirective {

  def logRequestEntity(route: Route, level: LogLevel)
                      (implicit m: Materializer, ex: ExecutionContext): Route = {

    def requestEntityLoggingFunction(loggingAdapter: LoggingAdapter)(req: HttpRequest): Unit = {
      val timeout = 900.millis
      val bodyAsBytes: Future[ByteString] = req.entity.toStrict(timeout).map(_.data)
      val bodyAsString: Future[String] = bodyAsBytes.map(_.utf8String)
      bodyAsString.onComplete {
        case Success(body) =>
          if (body.isEmpty) {
            loggingAdapter.log(level, "")
          } else {
            val logMsg = s"$req\nRequest body: $body"
            loggingAdapter.log(level, logMsg)
          }
        case Failure(t) =>
          val logMsg = s"Failed to get the body for: $req"
          loggingAdapter.error(t, logMsg)
      }
    }

    DebuggingDirectives.logRequest(LoggingMagnet(requestEntityLoggingFunction))(route)
  }
}
