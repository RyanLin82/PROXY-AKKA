package forex.config

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.{Location, RawHeader}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.forex.config.ServerUtils
import scala.forex.domain.ProxyException
import scala.forex.http.domain.ProxyErrorResponse

class ServerUtilsSpec extends AnyFlatSpec with Matchers with ScalatestRouteTest {

  "ExceptionHandler" should "handle IllegalArgumentException correctly" in {
    val route = Route.seal {
      handleExceptions(ServerUtils.exceptionHandler) {
        complete {
          throw new IllegalArgumentException("Invalid argument provided")
        }
      }
    }

    Get("/") ~> route ~> check {
      status shouldBe StatusCodes.BadRequest
      val response = responseAs[ProxyErrorResponse]
      response.status shouldBe 400
      response.errorCode shouldBe "400"
      response.message shouldBe "Invalid argument provided"
    }
  }

  it should "handle a mocked IllegalArgumentException" in {
    val route = Route.seal {
      handleExceptions(ServerUtils.exceptionHandler) {
        complete {
          throw new IllegalArgumentException("Invalid argument provided")
        }
      }
    }

    Get("/") ~> route ~> check {
      status shouldBe StatusCodes.BadRequest
      val response = responseAs[ProxyErrorResponse]
      response.status shouldBe 400
      response.errorCode shouldBe "400"
      response.message shouldBe "Invalid argument provided"
    }
  }

  it should "handle a mocked ProxyException" in {
    val proxyException = new ProxyException(429, "Too many requests")

    val route = Route.seal {
      handleExceptions(ServerUtils.exceptionHandler) {
        complete {
          throw proxyException
        }
      }
    }

    Get("/") ~> route ~> check {
      status shouldBe StatusCodes.TooManyRequests
      val response = responseAs[ProxyErrorResponse]
      response.status shouldBe 429
      response.errorCode shouldBe "429"
      response.message shouldBe "Too many requests"
    }
  }

  it should "handle a mocked RuntimeException" in {
    val route = Route.seal {
      handleExceptions(ServerUtils.exceptionHandler) {
        complete {
          throw new RuntimeException("Unexpected error")
        }
      }
    }

    Get("/") ~> route ~> check {
      status shouldBe StatusCodes.InternalServerError
      val response = responseAs[ProxyErrorResponse]
      response.status shouldBe 500
      response.errorCode shouldBe "500"
      response.message shouldBe "Internal server error"
    }
  }

  val routeWithoutSlash: Route = complete("No trailing slash")

  "removeTrailingSlash" should "redirect requests with a trailing slash" in {
    val route = Route.seal(ServerUtils.removeTrailingSlash(routeWithoutSlash))

    Get("http://example.com/example/") ~> route ~> check {
      status shouldBe StatusCodes.PermanentRedirect
      header[Location].get.uri.toString() shouldBe "http://example.com/example"
    }
  }

  it should "not redirect requests without a trailing slash" in {
    val route = Route.seal(ServerUtils.removeTrailingSlash(routeWithoutSlash))

    Get("http://example.com/example") ~> route ~> check {
      status shouldBe StatusCodes.OK
      responseAs[String] shouldBe "No trailing slash"
    }
  }

  "securityCheck" should "authorize a valid token" in {
    val route = Route.seal(ServerUtils.securityCheck { token =>
      complete(s"Authorized with token: $token")
    })

    Get("/").withHeaders(RawHeader("Authorization", "Bearer test-token")) ~> route ~> check {
      status shouldBe StatusCodes.OK
      responseAs[String] shouldBe "Authorized with token: Bearer test-token"
    }
  }

  it should "deny access for a missing token" in {
    val route = Route.seal(ServerUtils.securityCheck { token =>
      complete(s"Authorized with token: $token")
    })

    Get("/") ~> route ~> check {
      status shouldBe StatusCodes.Unauthorized
      responseAs[String] shouldBe "Missing or invalid token"
    }
  }

  it should "deny access for an invalid token" in {
    val route = Route.seal(ServerUtils.securityCheck { token =>
      complete(s"Authorized with token: $token")
    })

    Get("/").withHeaders(RawHeader("Authorization", "InvalidToken")) ~> route ~> check {
      status shouldBe StatusCodes.Unauthorized
      responseAs[String] shouldBe "Missing or invalid token"
    }
  }
}
