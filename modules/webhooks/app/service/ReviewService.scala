package service

import java.util.concurrent.{CopyOnWriteArrayList, ConcurrentHashMap, Executors}

import controllers.PullRequest
import ru.bugzmanov.prcheck.PrBotApp
import play.api.Logger

class ReviewService(botname: String, bottoken: String) {

  val logger: Logger = Logger(this.getClass)

  val processing: ConcurrentHashMap[Int, AnyRef] = new ConcurrentHashMap[Int, AnyRef]()

  val executors = Executors.newFixedThreadPool(5)

  val prbot = PrBotApp

  def reviewAsync(pr: PullRequest) = {
    if (!processing.containsKey(pr.number)) {
      executors.submit(new Runnable() {
        override def run(): Unit = {
          logger.info(s"Started reviewing pull request: ${pr.url}")
          processing.put(pr.number, new Object())
          try {
            prbot.runReviewOnApiCall(pr.url, bottoken)
            logger.info(s"Successfully finished reviewing pull request: ${pr.url}")
          } catch {
            case e: Exception => logger.error(s"Pull request review failed ${pr.url}", e)
          } finally {
            processing.remove(pr.number)
          }
        }
      })
    }
  }

  def removeRobotReviewComments(pullRequestUrl: String) = {
    executors.submit(new Runnable {
      override def run(): Unit = {
        try {
          prbot.removeComments(pullRequestUrl, bottoken, botname)
        } catch {
          case e: Exception => logger.error("Couldn't remove code review comments", e)
        }
      }
    })
  }

}
