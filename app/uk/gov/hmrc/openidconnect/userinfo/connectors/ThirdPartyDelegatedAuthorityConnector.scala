/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.openidconnect.userinfo.connectors

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet, NotFoundException}
import uk.gov.hmrc.openidconnect.userinfo.config.AppContext

@Singleton
class ThirdPartyDelegatedAuthorityConnector @Inject() (appContext: AppContext, http: HttpGet) {
  val serviceUrl: String = appContext.thirdPartyDelegatedAuthorityUrl

  def fetchScopes(authBearerToken: String)(implicit hc: HeaderCarrier): Future[Set[String]] = {
    http.GET(s"$serviceUrl/delegated-authority", Seq("auth_bearer_token" -> authBearerToken)) map { response =>
      (response.json \ "token" \ "scopes").as[Set[String]]
    } recover {
      case e: NotFoundException => Set.empty
    }
  }
}
