package org.pac4j.http4s
package example

import cats.effect._
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.server.HttpMiddleware
import scalatags.Text.all._
import scalatags.Text
import org.pac4j.core.profile.{CommonProfile, ProfileManager}

case class Routes(
  getProfiles: Request[IO] => List[CommonProfile],
  callbackService: CallbackService,
  localLogoutService: LogoutService,
  centralLogoutService: LogoutService,
  protectedPagesFilter: Option[Main.AuthClient] => HttpMiddleware[IO]
) {
  import ScalatagsInstances._

  val root: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req @ GET -> Root =>
      Ok(
        html(
          body(
            h1("index"),
            a(href := "/login/facebook")(
              "Protected url by Facebook: /facebook"
            ),
            "use a real account",
            br(),
            a(href := "/login/saml2")(
              "Protected url by SAML2: /saml2"
            ),
            "use testpac4j at gmail.com / Pac4jtest",
            br(),
            a(href := "/login/oidc")(
              "Protected url by OpenID Connect: /oidc"
            ),
            "(use a real account)",
            br(),
            a(href := "/login/form")(
              "Protected url by form authentication: /form"
            ),
            "(use username same as password)",
            br(),
            p(),
            a(href := "/logout")("Local Logout"),
            br(),
            renderProfiles(getProfiles(req))
          )
        )
      )
    case GET -> Root / "loginForm" =>
      Ok(
        form(
          action := "http://localhost:8080/callback?client_name=FormClient",
          method := "POST"
        )(
          input(`type` := "text", name := "username", value := "")(),
          p(),
          input(
            `type` := "password",
            name := "password",
            value := ""
          )(),
          p(),
          input(
            `type` := "submit",
            name := "submit",
            value := "Submit"
          )()
        )
      )
    case GET -> Root / "favicon.ico" =>
      NotFound()
    case req @ GET -> Root / "callback" =>
      callbackService.login(req)
    case req @ POST -> Root / "callback" =>
      callbackService.login(req)
    case req @ GET -> Root / "logout" =>
      localLogoutService.logout(req)
    case req @ GET -> Root / "centralLogout" =>
      centralLogoutService.logout(req)
  }

  val loginPages: HttpRoutes[IO] = HttpRoutes {
    case req @ GET -> Root / "form" =>
      protectedPagesFilter(Some("FormClient"))
        .apply(protectedPages)(req)
    case req @ GET -> Root / "facebook" =>
      protectedPagesFilter(Some("FacebookClient"))
        .apply(protectedPages)(req)
    case req @ GET -> Root / "oidc" =>
      protectedPagesFilter(Some("OidcClient"))
        .apply(protectedPages)(req)
    case req @ GET -> Root / "saml2" =>
      protectedPagesFilter(Some("SAML2Client"))
        .apply(protectedPages)(req)
  }

  val protectedPages: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req @ GET -> _ =>
      Ok(
        div()(
          h1()("Protected Page"),
          renderProfiles(getProfiles(req))
        )
      )
  }

  private def renderProfiles(
    profiles: List[CommonProfile]
  ): List[Text.TypedTag[String]] =
    profiles.map { profile =>
      p()(b("Profile: "), profile.toString, br())
    }
}
