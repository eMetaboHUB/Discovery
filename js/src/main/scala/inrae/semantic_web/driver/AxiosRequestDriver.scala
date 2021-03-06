package inrae.semantic_web.driver

import facade.npm.{Axios, qs}
import inrae.semantic_web.SWDiscoveryException
import inrae.semantic_web.event.{DiscoveryRequestEvent, DiscoveryStateRequestEvent}
import inrae.semantic_web.sparql.QueryResult
import wvlet.log.Logger.rootLogger.debug

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.{Dynamic, JSON, URIUtils}


case class AxiosRequestDriver(
                               idName : String,
                               method : String,
                               url: String,
                               login : String,
                               password: String,
                               token : String,
                               auth : String)
  extends HttpRequestDriver {

  def requestOnSWDB(query: String): Future[QueryResult] = {
    debug(" -- HttpRequestDriver > " + this.getClass.getName )
    publish(DiscoveryRequestEvent(DiscoveryStateRequestEvent.START_HTTP_REQUEST))
    method.toLowerCase() match {
      case "post" => post(query)
      case "get" => get(query)
      case _ => {
        throw HttpRequestDriverException(s"Unknown http type request : ${method}")
      }
    }
  }


  def get(query: String): Future[QueryResult] = {
    publish(DiscoveryRequestEvent(DiscoveryStateRequestEvent.PROCESS_HTTP_REQUEST))

    val configAxios = Dynamic.literal(
       "header" -> Dynamic.literal(
         "Accept" -> "application/json"
       )
     )

   Axios.get(url+"?query="+URIUtils.encodeURIComponent(query),configAxios).toFuture.map(response => {
     publish(DiscoveryRequestEvent(DiscoveryStateRequestEvent.FINISHED_HTTP_REQUEST))
     QueryResult(JSON.stringify(response.data))
   }).recover(
     e => {
       publish(DiscoveryRequestEvent(DiscoveryStateRequestEvent.ERROR_HTTP_REQUEST))
       throw SWDiscoveryException(e.getMessage)
     } )
 }

 def post(query: String): Future[QueryResult] = {
   publish(DiscoveryRequestEvent(DiscoveryStateRequestEvent.PROCESS_HTTP_REQUEST))

   val configAxios = Dynamic.literal(
     "url" -> url,
     "method" -> "POST",
     "header" -> Dynamic.literal(
       "Accept" -> "application/json",
       "Content-Type" -> "application/x-www-form-urlencoded"
     ),
     "data" -> qs.stringify(js.Dictionary[String](
       "query" -> query
     ))
   )

   Axios.request(configAxios).toFuture.map( response => {
     publish(DiscoveryRequestEvent(DiscoveryStateRequestEvent.FINISHED_HTTP_REQUEST))
     QueryResult(JSON.stringify(response.data))
   }).recover(
     e => {
       publish(DiscoveryRequestEvent(DiscoveryStateRequestEvent.ERROR_HTTP_REQUEST))
       throw SWDiscoveryException(e.getMessage)
     } )
 }

}
