import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.mappings.FieldType.{CompletionType, StringType}
import com.sksamuel.elastic4s.source.StringDocumentSource
import com.sksamuel.elastic4s.{ElasticClient, SimpleAnalyzer}
import org.scalatest.{FlatSpec, Matchers}

class PayloadSpec extends FlatSpec with Matchers {

  behavior of "Elasticsearch payloads"

  it should "elastic search reproduction case" in {
    val client = ElasticClient.remote("localhost", 9300)
    val indexName = "bands"

    client.execute { delete index indexName }.await

    val createRes = client.execute { create index indexName }.await
    createRes should be('acknowledged)

    client.execute {
      put mapping "bands" / "band" as Seq(
        field name "name" withType StringType,
        field name "suggest" withType CompletionType
          analyzer SimpleAnalyzer
          indexAnalyzer SimpleAnalyzer
          payloads true
      )
    }.await

    val insertJson = """
      |{
      |  "name" : "Queen",
      |  "suggest" : {
      |    "input" : ["Queen"],
      |    "output" : "Queen",
      |    "payload" : {
      |      "country" : "GB"
      |    }
      |  }
      |}
    """.stripMargin
    client.execute { index into indexName / "band" id "1" doc StringDocumentSource(insertJson) }.await

    val payloadUpdateJson = """
      |{
      |  "suggest" : {
      |    "payload" : {
      |      "country" : "new updated country"
      |    }
      |  }
      |}
    """.stripMargin
    client.execute { update id "1" in indexName / "band" doc StringDocumentSource(payloadUpdateJson) }.await

    Thread.sleep(1000) //wait for the indexing to finish
    val searchRes = client.execute { search in indexName / "band" }.await
    println("Search response:")
    println(searchRes)

    val resp = client.execute {
      search in indexName types "bands" suggestions(
        suggest.using(completion).as("suggest").text("Queen").from("suggest")
      )
    }.await
    println("")
    println("Suggest response:")
    println(resp)
  }
}
