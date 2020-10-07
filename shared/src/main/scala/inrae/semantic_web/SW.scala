package inrae.semantic_web
import scala.scalajs.js.annotation._
import java.util.UUID.randomUUID

import inrae.semantic_web.rdf._
import inrae.semantic_web.internal._
import inrae.semantic_web.sparql._

import scala.concurrent.{Future}

class SW(var config: StatementConfiguration) {

  /* root node */
  private var rootNode   : Root = new Root()
  /* focus node */
  private var focusNode  : Node = rootNode

  def print() : Unit = {
    println(" - SW -");
    println(" -- root --");
    //pprint.pprintln(rootNode.children)
    println(" -- focusNode --");
    //pprint.pprintln(focusNode.children)
  }

  /* manage the creation of an unique ref */
  def getUniqueRef() : String = randomUUID.toString

  /* set the current focus on the select node */
  def focus(ref : String) : SW = {
    var sn = new pm.SelectNode();
    focusNode = sn.setFocus(ref, rootNode)(0)

    return this
  }

  def focusManagement(n : Node) : SW = {
    focusNode.addChildren(n)
    /* current node is the focusNode */
    focusNode = n
    return this
  }

  /* start a request */
  def something( ref : String = getUniqueRef() ) : SW = {
    val lastNode = new Something(ref)
    /* special case when "somthing" is used. become the focus */
    focusManagement(lastNode)
  }

  /* create node which focus is the subject : ?focusId <uri> ?target */
  def isSubjectOf( uri : URI , ref : String = getUniqueRef() ) : SW = {
    val lastNode = new SubjectOf(ref,uri)
    focusManagement(lastNode)
  }


  /* create node which focus is the subject : ?focusId <uri> ?target */
  def isObjectOf( uri : URI , ref : String = getUniqueRef() ) : SW = {
    val lastNode = new ObjectOf(ref,uri)
    focusManagement(lastNode)
  }

  /* set */
  def set( uri : URI ) : SW = {
    val lastNode = new Value(uri)
    focusManagement(lastNode)
  }

  def debug() : SW = {
    var sc = new pm.SimpleConsole();
    //println( pprint.tokenize(rootNode).mkString )
    //pprint.pprintln(rootNode.children)
    println("--focus--")
    //pprint.pprintln(focusNode)
    //pprint.pprintln(focusNode.children)
    //rootNode.accept(sc)
    println(sc.get(rootNode))
    return this
  }

  def sparql() : String = {
    var sg = new pm.SparqlGenerator();
    return sg.body(config, rootNode)
  }

  def select() : Future[QueryResult] = {
    val sg = new pm.SparqlGenerator()
    val query = sg.prolog(config, rootNode ) + "\n" + sg.body(config, rootNode ) + sg.solutionModifier(config, rootNode)
    println(" ------------------------------- SPARQL ----------------------------- ")
    println(query)
    println(" ------------------------------- RESULT ----------------------------- ")

    //try {
    val futuresResults = config.conf.sources
        .map(source => QueryRunner(source))
        .map(runner => runner.query(query))

    import scala.concurrent.ExecutionContext.Implicits.global

    Future.reduceLeft(futuresResults)((a, b) => a)
    //} catch {
     // case e : Exception => { println(" ---- **  None source are defined ** ----- ") ;  }
    //}
  }

}