package org.pac4j.http4s

import io.circe.{Json, JsonObject}
import org.specs2.mutable.Specification
import Http4sCookieSessionStore._

object Http4sCookieSessionStoreSpec
    extends Specification
    with Http4sCookieSessionStore {
  "get" should {
    "return null if no session" >> {
      get(None, "test") must beNone
    }
    "return null if session empty" >> {
      val session = Json.fromJsonObject(JsonObject.empty)
      get(Some(session), "test") must beNone
    }
    "return null if session exists but key not present" >> {
      val session = Json.fromJsonObject(
        JsonObject.singleton("other", Json.fromString("hello"))
      )
      get(Some(session), "test") must beNone
    }
    "return value if session exists and key present" >> {
      val session = Json.fromJsonObject(
        JsonObject
          .singleton("test", Json.fromString(serialise("hello")))
      )
      get(Some(session), "test") must beSome("hello")
    }
    "return value if session exists and key present among others" >> {
      val session = Json.fromJsonObject(
        JsonObject.fromMap(
          Map(
            "test" -> Json.fromString(serialise("hello")),
            "other" -> Json.fromString(serialise("value"))
          )
        )
      )
      get(Some(session), "test") must beSome("hello")
    }
  }

  "set" should {
    "add a key if no session" >> {
      val newSession = set(None, "test", "hello")
      newSession.noSpaces must_== """{"test":"rO0ABXQABWhlbGxv"}"""
    }
    "add a key if session exists without that key" >> {
      val session = Json.fromJsonObject(
        JsonObject.singleton("other", Json.fromString("hello"))
      )
      val newSession = set(Some(session), "test", "hello")
      newSession.noSpaces must_== """{"other":"hello","test":"rO0ABXQABWhlbGxv"}"""
    }
    "update a key if session exists with that key" >> {
      val session = Json.fromJsonObject(
        JsonObject
          .singleton("test", Json.fromString(serialise("hello")))
      )
      val newSession = set(Some(session), "test", "world")
      newSession.noSpaces must_== """{"test":"rO0ABXQABXdvcmxk"}"""
    }
    "remove a key if session exists with that key" >> {
      val session = Json.fromJsonObject(
        JsonObject
          .singleton("test", Json.fromString(serialise("hello")))
      )
      val newSession = set(Some(session), "test", null)
      newSession.noSpaces must_== """{}"""
    }
  }
}
