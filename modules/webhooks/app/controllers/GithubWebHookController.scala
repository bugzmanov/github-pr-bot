package controllers

import java.util.concurrent.Executors

import play.api.libs.json._
import play.api._
import play.api.mvc._
import service.{JiraLinkerService, KarmaService, ReviewService}

trait GithubWebHookController extends Controller {

  val NothingToDo: Right[Nothing, String] = Right("Nothing to do here")

  lazy val CleanTheMessCommand = s"@$botusername clean the mess"
  lazy val PleaseReviewCommand = s"@$botusername please review"

  def reviewService: ReviewService
  def karmaService: KarmaService
  def jiraLinker: JiraLinkerService

  def botusername: String

  def incoming = Action(BodyParsers.parse.json) { implicit request =>
    val result = request.headers.get("X-GitHub-Event").collect {
      case "issue_comment" => issueComment(request)
      case "pull_request" => pullRequest(request)
    }.getOrElse(NothingToDo)

    result match {
      case Right(msg) => Ok(Json.obj("status" -> "OK", "message" -> msg))
      case Left(msg) => Conflict(Json.obj("status" -> "NOT_OK", "message" -> msg))
    }

  }

  def pullRequest(request: Request[JsValue]): Either[String, String] = {
    val action = (request.body \ "action").asOpt[String]
    action.collect { case "opened" =>
      val number = request.body \ "number"
      val url = request.body \ "pull_request" \ "url"
      val title = request.body \ "pull_request" \ "title"

      jiraLinker.handlePullRequest(url.as[String], title.as[String])

      reviewService.reviewAsync(new PullRequest("opened", number.as[Int], url.as[String]))
    }.getOrElse(NothingToDo)
  }

  def issueComment(request: Request[JsValue]): Either[String, String] = {
    val action = (request.body \ "action").asOpt[String]

    action.collect { case "created" =>
      (request.body \ "issue" \ "pull_request" \ "url").asOpt[String].map { url =>
        processComment(request, url)
      }.getOrElse(NothingToDo)
    }.getOrElse(NothingToDo)
  }


  def processComment(request: Request[JsValue], url: String): Either[String, String] = {
    val commentBody = (request.body \ "comment" \ "body").as[String]

    commentBody.trim match {
      case CleanTheMessCommand =>
        reviewService.removeRobotReviewComments(url)
        Right("messages will be removed")
      case PleaseReviewCommand =>
        val prNumber = request.body \ "issue" \ "number"
        reviewService.reviewAsync(new PullRequest("same", prNumber.as[Int], url))
      case _ =>
        val author = (request.body \ "comment" \ "user" \ "login").as[String]
        karmaService.handleKarma(url, commentBody, author)
        NothingToDo
    }
  }
}

case class PullRequest (
  action: String,
  number: Int,
  url: String
)

