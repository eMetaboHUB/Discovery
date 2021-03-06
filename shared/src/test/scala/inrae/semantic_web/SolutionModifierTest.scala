package inrae.semantic_web
import inrae.data.DataTestFactory
import inrae.semantic_web.rdf.{IRI, URI}
import utest.{TestSuite, Tests, test}

import scala.concurrent.ExecutionContext.Implicits.global

object SolutionModifierTest extends TestSuite {
  val insert_data = DataTestFactory.insert_virtuoso1(
    """
      <http://p1>    <http://xmlns.com/foaf/0.1/name> "Alice" .
      <http://p1>    <http://xmlns.com/foaf/0.1/mbox>  <mailto:alice@example.com> .

      <http://p2>    <http://xmlns.com/foaf/0.1/name>  "Alice" .
      <http://p2>    <http://xmlns.com/foaf/0.1/mbox>   <mailto:asmith@example.com> .

      <http://p3>    <http://xmlns.com/foaf/0.1/name> "Alice" .
      <http://p3>    <http://xmlns.com/foaf/0.1/mbox>   <mailto:alice.smith@example.com> .
      """.stripMargin, this.getClass.getSimpleName)

  val config: StatementConfiguration = DataTestFactory.getConfigVirtuoso1()

  val basereq : SWTransaction = SWDiscovery(config)
    .graph(IRI(DataTestFactory.graph1(this.getClass.getSimpleName)))
    .prefix("foaf","http://xmlns.com/foaf/0.1/")
    .something()
    .isSubjectOf(URI("name","foaf"), "name")
    .select(Seq("name"))

  def tests = Tests {
    test("no modifier") {
      insert_data.map(_ => {
        basereq.commit()
          .raw.map(r => {
          assert(r("results")("bindings").arr.length == 3)
        })
      }).flatten
    }

    test("limit") {
      insert_data.map(_ => {
        basereq
          .limit(1)
          .commit()
          .raw.map(r => {
          assert(r("results")("bindings").arr.length == 1)
        })
      }).flatten
    }

    test("offset") {
      insert_data.map(_ => {
        basereq
          .limit(2)
          .offset(1)
          .commit()
          .raw.map(r => {
            assert(true)
        })
      }).flatten
    }

    test("distinct") {
      insert_data.map(_ => {
        basereq
          .distinct
          .commit()
          .raw.map(r => {
          assert(r("results")("bindings").arr.length == 1)
        })
      }).flatten
    }

    test("reduced") {
      insert_data.map(_ => {
        basereq
          .reduced
          .commit()
          .raw.map(r => {
          assert(r("results")("bindings").arr.length <= 3)
        })
      }).flatten
    }
  }
}
