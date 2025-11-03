/*
 * Copyright 2024 HM Revenue & Customs
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

package services

import com.github.fge.jackson.JsonLoader
import com.github.fge.jsonschema.core.report.{ListReportProvider, LogLevel, ProcessingMessage, ProcessingReport}
import com.github.fge.jsonschema.main.JsonSchemaFactory
import com.google.inject.Inject
import models.responses._
import play.api.libs.json.{Json, _}
import scala.annotation.nowarn
import scala.jdk.CollectionConverters._

class ValidationService @Inject()(resources: ResourceService) {

  private lazy val efSchema = resources.getFile("/schemas/efSchema.json")
  private lazy val nRegSchema = resources.getFile("/schemas/nRegSchema.json")
  private lazy val pRegSchema = resources.getFile("/schemas/pRegSchema.json")
  private lazy val addDocumentSchemaNoClassType = resources.getFile("/schemas/addDocumentSchemaNoClassType.json")


  private val factory = JsonSchemaFactory
    .newBuilder()
    .setReportProvider(new ListReportProvider(LogLevel.ERROR, LogLevel.FATAL))
    .freeze()


  private def validateInternallyAgainstSchema(schemaString: String, input: JsValue) = {
    val schemaJson = JsonLoader.fromString(schemaString)
    val json = JsonLoader.fromString(Json.stringify(input))
    val schema = factory.getJsonSchema(schemaJson)
    schema.validate(json, true)
  }

  def getFieldName(processingMessage: ProcessingMessage, prefix: String = ""): String = {
    processingMessage.asJson().get("instance").asScala.map(instanceName => prefix + instanceName.asText).headOption.getOrElse("Field cannot be found")
  }

  def getMissingFields(processingMessage: ProcessingMessage, prefix: String = ""): List[MissingField] = {
    Option(processingMessage.asJson().get("missing")).map(_.asScala.map(
      instanceName => MissingField(path = s"${getFieldName(processingMessage, prefix)}/${instanceName.asText()}")
    ).toList).getOrElse(List())
  }

  def getUnexpectedFields(processingMessage: ProcessingMessage, prefix: String = ""): List[UnexpectedField] = {
    Option(processingMessage.asJson().get("unwanted")).map(_.asScala.map(
      instanceName => UnexpectedField(path = s"${getFieldName(processingMessage, prefix)}/${instanceName.asText()}")
    ).toList).getOrElse(List())
  }

  def getFieldErrorsFromReport(report: ProcessingReport, prefix: String = ""): Seq[FieldError] = {
    report.iterator.asScala.toList
      .flatMap {
        error =>
          val missingFields = getMissingFields(error, prefix)
          val unexpectedFields = getUnexpectedFields(error, prefix)
          if (missingFields.isEmpty && unexpectedFields.isEmpty) {
            List(
              InvalidField(getFieldName(error, prefix))
            )
          } else {
            if (missingFields.isEmpty) unexpectedFields else missingFields
          }
      }
  }

  @nowarn("msg=match may not be exhaustive")
  def validateDocType(docJson: JsValue): Either[BadRequestErrorResponse, Unit] = {
    def getResult(schema: String): Either[BadRequestErrorResponse, Unit] = {
      val result = validateInternallyAgainstSchema(schema, (docJson \ "documentMetadata" \ "classIndex").as[JsValue])
      if (!result.isSuccess) {
        Left(BadRequestErrorResponse(getFieldErrorsFromReport(result, "/documentMetadata/classIndex"), getClassDoc(docJson)))
      } else {
        Right(())
      }
    }

    (docJson \ "documentMetadata" \ "classIndex").validate[JsObject] match {
      case JsSuccess(x, _) if x.keys("ef") => getResult(efSchema)
      case JsSuccess(x, _) if x.keys("pReg") => getResult(pRegSchema)
      case JsSuccess(x, _) if x.keys("nReg") => getResult(nRegSchema)
      case JsError(errors) =>
        Left(mappingErrorResponse(errors.toSeq.map {
          case (_, errors) =>
            (__ \ "documentMetadata" \ "classIndex", errors.toSeq)
        }, getClassDoc(docJson)))
    }
  }

  private def mappingErrorResponse(mappingErrors: Seq[(JsPath, Seq[JsonValidationError])], typeOfDoc: Option[String]): BadRequestErrorResponse = {
    val errors = mapErrors(mappingErrors)
    BadRequestErrorResponse(errors, typeOfDoc)
  }

  private def mapErrors(mappingErrors: Seq[(JsPath, Seq[JsonValidationError])]) = {
    mappingErrors.map {
      x => InvalidField(path = x._1.toString())
    }
  }

  def validateJsonObj(input: JsValue): Option[JsValue] = {
    input.asOpt[JsObject]

  }


  def validate(input: JsValue, docId: String = ""): Option[JsValue] = {
    if (checkDocIdMatchesRegex(docId)) {
      if (validateJsonObj(input).isDefined) {
        val result = validateInternallyAgainstSchema(addDocumentSchemaNoClassType, input)
        if (result.isSuccess) {
          validateDocType(input)
            .fold(invalid => Some(Json.toJson(invalid)), _ => None)
        } else {
          Some(
            Json.toJson[BadRequestErrorResponse](BadRequestErrorResponse(getFieldErrorsFromReport(result), None))
          )
        }
      }
      else {
        Some(Json.toJson[OtherError](InvalidJsonType()))
      }

    } else {
      Some(
        Json.toJson[OtherError] (InvalidDocId())
      )
    }
  }

  def getClassDoc(toFindIn: JsValue): Option[String] = {
    List("ef", "nReg", "pReg").collectFirst {
      case el if (toFindIn \ "documentMetadata" \ "classIndex").asOpt[JsObject]
        .fold(ifEmpty = false)(classIndex => classIndex.keys(el)) => el

    }
  }

  def checkDocIdMatchesRegex(docId: String): Boolean = {
    docId.matches("^(([0-9]{1,19})|(1[0-7][0-9]{18})|(18[0-3][0-9]{17})|(184[0-3][0-9]{16}))$")
  }
}
