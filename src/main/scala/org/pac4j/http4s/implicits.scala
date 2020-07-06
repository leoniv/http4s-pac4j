package org.pac4j.http4s

import org.http4s.{ResponseCookie, RequestCookie}

object implicits {
  implicit class OptionScalaz[A](oa: Option[A]) {
    def cata[X](some: (A) ⇒ X, none: ⇒ X): X = oa match {
      case Some(a) => some(a)
      case None    => none
    }
  }
}
