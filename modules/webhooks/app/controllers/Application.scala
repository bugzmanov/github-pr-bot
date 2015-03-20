package controllers

import play.api._
import play.api.libs.json.Json
import play.api.mvc._
import ru.bugzmanov.prcheck.PullRequestBot
import service.{JiraLinkerService, SimpleStorage, KarmaService, ReviewService}
import scala.collection.JavaConversions._

object Application extends Controller with GithubWebHookController{

  val conf = Play.current.configuration

  val botusername: String = conf.getString("github.username", None).get
  private val bottoken: String = conf.getString("github.token", None).get

  private val jiraUrl: String = conf.getString("jira.url", None).get
  private val jiraCodes = conf.getStringList("jira.project.codes").get

  val prbot = new PullRequestBot(bottoken, botusername)

  val reviewService = new ReviewService(prbot)

  val simpleStorage: SimpleStorage = new SimpleStorage

  val karmaService = new KarmaService(prbot, simpleStorage)

  val jiraLinker = new JiraLinkerService(prbot, jiraCodes.toSet, jiraUrl)

  def index = Action {
    Ok(Json.obj("status" -> "OK"))
  }
}
