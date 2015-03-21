package service

import ru.bugzmanov.prcheck.PullRequestBot

import scala.util.Random

class KarmaService(prbot: PullRequestBot, storage: SimpleStorage) {

  val UpVoteMessage = Seq("leveled up!", "is on the rise!", "+1!", "gained a level!")
  val DownVoteMessage = Seq("lost a level.", "took a hit! Ouch.", "took a hit.", "lost a life.")

  val karmaRise = "(?s).*@(.+)\\+\\+.*".r
  val karmaFall = "(?s).*@(.+)--.*".r

  def handleKarma(url: String, expression: String, commentAuthor: String) = {
    import Random.shuffle
    expression.replaceAll("\n", " ") match {
      case karmaRise(username) if username == commentAuthor => prbot.publishComment(url, s"Nice try, @$username. ಠ_ಠ")
      case karmaFall(username) if username == commentAuthor => prbot.publishComment(url, s"Nice try, @$username. ಠ_ಠ")
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
