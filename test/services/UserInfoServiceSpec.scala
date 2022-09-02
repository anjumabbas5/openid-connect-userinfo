/*
 * Copyright 2022 HM Revenue & Customs
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

import connectors.{AuthConnector, AuthConnectorV1, ThirdPartyDelegatedAuthorityConnector}
import controllers.{Version_1_0, Version_1_1}
import data.UserInfoGenerator
import domain._
import org.mockito.BDDMockito.given
import org.scalatest.concurrent.ScalaFutures
import org.mockito.scalatest.MockitoSugar
import testSupport.UnitSpec
import uk.gov.hmrc.auth.core.retrieve.{ItmpAddress, ItmpName}
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.Authorization
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class UserInfoServiceSpec extends UnitSpec with MockitoSugar with ScalaFutures {

  val nino = Nino("AB123456A")
  val authBearerToken = "AUTH_BEARER_TOKEN"
  val desUserInfo = DesUserInfo(ItmpName(Some("John"), None, Some("Smith")), None, ItmpAddress(Some("1 Station Road"), Some("Town Centre"), None, None, None, None, None, None))
  val enrolments = Enrolments(Set(Enrolment("IR-SA", List(EnrolmentIdentifier("UTR", "174371121")), "Activated")))
  val authority: Authority = Authority("32131", Some("AB123456A"))

  val userDetails: UserDetails = UserDetails(None, None, None, None, None, None, None, None, Some("affinityGroup"), None, None,
                                             Some("User"), None, None, None, None, None, None)

  val governmentGateway: GovernmentGatewayDetails = GovernmentGatewayDetails(Some("32131"), Some(Seq("User")), Some("John"),
                                                                             Some("affinityGroup"), Some("agent-code-12345"), Some("agent-id-12345"), Some("agent-friendly-name"), Some("gateway-token-val"), Some(11), None, None)
  val mdtp = Mdtp("device-id-12", "session-id-133")

  val userInfo = UserInfo(Some("John"), Some("Smith"), None, Some(Address("1 Station Road\nTown Centre", None, None, None)),
                          None, None, Some(nino).map(_.nino), Some(enrolments.enrolments), Some(governmentGateway), Some(mdtp))

  trait Setup {
    implicit val headers = HeaderCarrier().copy(authorization = Some(Authorization(s"Bearer $authBearerToken")))

    val mockAuthConnector: AuthConnector = mock[AuthConnectorV1]
    val mockUserInfoGenerator: UserInfoGenerator = mock[UserInfoGenerator]
    val mockUserInfoTransformer = mock[UserInfoTransformer]
    val mockThirdPartyDelegatedAuthorityConnector = mock[ThirdPartyDelegatedAuthorityConnector]

    val sandboxInfoService = new SandboxUserInfoService(mockUserInfoGenerator)
    val liveInfoService = new LiveUserInfoService(mockAuthConnector, mockAuthConnector, mockUserInfoTransformer, mockThirdPartyDelegatedAuthorityConnector)
  }

  "LiveUserInfoService" should {

    "requests all available data" in new Setup {

      val scopes = Set("openid", "address", "profile", "openid:gov-uk-identifiers", "openid:hmrc-enrolments", "email", "openid:government-gateway")
      given(mockThirdPartyDelegatedAuthorityConnector.fetchScopes(authBearerToken)(headers, implicitly)).willReturn(scopes)
      given(mockAuthConnector.fetchAuthority()(headers, implicitly)).willReturn(Some(authority))
      given(mockAuthConnector.fetchEnrolments()(headers, implicitly)).willReturn(Some(enrolments))
      given(mockAuthConnector.fetchDesUserInfo()(headers, implicitly)).willReturn(Some(desUserInfo))
      when(mockAuthConnector.fetchUserDetails()(eqTo(headers), any[ExecutionContext])).thenReturn(Future.successful(Some(userDetails)))
      given(mockUserInfoTransformer.transform(scopes, Some(authority), Some(desUserInfo), Some(enrolments), Some(userDetails))).willReturn(any[UserInfo], any[UserInfo])

      await(liveInfoService.fetchUserInfo(Version_1_0))

      verify(mockAuthConnector).fetchDesUserInfo()
      verify(mockAuthConnector).fetchEnrolments()
      verify(mockAuthConnector).fetchAuthority()
      verify(mockAuthConnector).fetchUserDetails()(any[HeaderCarrier], any[ExecutionContext])
    }

    "should fail with BadRequestException when the NINO is not in the authority and a scope that requires a NINO is requested" in new Setup {
      val scopes = Set("address", "profile", "openid:gov-uk-identifiers", "openid:hmrc-enrolments")
      given(mockThirdPartyDelegatedAuthorityConnector.fetchScopes(authBearerToken)(headers, implicitly)).willReturn(scopes)
      given(mockAuthConnector.fetchAuthority()(headers, implicitly)).willReturn(Future(Some(authority.copy(nino = None))))
      given(mockAuthConnector.fetchEnrolments()(headers, implicitly)).willReturn(Future(None))
      when(mockAuthConnector.fetchUserDetails()(eqTo(headers), any[ExecutionContext])).thenReturn(Future.successful(Some(userDetails)))

      a[BadRequestException] should be thrownBy await(liveInfoService.fetchUserInfo(Version_1_0))
    }

    "does not request DES::fetchUserInfo when the scopes does not contain 'address' nor 'profile'" in new Setup {

      val scopes = Set("openid:gov-uk-identifiers", "openid:hmrc-enrolments")
      given(mockThirdPartyDelegatedAuthorityConnector.fetchScopes(authBearerToken)(headers, implicitly)).willReturn(scopes)
      given(mockAuthConnector.fetchAuthority()(headers, implicitly)).willReturn(Some(authority))
      given(mockAuthConnector.fetchEnrolments()(headers, implicitly)).willReturn(Future(Some(enrolments)))
      given(mockUserInfoTransformer.transform(scopes, Some(authority), None, Some(enrolments), None)).willReturn(any[UserInfo], any[UserInfo])
      given(mockAuthConnector.fetchUserDetails()(headers, implicitly)).willReturn(Future.successful(Some(userDetails)))

      await(liveInfoService.fetchUserInfo(Version_1_0))

      verify(mockAuthConnector, never).fetchDesUserInfo()(any[HeaderCarrier], any[ExecutionContext])
      verify(mockAuthConnector).fetchEnrolments()
    }

    "does not request AUTH::fetchNino nor DES::fetchUserInfo when the scopes does not contain 'address' nor 'profile' nor 'openid:gov-uk-identifiers'" in new Setup {

      val scopes = Set("openid:hmrc-enrolments")
      given(mockThirdPartyDelegatedAuthorityConnector.fetchScopes(authBearerToken)(headers, implicitly)).willReturn(scopes)

      given(mockAuthConnector.fetchAuthority()(headers, implicitly)).willReturn(Some(authority))
      given(mockAuthConnector.fetchEnrolments()(headers, implicitly)).willReturn(Some(enrolments))
      given(mockUserInfoTransformer.transform(scopes, Some(authority), None, Some(enrolments), None)).willReturn(any[UserInfo], any[UserInfo])
      given(mockAuthConnector.fetchUserDetails()(headers, implicitly)).willReturn(Future.successful(Some(userDetails)))

      await(liveInfoService.fetchUserInfo(Version_1_0))

      verify(mockAuthConnector, never).fetchDesUserInfo()(any[HeaderCarrier], any[ExecutionContext])
      verify(mockAuthConnector).fetchEnrolments()
    }

    "does not request AUTH::fetchEnrolments when the scopes does not contain 'openid:hmrc-enrolments'" in new Setup {

      val scopes = Set("address", "profile", "openid:gov-uk-identifiers")
      given(mockThirdPartyDelegatedAuthorityConnector.fetchScopes(authBearerToken)(headers, implicitly)).willReturn(scopes)

      given(mockAuthConnector.fetchAuthority()(headers, implicitly)).willReturn(Some(authority))
      given(mockAuthConnector.fetchDesUserInfo()(headers, implicitly)).willReturn(None)
      given(mockUserInfoTransformer.transform(scopes, Some(authority), None, None, None)).willReturn(any[UserInfo], any[UserInfo])
      given(mockAuthConnector.fetchUserDetails()(headers, implicitly)).willReturn(Future.successful(Some(userDetails)))

      await(liveInfoService.fetchUserInfo(Version_1_0))

      verify(mockAuthConnector, never).fetchEnrolments()(any[HeaderCarrier], any[ExecutionContext])
      verify(mockAuthConnector).fetchDesUserInfo()(any[HeaderCarrier], any[ExecutionContext])
    }
  }

  "SandboxUserInfoService" should {
    "return generated UserInfo v1.0" in new Setup {
      given(mockUserInfoGenerator.userInfoV1_0).willReturn(userInfo)

      val result: UserInfo = await(sandboxInfoService.fetchUserInfo(Version_1_0))

      result shouldBe userInfo
    }

    "return generated UserInfo v1.1" in new Setup {
      given(mockUserInfoGenerator.userInfoV1_1).willReturn(userInfo)

      val result = await(sandboxInfoService.fetchUserInfo(Version_1_1))

      result shouldBe userInfo
    }
  }
}
