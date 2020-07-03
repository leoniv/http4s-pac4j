import sbt._

object Versions {
  val circe = "0.13.0"
  val http4s = "0.21.6"
  val pac4j = "3.8.3"
  val specs2 = "4.10.0"
  val cats = "2.1.1"
  val catsEffect = "2.1.3"
  val vault = "2.0.0"
  val scalaTags = "0.6.7"
  val logback = "1.2.3"
}

object Deps {
  val circe = "io.circe" %% "circe-core" % Versions.circe
  val circejawn = "io.circe" %% "circe-jawn" % Versions.circe
  val http4sDsl = "org.http4s" %% "http4s-dsl" % Versions.http4s
  val http4sServer = "org.http4s" %% "http4s-server" % Versions.http4s
  val http4sCirce = "org.http4s" %% "http4s-circe" % Versions.http4s
  val http4sJawn = "org.http4s" %% "http4s-jawn" % Versions.http4s
  val http4sBlazeServer =
    "org.http4s" %% "http4s-blaze-server" % Versions.http4s
  val slf4jApi = "org.slf4j" % "slf4j-api" % "1.7.26"
  val logback =
    "ch.qos.logback" % "logback-classic" % Versions.logback
  val commonCodec = "commons-codec" % "commons-codec" % "1.14"
  val cats = "org.typelevel" %% "cats-core" % Versions.cats
  val catsEffect = "org.typelevel" %% "cats-effect" % Versions.cats
  val vault = "io.chrisdavenport" %% "vault" % Versions.vault
  val pack4j = "org.pac4j" % "pac4j-core" % Versions.pac4j
  val pack4jCas = "org.pac4j" % "pac4j-cas" % Versions.pac4j
  val pack4jHttp = "org.pac4j" % "pac4j-http" % Versions.pac4j
  val pack4jJwt = "org.pac4j" % "pac4j-jwt" % Versions.pac4j
  val pack4jOauth = "org.pac4j" % "pac4j-oauth" % Versions.pac4j
  val pack4jOidc = "org.pac4j" % "pac4j-oidc" % Versions.pac4j
  val pack4jOpenid = "org.pac4j" % "pac4j-openid" % Versions.pac4j
  val pack4jSaml = "org.pac4j" % "pac4j-saml" % Versions.pac4j
  val circeOptics = "io.circe" %% "circe-optics" % Versions.circe
  val specs2 = "org.specs2" %% "specs2-core" % Versions.specs2
  val specs2MatchersExtra =
    "org.specs2" %% "specs2-matcher-extra" % Versions.specs2
  val specs2Scalacheck =
    "org.specs2" %% "specs2-scalacheck" % Versions.specs2
  val specs2Cats = "org.specs2" %% "specs2-cats" % Versions.specs2
  val scalaTags = "com.lihaoyi" %% "scalatags" % Versions.scalaTags
}
