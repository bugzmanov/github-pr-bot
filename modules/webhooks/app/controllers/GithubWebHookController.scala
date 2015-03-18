package controllers

import java.util.concurrent.Executors

import play.api.libs.json._
import play.api._
import play.api.mvc._
import service.{KarmaService, ReviewService}

trait GithubWebHookController extends Controller {

  def reviewService: ReviewService

  def karmaService: KarmaService

  def botname = "iasbot"
  
  implicit val entityReads = Json.reads[PullRequest]

  def incoming = Action(BodyParsers.parse.json) { implicit request =>
    request.headers.get("X-GitHub-Event").map {
      case "issue_comment" => issueComment(request)
      case "pull_request" => pullRequest(request)
      case _ => //do nothing
    }

    Ok(Json.obj("status" -> "OK"))
  }

  def pullRequest(request: Request[JsValue]): Option[Any] = {
    val action = (request.body \ "action").asOpt[String]
    action.collect { case "opened" =>
      val number = request.body \ "number"
      val url = request.body \ "pull_request" \ "url"
      reviewService.reviewAsync(new PullRequest("opened", number.as[Int], url.as[String]))
    }
  }

  def issueComment(request: Request[JsValue]):Unit = {
    val action = (request.body \ "action").asOpt[String]
    action.collect { case "created" =>
      (request.body \ "issue" \ "pull_request" \ "url").asOpt[String].map { url =>
        val commentBody = (request.body \ "comment" \ "body").as[String]
        karmaService.handleKarma(url, commentBody)

        if (commentBody.trim == s"@$botname clean the mess") {
          reviewService.removeRobotReviewComments(url)
        } else if (commentBody.trim == s"@$botname please review") {
//          reviewService.reviewAsync()
          throw new Exception("NEEEEVEEEEER!")
        }
      }
    }
  }

}

case class PullRequest (
  action: String,
  number: Int,
  url: String
)