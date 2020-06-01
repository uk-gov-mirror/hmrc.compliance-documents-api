/*
 * Copyright 2020 HM Revenue & Customs
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

package models.responses

import play.api.libs.json.{Json, Writes}

class OtherError(val code: String, val reason: String)

class FieldError(override val code: String, override val reason: String, val path: String) extends OtherError(code, reason)

case class MissingField(override val path: String)
  extends FieldError(code = "MISSING_FIELD", reason = "Expected field not present", path)

case class InvalidField(override val path: String)
  extends FieldError(code = "INVALID_FIELD", reason = "Invalid value in field", path)

case class UnexpectedField(override val path: String)
  extends FieldError(code = "UNEXPECTED_FIELD", reason = "Unexpected field found", path)

case class InvalidJsonType(override val path: String = "")
  extends FieldError(code = "INVALID_JSON_TYPE", reason = "Invalid Json type as payload", path)

case class InvalidDocId()
  extends OtherError(code = "INVALID_DOCUMENT_ID", reason = "Submission has not passed validation. Invalid path parameter DocumentId.")

case class MissingDocId()
  extends OtherError(code = "MISSING_DOCUMENT_ID", reason = "Submission has not passed validation. Missing path parameter DocumentId.")

case class InvalidPayload()
  extends OtherError(code = "INVALID_PAYLOAD", reason = "Submission has not passed validation. Invalid payload.")

case class InvalidCorrelationId()
  extends OtherError(code = "INVALID_CORRELATION_ID", reason = "Submission has not passed validation. Invalid header CorrelationId.")

case class MissingCorrelationId()
  extends OtherError(code = "MISSING_CORRELATION_ID", reason = "Submission has not passed validation. Missing header CorrelationId.")


object OtherError {

  implicit val otherErrorWrites: Writes[OtherError] = {
    case fieldError: FieldError =>
      Json.obj(
        "code" -> fieldError.code,
        "message" -> fieldError.reason,
        "path" -> fieldError.path
      )

    case error =>
      Json.obj(
        "code" -> error.code,
        "message" -> error.reason
      )
  }

}
