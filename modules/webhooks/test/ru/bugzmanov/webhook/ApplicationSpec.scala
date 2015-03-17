package ru.bugzmanov.webhook

import javax.json.JsonValue

import controllers.{PullRequest, GithubWebHookController}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Request, AnyContentAsJson}
import play.api.test._
import service.ReviewService

import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import org.mockito.Mockito._
import org.mockito.Matchers._

class ApplicationSpec extends PlaySpec with MockitoSugar {

  "WebHookController" should  {
    "call reviewAsync as result of pull_request/create event" in {

      val service = mock[ReviewService]

      val controller = new GithubWebHookController {
        override def reviewService: ReviewService = service
      }

      val parse: JsValue = Json.parse( """{"action": "opened",  "number": 666, "pull_request" : { "url": "localhost" } }""")
      val request = new FakeRequest("POST", "blbasd", FakeHeaders(Seq()), parse)
        .withHeaders(
          ("content-type", "application/json"),
          ("User-Agent", "GitHub-Hookshot/9f39283"),
          ("X-GitHub-Delivery", "769e1d00-c6a3-11e4-8e93-3745994ca473"),
          ("X-GitHub-Event", "pull_request"),
          ("X-Hub-Signature", "sha1=0262bc242ea8becaff4aa16dce5c612cd66a9519"))

      val result  = controller.incoming().apply(request)

      val mvcResult = Await.result(result, 10 milli)

      mvcResult.header.status mustBe 200
      verify(service).reviewAsync(PullRequest("opened", 666, "localhost"))
    }
  }
}