/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package scala.services

import models.responses.{BadRequestErrorResponse, InvalidField}
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import services.{ResourceService, ValidationService}
import uk.gov.hmrc.http.HeaderCarrier

import scala.exampleData.VatDocumentExample._

class ValidationServiceSpec extends AnyWordSpec with Matchers with MockFactory {

  implicit lazy val hc: HeaderCarrier = HeaderCarrier(sessionId = None)

  val mockResource = mock[ResourceService]

  def validationService = new ValidationService(mockResource)

  "The validation service" should {
    "return errors when model does not map properly" in {
      (mockResource.getFile).expects("/schemas/addDocumentSchemaNoClassType.json").returns(
        """{
          |  "$schema": "http://json-schema.org/draft-04/schema#",
          |  "title": "Mapping tester",
          |  "type": "array",
          |  "items": {
          |    "title": "JustAThing",
          |    "type": "object",
          |    "properties": {
          |      "thisIsWrong": {
          |         "type": "string"
          |      }
          |    }
          |  }
          |}
          |""".stripMargin)
      validationService.validate(Json.parse(getExample("justInvalid")), "1234").get shouldBe Json.parse(
        """
{"code":"INVALID_PAYLOAD","message":"Submission has not passed validation. Invalid payload.","errors":[{"code":"INVALID_FIELD","message":"Invalid value in field","path":""}]}
""".stripMargin
      )
    }

    "return INVALID_DOCUMENT_ID if given wrong document id" in {
      validationService.validate(Json.parse(getExample("pReg")), "1234a").get shouldBe Json.parse(
        """
          |{"code":"INVALID_DOCUMENT_ID","message":"Submission has not passed validation. Invalid path parameter DocumentId."}
          |""".stripMargin
      )
    }
    "return nothing if given valid input - EF" in {
      (mockResource.getFile).expects("/schemas/addDocumentSchemaNoClassType.json").returns(schema).once()
      (mockResource.getFile).expects("/schemas/efSchema.json").returns(efSchema).once()
      assert(validationService.validate(Json.parse(getExample("ef")), "1234").isEmpty)
    }
    "return nothing if given valid input - nReg" in {
      (mockResource.getFile).expects("/schemas/addDocumentSchemaNoClassType.json").returns(schema).once()
      (mockResource.getFile).expects("/schemas/nRegSchema.json").returns(nRegSchema).once()
      assert(validationService.validate(Json.parse(getExample("nReg")), "1234").isEmpty)
    }
    "return nothing if given valid input - pReg" in {
      (mockResource.getFile).expects("/schemas/addDocumentSchemaNoClassType.json").returns(schema).once()
      (mockResource.getFile).expects("/schemas/pRegSchema.json").returns(pRegSchema).once()
      assert(validationService.validate(Json.parse(getExample("pReg")), "1234").isEmpty)
    }
    "return bad request if given invalid input for a valid classIndex" in {
      (mockResource.getFile).expects("/schemas/addDocumentSchemaNoClassType.json").returns(schema).once()
      (mockResource.getFile).expects("/schemas/efSchema.json").returns(efSchema).once()
      val resultOfBadOne = validationService.validate(Json.parse(getExample("efInvalid")), "1234")
      assert(resultOfBadOne.isDefined)
      resultOfBadOne.get shouldBe Json.parse(
        """
          |{"code":"INVALID_PAYLOAD","message":"Submission has not passed validation for the ef model. Invalid payload.","errors":[{"code":"MISSING_FIELD","message":"Expected field not present","path":"/documentMetadata/classIndex/ef/dTRN"},{"code":"INVALID_FIELD","message":"Invalid value in field","path":"/documentMetadata/classIndex/ef/locationCode"}]}
          |""".stripMargin
      )
    }
    "return an invalidJsonType if given wrong Json" in {
      val resultOfBadOne = validationService.validate(Json.parse("""["test"]"""), "1234")
      assert(resultOfBadOne.isDefined)
      resultOfBadOne.get shouldBe Json.parse(
        """
          |{"code":"INVALID_JSON_TYPE","message":"Invalid Json type as payload","path":""}
          |""".stripMargin
      )
    }
    "return bad request if given invalid classIndex" in {
      (mockResource.getFile).expects("/schemas/addDocumentSchemaNoClassType.json").returns(schema).once()
      val resultOfBadOne = validationService.validate(Json.parse(minWithEmptySpace(badDocument)), "1234")
      assert(resultOfBadOne.isDefined)
      resultOfBadOne.get shouldBe Json.parse(
        """
          |{"code":"INVALID_PAYLOAD","message":"Submission has not passed validation. Invalid payload.","errors":[{"code":"INVALID_FIELD","message":"Invalid value in field","path":"/documentMetadata/classIndex"}]}
          |""".stripMargin
      )

    }
    "return bad request if given invalid input with fields not matching Regex" in {
      (mockResource.getFile).expects("/schemas/addDocumentSchemaNoClassType.json").returns(schema).once()
      val resultOfBadOne = validationService.validate(Json.parse(getExample("invalidNoMissing")), "1234")
      assert(resultOfBadOne.isDefined)
      resultOfBadOne.get shouldBe Json.parse(
        """
          |{"code":"INVALID_PAYLOAD","message":"Submission has not passed validation. Invalid payload.","errors":[{"code":"INVALID_FIELD","message":"Invalid value in field","path":"/documentMetadata/allocateToUser"},{"code":"INVALID_FIELD","message":"Invalid value in field","path":"/documentMetadata/docBinaryType"}]}
          |""".stripMargin)
    }

    "return bad request if given invalid input with both missing & unexpected fields" in {
      (mockResource.getFile).expects("/schemas/addDocumentSchemaNoClassType.json").returns(schema).once()
      val resultOfBadOne = validationService.validate(Json.parse(getExample("unexpectedAndMissing")), "1234")
      resultOfBadOne.get shouldBe Json.parse(
        """
{"code":"INVALID_PAYLOAD","message":"Submission has not passed validation. Invalid payload.","errors":[{"code":"MISSING_FIELD","message":"Expected field not present","path":"/documentBinary"},{"code":"UNEXPECTED_FIELD","message":"Unexpected field found","path":"/documentMetadata/wrong"}]}
""".stripMargin
      )
    }
    "return a bad request if given a good document with invalid classdoc parameters" in {
      (mockResource.getFile).expects("/schemas/addDocumentSchemaNoClassType.json").returns(schema).once()
      (mockResource.getFile).expects("/schemas/efSchema.json").returns(efSchema).once()
      val resultOfBadOne = validationService.validate(Json.parse(minWithEmptySpace(efInvalid)), "1234")
      resultOfBadOne.get shouldBe Json.parse(
        """
          |{"code":"INVALID_PAYLOAD","message":"Submission has not passed validation for the ef model. Invalid payload.","errors":[{"code":"MISSING_FIELD","message":"Expected field not present","path":"/documentMetadata/classIndex/ef/dTRN"},{"code":"INVALID_FIELD","message":"Invalid value in field","path":"/documentMetadata/classIndex/ef/locationCode"}]}
          |""".stripMargin
      )
    }
  }
  "The document validation service" should {
    "return bad request if given document with a class doc that's not Json" in {
      val resultOfBadOne = validationService.validateDocType(Json.parse(notTrueJsonClassDoc))
      assert(resultOfBadOne.isLeft)
      resultOfBadOne.swap.toOption.get shouldBe BadRequestErrorResponse(
        List(InvalidField("/documentMetadata/classIndex")),
        None
      )
    }
  }
}
