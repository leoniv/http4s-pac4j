package org.pac4j.http4s

import java.util.Date

import cats.implicits._
import cats.data.OptionT
import cats.effect._
import io.chrisdavenport.vault.Key
import io.circe.jawn
import javax.crypto.spec.SecretKeySpec
import javax.crypto.{Cipher, Mac}
import org.http4s.Http4s._
import org.http4s._
import org.http4s.server.{HttpMiddleware, Middleware}
import org.pac4j.http4s.SessionSyntax._
import org.slf4j.LoggerFactory
import java.util.Base64
import org.apache.commons.codec.binary.Hex

import scala.concurrent.duration.Duration
import scala.util.Try

/*
 * Cookie based sessions for http4s
 *
 * @author Hugh Giddens
 * @author Iain Cardnell
 */



object SessionSyntax {
  import implicits._
  implicit final class RequestOps[F[_]](val v: Request[F]) extends AnyVal {
    def session(implicit S: Sync[F]): F[Option[Session]] =
      ??? //FIXME S.pure(v.attributes.lookup(Session.requestAttr))

    def session2: Option[Session] =
      ??? //FIXME v.attributes.lookup(Session.requestAttr)
  }

// FIXME:
//  implicit final class TaskResponseOps[F[_]](val v: F[Response]) extends AnyVal {
//    def clearSession: F[Response] =
//      v.withAttribute(Session.responseAttr(_ => None))
//
//    def modifySession(f: Session => Session): Task[Response] = {
//      val lf: Option[Session] => Option[Session] = _.cata(f.andThen(_.some), None)
//      v.map { response =>
//        response.withAttribute(Session.responseAttr(response.attributes.get(Session.responseAttr).cata(_.andThen(lf), lf)))
//      }
//    }
//
//    def newSession(session: Session): Task[Response] =
//      v.withAttribute(Session.responseAttr(_ => Some(session)))
//  }

  implicit final class ResponseOps[F[_]](val v: Response[F]) extends AnyVal {
    def clearSession: Response[F] =
      ??? //FIXME: v.withAttribute(Session.responseAttr, _ => None)

    def modifySession(f: Session => Session): Response[F] = {
      val lf: Option[Session] => Option[Session] = _.cata(f.andThen(_.some), None)
      ??? //FIXME: v.withAttribute(Session.responseAttr(v.attributes.lookup(Session.responseAttr).cata(_.andThen(lf), lf)))
    }

    def newOrModifySession(f: Option[Session] => Session): Response[F] = {
//      val newUpdateFn: Option[Session] => Option[Session] = v.attributes.lookup(Session.responseAttr) match {
//        case Some(currentUpdateFn) => currentUpdateFn.andThen(f.andThen(_.some))
//        case None => f.andThen(_.some)
//      }
      ??? //FIXME: v.withAttribute(Session.responseAttr(newUpdateFn))
    }

    def newSession(session: Session): Response[F] =
     ??? //FIXME:  v.withAttribute(Session.responseAttr(_ => Some(session)))
  }
}

/**
  * Session Cookie Configuration
  *
  * @param cookieName
  * @param mkCookie
  * @param secret
  * @param maxAge
  */
final case class SessionConfig[F[_]: Sync](
  cookieName: String,
  mkCookie: (String, String) => ResponseCookie,
  secret: String,
  maxAge: Duration
) {
  require(secret.length >= 16)

  def constantTimeEquals(a: String, b: String): Boolean =
    if (a.length != b.length) {
      false
    } else {
      var equal = 0
      for (i <- Array.range(0, a.length)) {
        equal |= a(i) ^ b(i)
      }
      equal == 0
    }

  private[this] def keySpec: SecretKeySpec =
    new SecretKeySpec(secret.substring(0, 16).getBytes("UTF-8"), "AES")

  private[this] def encrypt(content: String): String = {
    val cipher = Cipher.getInstance("AES")
    cipher.init(Cipher.ENCRYPT_MODE, keySpec)
    Hex.encodeHex(cipher.doFinal(content.getBytes("UTF-8"))).mkString
  }

  private[this] def decrypt(content: String): Option[String] = {
    val cipher = Cipher.getInstance("AES")
    cipher.init(Cipher.DECRYPT_MODE, keySpec)
    Try(new String(cipher.doFinal(Hex.decodeHex(content)), "UTF-8")).toOption
  }

  private[this] def sign(content: String): String = {
    val signKey = secret.getBytes("UTF-8")
    val signMac = Mac.getInstance("HmacSHA1")
    signMac.init(new SecretKeySpec(signKey, "HmacSHA256"))
    Base64.getEncoder.encodeToString(signMac.doFinal(content.getBytes("UTF-8")))
  }
//
//  // FIXME: rename to `responseCookie`
//  def cookie(content: String): F[ResponseCookie] =
//    Sync[F].delay(cookie2(content))

  // FIXME: rename to `responseCookie`
  def cookie(content: String): ResponseCookie = {
      val now = new Date().getTime / 1000
      val expires = now + maxAge.toSeconds
      val serialized = s"$expires-$content"
      val signed = sign(serialized)
      val encrypted = encrypt(serialized)
      mkCookie(cookieName, s"$signed-$encrypted")
    }

  // FIXME: rename param to `requestCookie`
  def check(cookie: RequestCookie): F[Option[String]] =
    Sync[F].delay {
      val now = new Date().getTime / 1000
      cookie.content.split('-') match {
        case Array(signature, value) =>
          for {
            decrypted <- decrypt(value) if constantTimeEquals(signature, sign(decrypted))
            Array(expires, content) = decrypted.split("-", 2)
            expiresSeconds <- Try(expires.toLong).toOption if expiresSeconds > now
          } yield content
        case _ =>
          None
      }
    }
}

object Session {
  type Cookie = ResponseCookie
  import implicits._
  private val logger = LoggerFactory.getLogger(this.getClass)

  val requestAttr: IO[Key[Session]] = Key.newKey[IO, Session]
  val responseAttr: IO[Key[Option[Session] => Option[Session]]] =
    Key.newKey[IO, Option[Session] => Option[Session]]
// FIXME
//  private[this] def sessionAsCookie(config: SessionConfig[IO], session: Session): IO[Cookie] =
//    config.cookie(session.noSpaces)

  private[this] def sessionAsCookie(config: SessionConfig[IO], session: Session): Cookie =
    config.cookie(session.noSpaces)

  private[this] def checkSignature(config: SessionConfig[IO], cookie: RequestCookie): IO[Option[Session]] =
    config.check(cookie).map(_.flatMap(jawn.parse(_).toOption))

  private[this] def sessionFromRequest(config: SessionConfig[IO], request: Request[IO]): IO[Option[Session]] =
    (for {
      allCookies <- OptionT.liftF(IO.pure(request.cookies))
      sessionCookie <- OptionT(IO.pure(allCookies.find(_.name === config.cookieName)))
      session <- OptionT(checkSignature(config, sessionCookie))
    } yield session).value

  private[this] def applySessionUpdates(
    config: SessionConfig[IO],
    sessionFromRequest: Option[Session],
    serviceResponse: OptionT[IO, Response[IO]]): OptionT[IO, Response[IO]] = {
    ??? //  FIXME:
//    Task.delay {
//      serviceResponse match {
//        case response: Response =>
//          val updateSession = response.attributes.get(responseAttr) | identity
//          updateSession(sessionFromRequest).cata(
//            session => {
//              response.addCookie(sessionAsCookie2(config, session))
//            },
//            if (sessionFromRequest.isDefined) response.removeCookie(config.cookieName) else response
//          )
//        case Pass => Pass
//      }
//    }
  }

  def sessionManagement(config: SessionConfig[IO]): HttpMiddleware[IO] =
    ??? //  FIXME:
//    Middleware { (request, service) =>
//      logger.debug(s"starting for ${request.method} ${request.uri}")
//      for {
//        sessionFromRequest <- sessionFromRequest(config, request)
//        requestWithSession = sessionFromRequest.cata(
//          session => request.withAttribute(requestAttr, session),
//          request
//        )
//        _ <- printRequestSessionKeys(requestWithSession.session2)
//        maybeResponse <- service(requestWithSession)
//        responseWithSession <- applySessionUpdates(config, sessionFromRequest, maybeResponse)
//        _ <- Task.delay { logger.debug(s"finishing for ${request.method} ${request.uri}") }
//      } yield responseWithSession
//    }

  def sessionRequired(fallback: IO[Response[IO]]): HttpMiddleware[IO] =
    ??? //  FIXME:
//    Middleware { (request, service) =>
//      import SessionSyntax._
//      OptionT(request.session).flatMapF(_ => service(request).map(_.toOption)).getOrElseF(fallback)
//    }

  def printRequestSessionKeys(sessionOpt: Option[Session]): IO[Unit] =
    IO.delay {
      sessionOpt match {
        case Some(session) => logger.debug("Request Session contains keys: " + session.asObject.map(_.toMap.keys.mkString(", ")))
        case None => logger.debug("Request Session empty")
      }
    }
}