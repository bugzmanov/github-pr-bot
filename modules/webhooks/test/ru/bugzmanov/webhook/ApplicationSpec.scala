package ru.bugzmanov.webhook

import controllers.{GithubWebHookController, PullRequest}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play._
import play.api.libs.json.Json
import play.api.test._
import service.{JiraLinkerService, KarmaService, ReviewService}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class ApplicationSpec extends PlaySpec with MockitoSugar {

  trait MockContext {
    val reviewService = mock[ReviewService]
    val karmaService = mock[KarmaService]
    val jiraLinker = mock[JiraLinkerService]
    val botusername =  "bot"
  }

  "WebHookController" should  {
    "call reviewAsync as result of pull_request create event" in {
      val controller = new GithubWebHookController with MockContext

      when(controller.reviewService.reviewAsync(any())).thenReturn(Right("ok"))

      val payload = Json.parse(
        """
          |{
          |  "action": "opened",
          |  "number": 666,
          |  "pull_request" : {
          |    "url": "localhost",
          |    "title": "blabla"
          |   }
          |}""".stripMargin)
      val request = new FakeRequest("POST", "blbasd", FakeHeaders(Seq()), payload)
        .withHeaders(
          ("content-type", "application/json"),
          ("X-GitHub-Event", "pull_request"))

      val result  = controller.incoming().apply(request)
      val mvcResult = Await.result(result, 10 milli)
      mvcResult.header.status mustBe 200

      verify(controller.reviewService).reviewAsync(PullRequest("opened", 666, "localhost"))
    }

    "recognise '@bot please review' command" in {

      val controller = new GithubWebHookController with MockContext
      when(controller.reviewService.reviewAsync(any())).thenReturn(Right("ok"))

      val payload = Json.parse(
        """{
          |"action": "created",
          |"comment": {"body": "@bot please review" },
          |"issue": {
          |    "number": 666,
          |   "pull_request" : { "url": "localhost" } }
          |}
          |"""stripMargin)

      val request = new FakeRequest("POST", "blbasd", FakeHeaders(Seq()), payload)
        .withHeaders(
          ("content-type", "application/json"),
          ("X-GitHub-Event", "issue_comment")
        )

      val result  = controller.incoming().apply(request)
      val mvcResult = Await.result(result, 10 milli)
      mvcResult.header.status mustBe 200

      verify(controller.reviewService).reviewAsync(PullRequest("same", 666, "localhost"))
    }

    "recognise '@bot clean the mess' command" in {
      val controller = new GithubWebHookController with MockContext

      val payload = Json.parse(
        """{
          |  "action": "created",
          |  "comment": {
          |   "body": "@bot clean the mess",
          |   "user": {"login": "bugzmanov"}
          |  },
          |  "issue": {
          |    "pull_request" : { "url": "http://api.localhost/" }
          |  }
          |}
          |"""stripMargin)

      val request = new FakeRequest("POST", "blbasd", FakeHeaders(Seq()), payload)
        .withHeaders(
          ("content-type", "application/json"),
          ("X-GitHub-Event", "issue_comment")
        )

      val result  = controller.incoming().apply(request)
      val mvcResult = Await.result(result, 10 milli)
      mvcResult.header.status mustBe 200

      verify(controller.reviewService).removeRobotReviewComments("http://api.localhost/")
    }

    "ignore any other comments, but look for karma management" in {

      val controller = new GithubWebHookController with MockContext

      val payload = Json.parse(
        """{
          |"action": "created",
          |"comment": {
          |   "body": "@bot do some stuff",
          |   "user": {"login": "bugzmanov"}
          | },
          |"issue": {
          |   "pull_request" : { "url": "http://api.localhost/" } }
          |}
          |"""stripMargin)

      val request = new FakeRequest("POST", "blbasd", FakeHeaders(Seq()), payload)
        .withHeaders(
          ("content-type", "application/json"),
          ("X-GitHub-Event", "issue_comment")
        )
      val result  = controller.incoming().apply(request)
      val mvcResult = Await.result(result, 10 milli)
      mvcResult.header.status mustBe 200

      verifyZeroInteractions(controller.reviewService)

      verify(controller.karmaService).handleKarma("http://api.localhost/",  "@bot do some stuff", "bugzmanov")
    }
  }
}