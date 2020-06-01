package definition

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSClient
import play.api.test.Helpers.{OK, NOT_FOUND}
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}

class DefinitionControllerISpec extends PlaySpec with GuiceOneServerPerSuite with FutureAwaits with DefaultAwaitTimeout {

  def wsClient: WSClient = app.injector.instanceOf[WSClient]

  "api/definition" should {
    "return the correct definition from config" in {
      val response = await(wsClient.url(s"http://localhost:$port/api/definition").get())

      response.status mustBe OK
      response.body[JsValue] mustBe Json.parse(
        s"""
           |{
           |  "scopes": [
           |    {
           |      "key": "write:protect-connect",
           |      "name": "Protect Connect",
           |      "description": "Scope for accessing protect connect APIs"
           |    }
           |  ],
           |  "api": {
           |    "name": "Compliance Documents",
           |    "description": "Api to manage vat repayment documents sent to EF",
           |    "context": "misc/compliance-documents",
           |    "categories": ["PRIVATE_GOVERNMENT"],
           |    "versions": [
           |      {
           |        "version": "1.0",
           |        "status": "ALPHA",
           |        "endpointsEnabled": false,
           |        "access" : {
           |          "type": "PRIVATE",
           |          "whitelistedApplicationIds": ["ID-1"]
           |        }
           |      }
           |    ]
           |  }
           |}
      """.stripMargin)
    }
  }

  "api/conf/1.0/{file}" should {
    "return the correct file" in {
      val response = await(wsClient.url(s"http://localhost:$port/api/conf/1.0/docs/testing.md").get())


      response.status mustBe OK
      response.body mustBe
        """You can use the sandbox environment to [test this API](https://developer.service.hmrc.gov.uk/api-documentation/docs/testing).""".stripMargin

    }
    "return a 404 if given a nonexistent file to retrieve" in {
      val response = await(wsClient.url(s"http://localhost:$port/api/conf/1.0/docs/thisfiledoesnotexist.md").get())

      response.status mustBe NOT_FOUND
      response.body[JsValue] mustBe Json.parse(
        """
          |{
          |  "statusCode": 404,
          |  "message": "URI not found",
          |  "requested": "/api/conf/1.0/docs/thisfiledoesnotexist.md"
          |}
          |""".stripMargin)
    }
  }
}
