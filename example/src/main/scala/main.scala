package org.pac4j.http4s
package example

import cats.effect._
import org.http4s._
import org.http4s.server._
import org.http4s.server.blaze._
import org.http4s.implicits._
import scala.concurrent.duration._
import org.pac4j.core.profile.{CommonProfile, ProfileManager}
import scala.collection.JavaConverters._

object Main extends IOApp {
  type AuthClient = String

  val sessionConfig = SessionConfig(
    cookieName = "session",
    mkCookie = ResponseCookie(_, _, path = Some("/")),
    cryptoManager = CryptoManager.aes[IO]("This is a secret"),
    maxAge = 5.minutes
  )

  val config = ConfigFactory.build()
  val callbackService = new CallbackService(config)

  val localLogoutService = new LogoutService(
    config,
    Some("/?defaulturlafterlogout"),
    destroySession = true
  )
  val centralLogoutService = new LogoutService(
    config,
    defaultUrl =
      Some("http://localhost:8080/?defaulturlafterlogoutafteridp"),
    destroySession = true,
    logoutUrlPattern = Some("http://localhost:8080/.*"),
    localLogout = false,
    centralLogout = true
  )

  val protectedPagesFilter: Option[AuthClient] => HttpMiddleware[IO] =
    clients =>
      SecurityFilterMiddleware.securityFilter(
        config,
        clients = clients
      )

  // Helper to retrieve authorized profile from request
  val getProfiles: Request[IO] => List[CommonProfile] = request => {
    val context = Http4sWebContext(request, config)
    val manager = new ProfileManager[CommonProfile](context)
    manager.getAll(true).asScala.toList
  }

  val routes = Routes(
    getProfiles,
    callbackService,
    localLogoutService,
    centralLogoutService,
    protectedPagesFilter
  )

  def mkHttpApp: HttpApp[IO] =
    Session
      .sessionManagement(sessionConfig) {
        Router[IO](
          "/" -> routes.root,
          "/login" -> routes.loginPages,
          "/protected" -> protectedPagesFilter(None)(
            routes.protectedPages
          )
        )
      }
      .orNotFound

  def mkServer(httpApp: HttpApp[IO]): Resource[IO, Server[IO]] =
    BlazeServerBuilder[IO]
      .bindHttp(8080, "localhost")
      .withHttpApp(httpApp)
      .resource

  def run(arg: List[String]): IO[ExitCode] =
    mkServer(mkHttpApp).use(_ => IO.never).as(ExitCode.Success)
}
