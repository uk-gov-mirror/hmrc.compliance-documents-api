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

package controllers

import config.ErrorInternalServerError
import connectors.ComplianceDocumentsConnector
import controllers.actions.{AuthenticateApplicationAction, ValidateCorrelationIdHeaderAction}
import javax.inject._
import play.api.Logger
import play.api.http.ContentTypes
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import services.ValidationService
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.LoggerHelper
import utils.LoggerHelper._

import scala.concurrent.{ExecutionContext, Future}

class VatRepaymentApiController @Inject()(
                                           validator: ValidationService,
                                           complianceDocumentsConnector: ComplianceDocumentsConnector,
                                           getCorrelationId: ValidateCorrelationIdHeaderAction,
                                           authenticateApplication: AuthenticateApplicationAction,
                                           cc: ControllerComponents
                                         )(implicit ec: ExecutionContext) extends BackendController(cc) {
  private val logger: Logger = Logger(this.getClass)

  def postRepaymentData(documentId: String): Action[AnyContent] = (authenticateApplication andThen getCorrelationId).async { implicit request =>

    val input = request.body.asJson.getOrElse(Json.parse("{}"))
    logger.info(logProcess("VatRepaymentApiController",
      "postRepaymentData",
      s"Post request received",
      Some(request.correlationId),
      Some(input)))
    validator.validate(input, documentId) match {
      case None =>
        logger.info(logProcess("VatRepaymentApiController", "postRepaymentData: Right",
          s"Request received - passing on to IF", Some(request.correlationId), Some(input)))
        complianceDocumentsConnector.vatRepayment(input, request.correlationId, documentId.toLong).map {
          el =>
            el.map(response => responseMapper(response)) getOrElse InternalServerError(Json.toJson(ErrorInternalServerError()))
        }
      case Some(errors) =>
        logger.warn(LoggerHelper.logProcess("VatRepaymentApiController", "postRepaymentData: Left",
          s"request body didn't match json with errors: ${Json.prettyPrint(errors)}",
          Some(request.correlationId), Some(input)))
        Future.successful(BadRequest(errors))
    }
  }

  private def responseMapper(response: HttpResponse): Result = {
    Status(response.status)

      .as(ContentTypes.JSON)
  }


}
