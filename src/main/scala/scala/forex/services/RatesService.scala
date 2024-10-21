package scala.forex.services

import akka.http.scaladsl.model.HttpResponse

import scala.concurrent.Future
import scala.forex.domain.Rate
import scala.forex.programs.forex.ForexClient

class RatesService(externalClient: ForexClient) {

  def fetchRatesForMultiplePairs(pair: Rate.Pair): Future[HttpResponse] = {
    externalClient.ratesLookup(pair)
  }
}