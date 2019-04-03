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

package it

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import it.stubs.{AuthStub, ThirdPartyDelegatedAuthorityStub}
import org.scalatest._
import org.scalatestplus.play.OneServerPerTest

import scala.concurrent.duration._

abstract class BaseFeatureSpec extends FeatureSpec with GivenWhenThen with Matchers
with BeforeAndAfterEach with BeforeAndAfterAll with OneServerPerTest {

  override lazy val port = 19111
  val serviceUrl = s"http://localhost:$port"
  val timeout = 10.second

  val authStub = AuthStub
  val thirdPartyDelegatedAuthorityStub = ThirdPartyDelegatedAuthorityStub

  val mocks = Seq(authStub, thirdPartyDelegatedAuthorityStub)

  override protected def beforeEach(): Unit = {
    mocks.foreach(m => if (!m.stub.server.isRunning) m.stub.server.start()
    )
  }

  override protected def afterEach(): Unit = {
    mocks.foreach(_.stub.mock.resetMappings())
  }

  override protected def afterAll(): Unit = {
    mocks.foreach(_.stub.server.stop())
  }
}

case class MockHost(port: Int) {
  val server = new WireMockServer(WireMockConfiguration.wireMockConfig().port(port))
  val mock = new WireMock("localhost", port)
}

trait Stub {
  val stub: MockHost
}
