package org.pac4j.http4s.example

import cats.effect.IO
import org.http4s.{Charset, DefaultCharset, EntityEncoder, MediaType}
import org.http4s.MediaType
import org.http4s.headers.`Content-Type`
import scalatags.Text.TypedTag

trait ScalatagsInstances {

  implicit def htmlContentEncoder(
    implicit
    charset: Charset = DefaultCharset
  ): EntityEncoder[IO, TypedTag[String]] =
    contentEncoder(MediaType.text.html)

  private def contentEncoder[C <: TypedTag[String]](
    mediaType: MediaType
  )(implicit
    charset: Charset = DefaultCharset
  ): EntityEncoder[IO, C] =
    EntityEncoder
      .stringEncoder(charset)
      .contramap[C](content => content.render)
      .withContentType(`Content-Type`(mediaType, charset))
}

object ScalatagsInstances extends ScalatagsInstances
