package service

import java.util.concurrent.{CopyOnWriteArrayList, ConcurrentHashMap, Executors}

import controllers.PullRequest
import ru.bugzmanov.prcheck.PrBot
import play.api.Logger

class ReviewService(prbot: PrBot) {

  val logger: Logger = Logger(this.getClass)

  val processing: ConcurrentHashMap[Int, AnyRef] = new ConcurrentHashMap[Int, AnyRef]()

  val executors = Executors.newFixedThreadPool(5)

  def reviewAsync(pr: PullRequest) = {
    if (!processing.containsKey(pr.number)) {
      executors.submit(new Runnable() {
        override def run(): Unit = {
          logger.info(s"Started reviewing pull request: ${pr.url}")
          processing.put(pr.number, new Object())
          try {
            prbot.runReviewOnApiCall(pr.url)
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
          prbot.removeComments(pullRequestUrl)
        } catch {
          case e: Exception => logger.error("Couldn't remove code review comments", e)
        }
      }
    })
  }

}
