package inrae.semantic_web.driver

import inrae.semantic_web.SWDiscoveryException
import inrae.semantic_web.event.{DiscoveryRequestEvent, DiscoveryStateRequestEvent}
import inrae.semantic_web.sparql.QueryResult
import org.eclipse.rdf4j.query.resultio.sparqljson.SPARQLResultsJSONWriter
import org.eclipse.rdf4j.repository.RepositoryConnection

import java.io.ByteArrayOutputStream
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

trait Rdf4jRequestDriver extends RequestDriver {

  def requestConnexionRepository(con : RepositoryConnection, query : String): Future[QueryResult] = {
    Future {
      publish(DiscoveryRequestEvent(DiscoveryStateRequestEvent.START_HTTP_REQUEST))
      publish(DiscoveryRequestEvent(DiscoveryStateRequestEvent.PROCESS_HTTP_REQUEST))
      val out = new ByteArrayOutputStream()
      /*
              val result = repo.getConnection()
                .prepareTupleQuery(query).evaluate()
              while (result.hasNext()) println(result.next())
      */
      Try(con
        .prepareTupleQuery(query)
        .evaluate(new SPARQLResultsJSONWriter(out))) match {
        case Success(_) => {
          publish(DiscoveryRequestEvent(DiscoveryStateRequestEvent.FINISHED_HTTP_REQUEST))
          publish(DiscoveryRequestEvent(DiscoveryStateRequestEvent.RESULTS_BUILD))
          val response = inrae.semantic_web.sparql.QueryResult(out.toString())
          publish(DiscoveryRequestEvent(DiscoveryStateRequestEvent.RESULTS_DONE))
          response
        }
        case Failure(e) => {
          publish(DiscoveryRequestEvent(DiscoveryStateRequestEvent.ERROR_HTTP_REQUEST))
          throw SWDiscoveryException("\n"+query+"\n"+e.getMessage)
        }
      }
    }
  }
}
