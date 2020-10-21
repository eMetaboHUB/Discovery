package inrae.semantic_web.internal

import inrae.semantic_web.rdf._

import scala.concurrent.Future

/*
sealed trait Node {
  var children : Seq[ReferenceNode] = Seq[ReferenceNode]()
  var sources : Seq[String] = Seq[String]()

  def addChildren(n : Node) : Node = {
    children = children :+ n
    return n
  }

  def addSource(s : String) : Node = {
    sources = sources :+ s
    this
  }
}*/

class Node(val uniqRef : Option[String]) {

  var children: Seq[Node] = Seq[Node]()

  def addChildren(n: Node): Node = {
    children = children :+ n
    return n
  }

  def references(): Seq[String] = {

    val l: Seq[String] = uniqRef match {
      case Some(v) => Seq[String](v)
      case None => Seq[String]()
    }

    l ++: children.flatMap(c => c.references())
  }

  def reference(): Option[String] = uniqRef
  
}

/* Filter node */


/* Node case */
case class Root() extends Node(None) {
  var lSourcesNodes : Seq[SourcesNode] = List[SourcesNode]()
  var lOperatorsNode : Seq[OperatorNode] = List[OperatorNode]()

  def sourcesNode(n : Node) : Option[SourcesNode] = {
    lSourcesNodes.find( p => p.n == n )
  }
}

/* triplets */
sealed trait RdfNode
case class Something(concretUniqRef: String) extends Node(Some(concretUniqRef)) with RdfNode
case class SubjectOf(concretUniqRef : String, var uri : URI) extends Node(Some(concretUniqRef)) with RdfNode
case class ObjectOf(concretUniqRef : String, var uri : URI) extends Node(Some(concretUniqRef)) with RdfNode
case class LinkTo(concretUniqRef : String, var term : RdfType) extends Node(Some(concretUniqRef)) with RdfNode
case class LinkFrom(concretUniqRef : String, var uri : URI) extends Node(Some(concretUniqRef)) with RdfNode
case class Attribute(concretUniqRef : String, var uri : URI) extends Node(Some(concretUniqRef)) with RdfNode
case class Value(var rdfterm : RdfType) extends Node(None) with RdfNode

/* Logic */
sealed trait LogicNode
case class UnionBlock( var sire : Node ) extends Node(None) with LogicNode
case class Not( var sire : Node ) extends Node(None) with LogicNode


/* filter */
sealed trait FilterNode
case class isLiteral() extends Node(None) with FilterNode
case class isURI() extends Node(None) with FilterNode

/* SourcesNode */
case class SourcesNode(var n : Node, var sources : Seq[String]) extends Node(n.reference())

/* Operator */
case class OperatorNode(var operator : String ) extends Node(None)