/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.openidconnect.userinfo.config

import play.api.Play._
import uk.gov.hmrc.play.config.ServicesConfig

object AppContext extends ServicesConfig {
  lazy val appName = current.configuration.getString("appName").getOrElse(throw new RuntimeException("appName is not configured"))
  lazy val appUrl = current.configuration.getString("appUrl").getOrElse(throw new RuntimeException("appUrl is not configured"))
  lazy val serviceLocatorUrl: String = baseUrl("service-locator")
  lazy val registrationEnabled: Boolean = current.configuration.getBoolean(s"$env.microservice.services.service-locator.enabled").getOrElse(true)
  lazy val access = current.configuration.getConfig(s"$env.api.access")
  lazy val desEnvironment = current.configuration.getString(s"$env.microservice.services.des.environment").getOrElse(throw new RuntimeException(s"$env.microservice.services.des.environment is not configured"))
  lazy val desBearerToken =  current.configuration.getString(s"$env.microservice.services.des.bearer-token").getOrElse(throw new RuntimeException(s"$env.microservice.services.des.bearer-token is not configured"))
  lazy val logUserInfoResponsePayload = current.configuration.underlying.getBoolean("log-user-info-response-payload")
}
