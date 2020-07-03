package org.pac4j.http4s
package example

import cats.effect._
import org.http4s._
import org.http4s.server._
import org.http4s.server.blaze._
import org.http4s.dsl.io._
import org.http4s.implicits._

object Main extends IOApp {
  def run(arg: List[String]): IO[ExitCode] =
    mkServer(mkHttpApp).use(_ => IO.never).as(ExitCode.Success)

  def mkHttpApp: HttpApp[IO] = Router[IO]("/" -> HttpRoutes.empty).orNotFound

  def mkServer(httpApp: HttpApp[IO]): Resource[IO, Server[IO]] =
    BlazeServerBuilder[IO]
      .bindHttp(8080, "localhost")
      .withHttpApp(httpApp)
      .resource
}
