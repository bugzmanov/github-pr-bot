package ru.bugzmanov.webhook

import org.mockito.Mockito
import org.scalatest.{Matchers, FlatSpec}
import org.scalatest.mock.MockitoSugar
import ru.bugzmanov.prcheck.PullRequestBot
import service.{KarmaService, SimpleStorage}
import Matchers._
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.hamcrest.{Matchers => JMatchers}

import scala.collection.mutable

class KarmaServiceSpec extends FlatSpec  with MockitoSugar  {
  val url = "http://localhost:8080"

  def simpleStorage: SimpleStorage = new SimpleStorage {
    val map = mutable.Map[String, String]()

    override def get(key: String): Option[String] = map.get(key)
    override def put(key: String, value: String): Unit = map.put(key, value)
  }

  "Karma service" should "set karma to '1' if it faces @user++ first time" in {
    val storage  = simpleStorage
    val karmaService = new KarmaService(mock[PullRequestBot], storage)

    karmaService.handleKarma(url, "hey @user++", "someone")
    storage.get("user") should be (Some("1"))
  }

  "Karma service" should "decrease karma if faces @user--" in {
    val storage  = simpleStorage
    val karmaService = new KarmaService(mock[PullRequestBot], storage)

    karmaService.handleKarma(url, "hey @user--", "someone")
    storage.get("user") should be (Some("-1"))
  }

  "Karma service" should "not allow user to modify his own karma" in {
    val storage  = simpleStorage
    val karmaService = new KarmaService(mock[PullRequestBot], storage)

    storage.put("user", "1")

    karmaService.handleKarma(url, "hey @user++", "user")
    storage.get("user") should be (Some("1"))

    karmaService.handleKarma(url, "hey @user--", "user")
    storage.get("user") should be (Some("1"))
  }

  "Karma service" should "publish new karma value on github" in {
    val storage  = simpleStorage
    storage.put("user", "1")

    val bot: PullRequestBot = mock[PullRequestBot]
    val karmaService = new KarmaService(bot, storage)

    karmaService.handleKarma(url, "hey @user++", "someoneelse")

    import JMatchers._
    verify(bot).publishComment(argThat(equalTo(url)), argThat(endsWith("Karma: 2")))
  }
}
