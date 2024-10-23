package forex.programs.rates.forex

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.Materializer
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.Future
import scala.forex.domain.{Currency, Rate}
import scala.forex.programs.rates.forex.{ForexClient, ForexClientConfig}
import scala.forex.programs.rates.utils.ClientUtils

class ForexClientSpec extends AsyncFlatSpec with Matchers with MockitoSugar with ScalatestRouteTest with BeforeAndAfterAll with ScalaFutures {
  override implicit val system: ActorSystem = ActorSystem("TestSystem")

  override def afterAll(): Unit = {
    system.terminate().onComplete(_ => println("Actor system terminated"))(system.dispatcher)
  }

  implicit val mat: Materializer = Materializer(system)
  val config: ForexClientConfig = new ForexClientConfig {
    override val baseUrl: String = "http://mock-api.com"
    override val token: String = "test-token"
  }

  "ForexClient" should "fetch rates for all currency pairs successfully" in {
    val mockHttpResponse = HttpResponse(
      status = StatusCodes.OK,
      entity = HttpEntity(
        ContentTypes.`application/json`,
        """[{"pair":{"from":"USD","to":"EUR"},"price":{"value":1.12},"timestamp":"2024-10-23T10:15:30Z"}]"""
      )
    )

    when(ClientUtils.withRetryAndCircuitBreaker(any[Future[HttpResponse]]))
      .thenReturn(Future.successful(mockHttpResponse))

    val forexClient = new ForexClient(config)

    forexClient.ratesLookup().map { result =>
      result should have size 1
      result.head.pair.from shouldBe Currency.USD
      result.head.pair.to shouldBe Currency.EUR
      result.head.price.value shouldBe BigDecimal(1.12)
    }
  }

  it should "handle failed HTTP requests by returning an appropriate exception" in {
    val mockHttpResponse = HttpResponse(status = StatusCodes.InternalServerError)

    when(ClientUtils.withRetryAndCircuitBreaker(any[Future[HttpResponse]]))
      .thenReturn(Future.successful(mockHttpResponse))

    val forexClient = new ForexClient(config)

    recoverToExceptionIf[Exception] {
      forexClient.ratesLookupPair(Rate.Pair(Currency.USD, Currency.EUR))
    }.map { ex =>
      ex.getMessage should include("Cannot process request due to 500 status")
    }
  }
}
