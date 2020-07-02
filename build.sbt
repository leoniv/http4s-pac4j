inThisBuild(
  Seq(
    scalaVersion := "2.12.11",
    organization := "org.pac4j",
    version := "2.0.0-SNAPSHOT",
    scalacOptions ++= Seq(
      "-Ypartial-unification",
      "-language:implicitConversions",
      "-language:higherKinds"
    )
  )
)

lazy val lib = project
  .in(file("."))
  .settings(
    libraryDependencies ++= Seq(
      Deps.circe,
      Deps.circejawn,
      Deps.http4sDsl,
      Deps.http4sServer,
      Deps.http4sCirce,
      Deps.pack4j,
      Deps.slf4jApi,
      Deps.commonCodec,
      Deps.cats,
      Deps.vault,
      Deps.circeOptics % Test,
      Deps.http4sJawn % Test,
      Deps.specs2 % Test,
      Deps.specs2MatchersExtra % Test,
      Deps.specs2Scalacheck % Test,
      Deps.specs2Cats % Test
    )
  )
