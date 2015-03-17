package controllers

import play.api._
import play.api.mvc._
import service.ReviewService

object Application extends Controller with GithubWebHookController{

  val conf = Play.current.configuration

  val reviewService = new ReviewService(conf.getString("github.username", None).get, conf.getString("github.token", None).get)

  def index = Action {
    Ok("Your new application is ready.")
  }


}
