package scalaoauth2.provider

import org.scalatest._
import org.scalatest.Matchers._
import java.util.Date

class ProtectedResourceSpec extends FlatSpec {

  def successfulDataHandler() = new MockDataHandler() {

    override def findAccessToken(token: String): Option[AccessToken] = Some(AccessToken("token1", Some("refreshToken1"), Some("all"), Some(3600), new Date()))

    override def findAuthInfoByAccessToken(accessToken: AccessToken): Option[AuthInfo[MockUser]] = Some(
      AuthInfo(user = MockUser(10000, "username"), clientId = "clientId1", scope = Some("all"), redirectUri = None)
    )

  }

  it should "be handled request with token into header" in {
    val request = ProtectedResourceRequest(
      Map("Authorization" -> Seq("OAuth token1")),
      Map("username" -> Seq("user"), "password" -> Seq("pass"), "scope" -> Seq("all"))
    )

    val dataHandler = successfulDataHandler()
    ProtectedResource.handleRequest(request, dataHandler) should be ('right)
  }

  it should "be handled request with token into body" in {
    val request = ProtectedResourceRequest(
      Map(),
      Map("access_token" -> Seq("token1"), "username" -> Seq("user"), "password" -> Seq("pass"), "scope" -> Seq("all"))
    )

    val dataHandler = successfulDataHandler()
    ProtectedResource.handleRequest(request, dataHandler) should be ('right)
  }

  it should "be lost expired" in {
    val request = ProtectedResourceRequest(
      Map("Authorization" -> Seq("OAuth token1")),
      Map("username" -> Seq("user"), "password" -> Seq("pass"), "scope" -> Seq("all"))
    )

    val dataHandler = new MockDataHandler() {

      override def findAccessToken(token: String): Option[AccessToken] = Some(AccessToken("token1", Some("refreshToken1"), Some("all"), Some(3600), new Date(new Date().getTime() - 4000 * 1000)))

      override def findAuthInfoByAccessToken(accessToken: AccessToken): Option[AuthInfo[MockUser]] = Some(
        AuthInfo(user = MockUser(10000, "username"), clientId = "clientId1", scope = Some("all"), redirectUri = None)
      )

    }

    intercept[ExpiredToken] {
      ProtectedResource.handleRequest(request, dataHandler) match {
        case Left(e) => throw e
        case _ =>
      }
    }
  }

  it should "be invalid request without token" in {
    val request = ProtectedResourceRequest(
      Map(),
      Map("username" -> Seq("user"), "password" -> Seq("pass"), "scope" -> Seq("all"))
    )

    val dataHandler = successfulDataHandler()
    intercept[InvalidRequest] {
      ProtectedResource.handleRequest(request, dataHandler) match {
        case Left(e) => throw e
        case _ =>
      }
    }
  }
}
