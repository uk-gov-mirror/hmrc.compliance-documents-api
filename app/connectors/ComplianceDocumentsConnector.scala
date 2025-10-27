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

package connectors

import connectors.httpParsers.ComplianceDocumentsConnectorParser
import play.api.http.Status._
import javax.inject._
import play.api.http.{ContentTypes, HeaderNames}
import play.api.libs.json.JsValue
import play.api.{Configuration, Logger}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import utils.LoggerHelper
import uk.gov.hmrc.http.HttpReads.Implicits._
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue

import scala.concurrent.{ExecutionContext, Future}

class ComplianceDocumentsConnector @Inject()(
  httpClient: HttpClientV2,
  config: Configuration
) extends ComplianceDocumentsConnectorParser {


  override val className: String = this.getClass.getSimpleName
  override val logger: Logger = Logger(this.getClass)

  lazy val bearerToken: String = config.get[String]("integration-framework.auth-token")
  lazy val iFEnvironment: String = config.get[String]("integration-framework.environment")
  lazy val ifBaseUrl: String = config.get[String]("integration-framework.base-url")
  lazy val vatRepaymentUri: String = config.get[String]("integration-framework.endpoints.vat-repayment-info")

  def vatRepayment(request: JsValue, correlationId: String, documentId: String)
                  (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Option[HttpResponse]] = {
    val url = s"$ifBaseUrl$vatRepaymentUri/$documentId"
    val customHeaders = Seq(
      HeaderNames.CONTENT_TYPE -> ContentTypes.JSON,
      "CorrelationId" -> correlationId,
      "Environment" -> iFEnvironment,
      "Authorization" -> s"Bearer $bearerToken"
    )

    httpClient.post(url"$url")
      .withBody(request)
      .setHeader(customHeaders*)
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case ACCEPTED => Some(response)
          case BAD_REQUEST | UNAUTHORIZED => None
          case _ => None
        }
      }
      .recover {
        case e: Exception =>
          logger.error(LoggerHelper.logProcess("ComplianceDocumentsConnector", "vatRepayment",
            s"Exception from when trying to talk to $ifBaseUrl$vatRepaymentUri - ${e.getMessage} " +
              s"(IF_VAT_REPAYMENT_ENDPOINT_UNEXPECTED_EXCEPTION)" + e, Some(correlationId), Some(request)))
          None
      }
  }
}
