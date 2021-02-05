package inrae.semantic_web.rdf
import upickle.default.{ReadWriter => RW, macroRW}
import scala.scalajs.js.annotation.JSExportTopLevel
import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

case class Graph(triples : Set[Triple])

case class Triple(s: SparqlDefinition, p: SparqlDefinition, o: SparqlDefinition)

sealed trait SparqlDefinition {

  def sparql : String

  def naiveLabel : String
}

object SparqlDefinition {

  implicit val rw: RW[SparqlDefinition] = RW.merge(
    IRI.rw,
    URI.rw,
    Anonymous.rw,
    PropertyPath.rw,
    Literal.rw,
    QueryVariable.rw,
  )

  def cleanString(str : String) = {
    str.replaceAll("^\"","")
      .replaceAll("\"$","")
      .replaceAll("^<","")
      .replaceAll(">$","")
  }
}

object IRI {
  implicit def fromString(s: String): IRI = IRI(s)
  implicit val rw: RW[IRI] = macroRW
}

@JSExportTopLevel(name="IRI")
case class IRI (var iri : String) extends SparqlDefinition {
  iri = SparqlDefinition.cleanString(iri)
  override def toString : String = {
      "<"+iri+">"
  }
  def sparql : String = toString

  def naiveLabel : String = iri.split("[/#]").last

}

object URI {
  implicit val rw: RW[URI] = macroRW

  implicit def fromString(s: String): URI = URI(s)

  val empty = new URI("")
}


@JSExportTopLevel(name="URI")
case class URI (localNameUser : String,nameSpaceUser : String = "") extends SparqlDefinition {
  val localName = nameSpaceUser match {
    case "" if (!localNameUser.contains("://")) => {
      SparqlDefinition.cleanString(localNameUser.split(":").last)
    }
    case _ => SparqlDefinition.cleanString(localNameUser)
  }

  val nameSpace = nameSpaceUser match {
    case "" if (!localNameUser.contains("://")) => {
      localNameUser.split(":") match {
        case arr if (arr.length==2) => arr(0)
        case _ => "" /* something wrong if arity if different that 2 */
      }
    }
    case _ => nameSpaceUser
  }

  override def toString : String = {
    (localName,nameSpace) match {
      case ("a",_) => "a"
      case (_,"") => "<"+localName+">"
      case _ => nameSpace + ":" + localName
    }
  }

  def sparql : String = toString

  def naiveLabel : String = localName.split("[/#]").last
}


object Anonymous {

  implicit val rw: RW[Anonymous] = macroRW

  implicit def fromString(s: String): Anonymous = Anonymous(s)
}

@JSExportTopLevel(name="Anonymous")
case class Anonymous(var value : String) extends SparqlDefinition {
  value = SparqlDefinition.cleanString(value)

  override def toString : String = value

  def sparql : String = toString

  def naiveLabel : String = s"Anonymous[$value]"
}

@JSExportTopLevel(name="PropertyPath")
case class PropertyPath(var value : String) extends SparqlDefinition {
  value = SparqlDefinition.cleanString(value)

  override def toString : String = value

  def sparql : String = toString

  def naiveLabel : String = s"PropertyPath[$value]"
}

object PropertyPath {

  implicit val rw: RW[PropertyPath] = macroRW

  implicit def fromString(s: String): PropertyPath = PropertyPath(s)
}

object Literal {
  implicit val rw: RW[Literal] = macroRW

  implicit def fromString(s: String): Literal = Literal(s)
  implicit def fromString(s: Int): Literal = Literal(s.toString,URI("integer","xsd"))
  implicit def fromString(s: Boolean): Literal = Literal(s.toString,URI("boolean","xsd"))
  implicit def fromString(s: Double): Literal = Literal(s.toString,URI("double","xsd"))
  implicit def fromString(s: Float): Literal = Literal(s.toString,URI("float","xsd"))
}

@JSExportTopLevel(name="Literal")
case class Literal(var value : String,var datatype : URI = URI.empty,var tag : String="") extends SparqlDefinition {
  value = SparqlDefinition.cleanString(value)
  tag = SparqlDefinition.cleanString(tag)

  override def toString() : String = "\""+ value + "\""+ (datatype match {
    case URI.empty => ""
    case _ if (tag == "") => "^^"+datatype.toString()
    case _ => ""

  }) + ( tag match {
    case "" => ""
    case _ => "@"+tag
  })

  def toInt : Int = value.toInt

  def toDouble : Double = value.toDouble

  def toBoolean : Boolean = value.toBoolean

  def sparql : String = toString

  def naiveLabel : String = value
}

object QueryVariable {
implicit val rw: RW[QueryVariable] = macroRW
}

@JSExportTopLevel(name="QueryVariable")
case class QueryVariable (var name : String) extends SparqlDefinition {
  name = SparqlDefinition.cleanString(name)
  override def toString() : String = {
    "?"+name
  }
  def sparql : String = toString

  def naiveLabel : String = s"Variable[$name]"
}

object SparqlBuilder {

  def create(value: ujson.Value): SparqlDefinition = {
    (Try(value("type").value) match {
      case Success(v1) => v1
      case Failure(_) => throw new Error("Can not found key `type` in obj:"+value.toString())
      }) match {
      case "uri" => createUri(value)
      case "literal" | "typed-literal"=> createLiteral(value)
      case _ => throw new Error("unknown type ")
    }
  }

  def createUri(value: ujson.Value): URI = URI(value("value").value.toString)

  def createLiteral(value: ujson.Value): Literal = {
    val datatype = try { SparqlDefinition.cleanString(value("datatype").toString) match {
        case v if v.length<=0 => URI.empty
        case v => URI(v)
      }
    } catch {
      case _ : java.util.NoSuchElementException => URI.empty
    }

    val tag = try {
      SparqlDefinition.cleanString(value("tag").toString)
    } catch {
      case _ : java.util.NoSuchElementException => ""
    }

    Literal(value("value").toString, datatype,tag)
  }

}
