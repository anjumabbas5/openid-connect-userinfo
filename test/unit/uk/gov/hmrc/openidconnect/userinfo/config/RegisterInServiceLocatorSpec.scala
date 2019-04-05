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

package unit.uk.gov.hmrc.openidconnect.userinfo.config

import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.api.connector.ServiceLocatorConnector
import uk.gov.hmrc.openidconnect.userinfo.config.{AppContext, ServiceLocatorRegistration}
import uk.gov.hmrc.play.test.UnitSpec
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class RegisterInServiceLocatorSpec extends UnitSpec with MockitoSugar {

  trait Setup  {
    val mockConnector = mock[ServiceLocatorConnector]
    val mockAppContext = mock[AppContext]
    implicit val hc = HeaderCarrier()
  }

  "onStart" should {
    "register the microservice in service locator when registration is enabled" in new Setup {
      when(mockAppContext.registrationEnabled).thenReturn(true)
      when(mockConnector.register(any())).thenReturn(Future.successful(true))
      val registration = new ServiceLocatorRegistration(mockAppContext, mockConnector)
      verify(mockConnector).register(any())
    }

    "not register the microservice in service locator when registration is disabled" in new Setup {
      when(mockAppContext.registrationEnabled).thenReturn(false)
      val registration = new ServiceLocatorRegistration(mockAppContext, mockConnector)
      verify(mockConnector, never()).register(any())
    }
  }
}
