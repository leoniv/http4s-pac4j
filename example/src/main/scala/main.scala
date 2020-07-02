package org.pac4j.http4s

import cats.effect._

object Main extends IOApp {
  def run(arg: List[String]): IO[ExitCode] = {
    IO.delay(println("FIXME")).as(ExitCode.Success)
  }
}
