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
  implicit final class RequestOps(val v: Request[IO]) extends AnyVal {
    def session: IO[Option[Session]] =
      Session.requestAttr.map(v.attributes.lookup(_))
  }

  implicit final class ResponseOps(val v: Response[IO]) extends AnyVal {
    def clearSession: IO[Response[IO]] =
      Session.responseAttr.map(v.withAttribute(_, (_: Option[Session]) => None))

    def modifySession(f: Session => Session): IO[Response[IO]] = {
      val lf: Option[Session] => Option[Session] = _.cata(f.andThen(_.some), None)
      Session.responseAttr.map { key =>
        v.withAttribute(key ,v.attributes.lookup(key).cata(_.andThen(lf), lf))
      }
    }

    def newOrModifySession(f: Option[Session] => Session): IO[Response[IO]] = {
      val lf: Option[Session] => Option[Session] = f.andThen(_.some)
      Session.responseAttr.map { key =>
        v.withAttribute(key ,v.attributes.lookup(key).cata(_.andThen(lf), lf))
      }
    }

    def newSession(session: Session): IO[Response[IO]] =
      Session.responseAttr.map { key =>
        v.withAttribute(key, (_: Option[Session]) => Some(session))
      }
  }
}

trait CryptoManager[F[_]] {
  def encrypt(conetent: String): F[String]
  def decrypt(conetent: String): F[Option[String]]
  def sign(conent: String): F[String]
  def checkSign(signature: String, content: String): F[Option[Unit]]
}

object CryptoManager {
  def aes[F[_]: Sync](secret: String): CryptoManager[F] = new CryptoManager[F] {
    require(secret.length >= 16)

    private def constantTimeEquals(a: String, b: String): Boolean =
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

    def encrypt(content: String): F[String] = Sync[F].delay {
      val cipher = Cipher.getInstance("AES")
      cipher.init(Cipher.ENCRYPT_MODE, keySpec)
      Hex.encodeHex(cipher.doFinal(content.getBytes("UTF-8"))).mkString
    }

    def decrypt(content: String): F[Option[String]] = Sync[F].delay {
      val cipher = Cipher.getInstance("AES")
      cipher.init(Cipher.DECRYPT_MODE, keySpec)
      Try(new String(cipher.doFinal(Hex.decodeHex(content)), "UTF-8"))
        .toOption
    }

    def sign(content: String): F[String] = Sync[F].delay {
      val signKey = secret.getBytes("UTF-8")
      val signMac = Mac.getInstance("HmacSHA1")
      signMac.init(new SecretKeySpec(signKey, "HmacSHA256"))
      Base64
        .getEncoder
        .encodeToString(signMac.doFinal(content.getBytes("UTF-8")))
    }

    def checkSign(signature: String, content: String): F[Option[Unit]] =
      sign(content).map { actualSign =>
        if (constantTimeEquals(signature, actualSign)) ().some
        else None
    }
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
  cryptoManager: CryptoManager[F],
  maxAge: Duration
) {

  def cookie(content: String): F[ResponseCookie] = {
      val now = new Date().getTime / 1000
      val expires = now + maxAge.toSeconds
      val serialized = s"$expires-$content"
      for {
        signed <- cryptoManager.sign(serialized)
        encrypted <- cryptoManager.encrypt(serialized)
      } yield mkCookie(cookieName, s"$signed-$encrypted")
    }

   private[this] def split2(str: String): Option[(String, String)] =
     str.split('-') match {
       case Array(signature, value) => (signature, value).some
       case _ => None
   }

  private[this] def decrypt(cookie: RequestCookie): OptionT[F, String] =
    for {
      (signature, value) <- OptionT(Sync[F].pure(split2(cookie.content)))
      decrypted <- OptionT(cryptoManager.decrypt(value))
      _ <- OptionT(cryptoManager.checkSign(signature, decrypted))
    } yield decrypted

  def check(cookie: RequestCookie): OptionT[F, String] = {
      val now = new Date().getTime / 1000

      for {
        decrypted <- decrypt(cookie)
        (expires, content) <- OptionT(Sync[F].pure( split2(decrypted)))
        expiresSeconds <- OptionT(Sync[F].delay(Try(expires.toLong).toOption))
          if expiresSeconds > now
      } yield content
  }
}

object Session {
  import implicits._
  private val logger = LoggerFactory.getLogger(this.getClass)

  val requestAttr: IO[Key[Session]] = Key.newKey[IO, Session]
  val responseAttr: IO[Key[Option[Session] => Option[Session]]] =
    Key.newKey[IO, Option[Session] => Option[Session]]

  private[this] def sessionAsCookie(config: SessionConfig[IO], session: Session): IO[ResponseCookie] =
    config.cookie(session.noSpaces)

  private[this] def checkSignature(config: SessionConfig[IO], cookie: RequestCookie): OptionT[IO, Session] =
    config.check(cookie).mapFilter(jawn.parse(_).toOption)

  private[this] def sessionFromRequest(config: SessionConfig[IO], request: Request[IO]): OptionT[IO, Session] =
    (for {
      allCookies <- OptionT.liftF(IO.pure(request.cookies))
      sessionCookie <- OptionT(IO.pure(allCookies.find(_.name === config.cookieName)))
      session <- checkSignature(config, sessionCookie)
    } yield session)

  private[this] def applySessionUpdates(
    config: SessionConfig[IO],
    sessionFromRequest: Option[Session],
    serviceResponse: Response[IO]): Response[IO] = {
    ??? //  FIXME:
//    Task.delay {
//      serviceResponse match {
//        case response: Response =>
//          val updateSession = response.attributes.get(responseAttr) | identity
//          updateSession(sessionFromRequest).cata(
//            session => {
//              response.addCookie(sessionAsCookie(config, session))
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
    Middleware { (request, service) =>
      import SessionSyntax._
      OptionT(request.session).flatMap(_ => service(request)).orElse(OptionT.liftF(fallback))
    }

  def printRequestSessionKeys(sessionOpt: Option[Session]): IO[Unit] =
    IO.delay {
      sessionOpt match {
        case Some(session) => logger.debug("Request Session contains keys: " + session.asObject.map(_.toMap.keys.mkString(", ")))
        case None => logger.debug("Request Session empty")
      }
    }
}
