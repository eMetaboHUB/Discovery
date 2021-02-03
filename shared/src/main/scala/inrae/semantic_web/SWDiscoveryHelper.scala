package inrae.semantic_web

import inrae.semantic_web.rdf.{QueryVariable, SparqlBuilder, URI}
import wvlet.log.Logger.rootLogger.debug

import scala.concurrent.Future

case class SWDiscoveryHelper(sw : SWDiscovery) {
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  def count : Future[Int] = {
    SWTransaction(sw
      .root()
      .projection(Seq())
      .agg_projection("count")
      .countAll().console())
      .commit()
      .raw
      .map( json => {
        SparqlBuilder.createLiteral(json("results")("bindings")(0)("count")).toInt()
      })
  }

  /**
   * Discovery search functionalities
   *
   */

  def findClasses(motherClass: URI = URI("") ) : Future[Seq[URI]] = {
    debug(" -- findClasses -- ")
    (motherClass match {
      case uri : URI if uri == URI("")  => sw.isSubjectOf(URI("a"),"_esp___type")
      case _ : URI =>  sw.isSubjectOf(URI("a"),"_esp___type")
        .isSubjectOf(URI("a"))
        .set(motherClass)
    })
      .focus("_esp___type")
      .select(List("_esp___type"))
      .commit()
      .raw
      .map( json => {
        json("results")("bindings").arr.map(
          row => SparqlBuilder.createUri(row("_esp___type"))
        ).toSeq
      })
  }

  def findProperties(motherClassProperties: URI = URI("") , kind : String = "objectProperty" ) : Future[Seq[URI]] = {
    debug(" -- findProperties -- ")

    /* inherited from something ??? */
    val state = if (motherClassProperties != URI("")) {
      sw.root()
        .something("_esp___type")
        .focus(sw.focusNode)
        .isLinkTo(QueryVariable("_esp___type"),"_esp___property").isSubjectOf(URI("a"))
        .set(motherClassProperties)
    } else {
      sw.root()
        .something("_esp___type")
        .focus(sw.focusNode)
        .isLinkTo(QueryVariable("_esp___type"),"_esp___property")
    }

    /* object or datatype properties owl def. */
    ( kind  match {
      case "objectProperty" => state.focus("_esp___type").filter.isUri
      case "datatypeProperty" => state.focus("_esp___type").filter.isLiteral
      case _ => state
    }).select(List("_esp___property"))
      .commit()
      .raw
      .map( json => {
        json("results")("bindings").arr.map(
          row => {
            SparqlBuilder.createUri(row("_esp___property")) }
        ).toSeq
      })
  }

  def findObjectProperties(motherClassProperties: URI = URI("") ) : Future[Seq[URI]] = {
    debug(" -- findObjectProperties -- ")
    findProperties(motherClassProperties)
  }
  def findDatatypeProperties(motherClassProperties: URI = URI("") ) : Future[Seq[URI]] = {
    debug(" -- findDatatypeProperties -- ")
    findProperties(motherClassProperties,"datatypeProperty")
  }
}
