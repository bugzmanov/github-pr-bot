package service

import ru.bugzmanov.prcheck.PrBot

import scala.util.Random

class KarmaService(prbot: PrBot, storage: SimpleStorage) {

  val UpVoteMessage = Seq("leveled up!", "is on the rise!", "+1!", "gained a level!")
  val DownVoteMessage = Seq("lost a level.", "took a hit! Ouch.", "took a hit.", "lost a life.")

  val karmaRise = ".*@(.+)\\+\\+.*".r
  val karmaFall = ".*@(.+)--.*".r

  def handleKarma(url: String, expression: String) = {
    import Random.shuffle
    expression match {
      case karmaRise(username) =>
        val current = storage.get(username).map(_.toInt).getOrElse(0) + 1
        storage.put(username, current.toString)
        prbot.publishComment(url, s"@$username " + shuffle(UpVoteMessage).head + s" Karma: $current")
      case karmaFall(username) =>
        val current = storage.get(username).map(_.toInt).getOrElse(0) - 1
        storage.put(username, current.toString)
        prbot.publishComment(url, s"@$username " + shuffle(DownVoteMessage).head + s" Karma: $current")
      case _ => //do nothing
    }
  }

}
