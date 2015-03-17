package controllers

import java.util.concurrent.Executors

import play.api.libs.json._
import play.api._
import play.api.mvc._
import service.ReviewService

trait GithubWebHookController extends Controller {

  def reviewService: ReviewService

  def botname = "@iasbot"

  implicit val entityReads = Json.reads[PullRequest]

  def incoming = Action(BodyParsers.parse.json) { implicit request =>
    println("----")
    request.headers.get("X-GitHub-Event").map {
      case "issue_comment" => //do nothing
        val action = (request.body \ "action").asOpt[String]
        action.collect { case "created" =>
          (request.body \ "issue" \ "pull_request" \ "url").asOpt[String].map { url =>
            val body = (request.body \ "comment").as[String]

          }
        }
      case "pull_request" =>
        val action = (request.body \ "action").asOpt[String]
        action.collect { case "opened" =>
          val number = request.body \ "number"
          val url = request.body \ "pull_request" \ "url"
          reviewService.reviewAsync(new PullRequest("opened", number.as[Int], url.as[String]))
        }
      case _ => //do nothing
    }

    Ok(Json.obj("status" -> "OK"))
  }

  def index2 = Action {

    Ok("Your new application is ready.")
  }
//  def newPRComment = Action(BodyParsers.parse.json) { request =>
//        request.body.
//    Ok(Json.obj("status" -> "OK"))
//  }
}

case class PullRequest (
  action: String,
  number: Int,
  url: String
)