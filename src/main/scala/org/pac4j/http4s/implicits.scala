package org.pac4j.http4s

import org.http4s.{RequestCookie, ResponseCookie}

object implicits {
  implicit class OptionScalaz[A](oa: Option[A]) {
    def cata[X](
      some: (A) ⇒ X,
      none: ⇒ X
    ): X = oa match {
      case Some(a) => some(a)
      case None => none
    }

    def |(a: A): A = oa.getOrElse(a)
  }

  implicit class ResponseCookieOps(c: ResponseCookie) {
    def toRequestCookie: RequestCookie =
      RequestCookie(c.name, c.content)
  }
}
