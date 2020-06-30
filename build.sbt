scalaVersion := "2.12.11" // Also supports 2.11.x
organization := "org.pac4j"
version      := "1.1.0-SNAPSHOT"

val circeVersion = "0.8.0"
val http4sVersion = "0.17.6"
val pac4jVersion = "3.8.3"
val specs2Version = "3.8.9"
val catsVersion = "0.9.0"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-jawn" % circeVersion,
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-server" % http4sVersion,
  "org.http4s" %% "http4s-circe" % http4sVersion,
  "org.pac4j" % "pac4j-core" % pac4jVersion,
  "org.slf4j" % "slf4j-api" % "1.7.26",
  "commons-codec" % "commons-codec" % "1.14",
  "org.typelevel" %% "cats-core" % catsVersion,

  "io.circe" %% "circe-optics" % circeVersion % Test,
  "org.http4s" %% "http4s-jawn" % http4sVersion % Test,
  "org.specs2" %% "specs2-matcher-extra" % specs2Version % Test,
  "org.specs2" %% "specs2-scalacheck" % specs2Version % Test,
  "org.specs2" %% "specs2-scalaz" % specs2Version % Test
)

scalacOptions ++= Seq("-Ypartial-unification", "-language:implicitConversions", "-language:higherKinds")
