package inrae.semantic_web.internal.pm
import wvlet.log.Logger.rootLogger._

import inrae.semantic_web._
import inrae.semantic_web.internal.Node.references
import inrae.semantic_web.internal._
import inrae.semantic_web.rdf.{IRI, QueryVariable, SparqlDefinition}

/**
 * 
 */
object SparqlGenerator  {

    def prefixes(prefixes : Map[String,IRI]) : String = {
        prefixes.map {
            case (k,v) => "PREFIX "+k+": "+v.sparql()
        }.mkString("\n")
    }

    def queryFormSelect(listVariables : Seq[String] = Seq[String]()) : String = {
        if (listVariables.length == 0 ) {
            "SELECT *"
        } else {
            "SELECT DISTINCT" + listVariables.foldLeft(" ")( (acc,identifier) => acc+"?"+identifier+" ")
        }
    }

    def start_where() : String = "WHERE {"

    def from(graphs : Seq[IRI]): String = graphs.map( g => "FROM "+g.sparql()).mkString("\n")

    def fromNamed(graphs : Seq[IRI]): String = graphs.map( g => "FROM NAMED"+g.sparql()).mkString("\n")

    def solutionModifier () : String = {
        "} limit 13"
    }

    def prologCountSelection(varCount : String) : String = {
        "SELECT ( COUNT(*) as ?"+varCount+" )"
    }

    def solutionModifierSourcesSelection () : String = {
        "} LIMIT 1"
    }

    def queryVariableTransform(term : SparqlDefinition,
                               referenceToIdentifier : Map[String,String]) : SparqlDefinition = term match {
        case v : QueryVariable =>QueryVariable(
            { referenceToIdentifier.get(v.name) match {
                case Some(u) => u
                case None => throw new Error("Reference variable does not exist :"+v.name)
            }})
        case _ => term
    }

    def sparqlNode(n: Node,
                   referenceToIdentifier : Map[String,String],
                   varIdSire : String,
                   variableName : String) : String = {
        trace(varIdSire+" - "+variableName)
         n match {
            case node : SubjectOf          => "\t?" + varIdSire + " " +
              queryVariableTransform(node.term,referenceToIdentifier).toString() + " " + "?"+ variableName + " .\n"
            case node : ObjectOf           => "\t?" + variableName + " " +
              queryVariableTransform(node.term,referenceToIdentifier).toString() + " " + "?"+ varIdSire + " .\n"
            case node : LinkTo           => "\t?"+ varIdSire + " " + "?" + variableName + " " + queryVariableTransform(node.term,referenceToIdentifier).toString() + " .\n"
            case node : LinkFrom           => queryVariableTransform(node.term,referenceToIdentifier).toString() + " " + "?" + variableName + " " + "?"+ varIdSire + " .\n"
            case node : Value              => node.term match {
                case _ : QueryVariable => "BIND ( ?" + varIdSire +  " AS " + queryVariableTransform(node.term,referenceToIdentifier).toString() + ")"
                case _  =>  "VALUES ?" +varIdSire+ " { " + queryVariableTransform(node.term,referenceToIdentifier).toString() + " }.\n" }
            case node : ListValues         => "VALUES ?" +varIdSire+ " { " + node.terms.map(t => t.sparql()).mkString(" ") + " }.\n"
            case node : FilterNode         => "filter ( " + {
                node.negation match {
                    case true => "!"
                    case false => ""
                }
            } + {
                node match {
                    case n : Contains           => "contains(str(" + "?" +varIdSire + "), \""+ n.value + "\")"
                    case n : isBlank            => "isBlank(" + "?" +varIdSire + ")"
                    case n : isURI              => "isURI(" + "?" +varIdSire + ")"
                    case n : isLiteral          => "isLiteral(" + "?" +varIdSire + ")"
                    case _ => throw new Exception("SparqlGenerator::sparqlNode . [Devel error] Node undefined ["+n.toString()+"]")
                }
            } + " )\n"
            case _ : Root| _ : Something         => ""
            case _                               => throw new Error("Not implemented yet :"+n.getClass.getName)
        }
    }

    def generic_name(n:RdfNode) : String = {
        n match {
            case _: Something => "something"
            case _: SubjectOf => "object"
            case _: ObjectOf => "subject"
            case _: LinkTo => "linkto"
            case _: LinkFrom => "linkfrom"
            case _ => "unknown"
        }
    }

    /**
     *
     * @param n : Get variable from this Node
     * @param ms : Construction Map to build generic variable
     * @return Variable Name, Map [Generic name -> last index variable)
     */
    def getVariableIdentifier(
                       n:RdfNode,
                       ms : Map[String,Int] = Map[String,Int](), /* map to increase id and manage new variable name */
                     ) : Option[(String,Map[String,Int])] = {

        if ( ! n.reference().startsWith("_internal_")) {
            Some(n.reference(), ms)
        } else {
            val genericName = generic_name(n)
            genericName match {
                case s: String => {
                    val v = ms.getOrElse(s, 0)
                    Some(genericName + n.reference(), ms + (s -> (v + 1)))
                }
            }
        }
    }

    /**
     *
     * @param n : Get Variable Node
     * @param buildMap
     * @return
     *              Map[String,String] : Correspondence reference -> variableName
     *              Map[String,Int] : Iterator/index to increment new variable for a RdfNode
     */
    def correspondenceVariablesIdentifier(n:Node, buildMap : Map[String,Int]= Map[String,Int]())
                                       : (Map[String,String],Map[String,Int]) = {
        val resLoc : (Map[String,String],Map[String,Int]) = n match {
            case rdf : RdfNode => {
                val name = generic_name(rdf)
                val newBuildMap : Map[String,Int] = {
                    buildMap.get(name) match {
                        case Some(v: Int) => buildMap + (name -> (v + 1))
                        case None => buildMap + (name -> 0)
                    }
                }
                (Map(rdf.reference() -> (name + newBuildMap(name).toString)),newBuildMap)
                }
            case _ => (Map(),buildMap)
            }

        n.children.toArray.foldLeft(  resLoc ) {
            (acc, child) => {
                val r = correspondenceVariablesIdentifier(child,acc._2)
                (acc._1 ++ r._1,r._2)
            }
         }
    }

    def body(n: Node, /* current node to browse with children */
             referenceToIdentifier : Map[String,String],
             varIdSire : String = "" /* sire variable */
            )  : String = {
        val variableName : String = n match {
            case rdf : RdfNode => {
                referenceToIdentifier.get(rdf.reference()) match {
                    case Some(value) => value
                    case None => varIdSire
            } }
            case _ => varIdSire
        }

        trace(n.toString())
        trace("varIdSire:"+varIdSire)
        trace("variableName:"+variableName)
        val triplet : String = sparqlNode(n,referenceToIdentifier,varIdSire,variableName)
        triplet + n.children.map( child => body( child,referenceToIdentifier, variableName)).mkString("")
    } 
}
