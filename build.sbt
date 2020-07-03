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

lazy val pack4jLib = project
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
      Deps.catsEffect,
      Deps.vault,
      Deps.circeOptics % Test,
      Deps.http4sJawn % Test,
      Deps.specs2 % Test,
      Deps.specs2MatchersExtra % Test,
      Deps.specs2Scalacheck % Test,
      Deps.specs2Cats % Test
    )
  )

lazy val pack4jExample = project
  .in(file("example"))
  .settings(
    libraryDependencies ++= Seq(
      Deps.http4sDsl,
      Deps.http4sServer,
      Deps.http4sBlazeServer,
      Deps.pack4j,
      Deps.pack4jCas,
      Deps.pack4jHttp,
      Deps.pack4jJwt,
      Deps.pack4jOauth,
      Deps.pack4jOidc,
      Deps.pack4jOpenid,
      Deps.pack4jSaml,
      Deps.cats,
      Deps.catsEffect,

      Deps.logback,
      Deps.slf4jApi,
      Deps.scalaTags
    ),
    //It's require for getting of net.shibboleth.tool:xmlsectool
    resolvers += "opensaml Repository" at
      "https://build.shibboleth.net/nexus/content/repositories/releases"
  ).dependsOn(pack4jLib)
