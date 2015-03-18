package service

import java.util.concurrent.ConcurrentHashMap

import ru.bugzmanov.prcheck.PrBotApp

class KarmaService(token: String) {

  val karma: ConcurrentHashMap[String, Integer] =  new ConcurrentHashMap[String, Integer]()

  val karmaRise = ".*@(.+)\\+\\+.*".r
  val karmaFall = ".*@(.+)--.*".r

  def handleKarma(url: String, expression: String) = {

    expression match {
      case karmaRise(username) =>
        val current: Integer = karma.putIfAbsent(username, 1)
        if (current != null)
          karma.replace(username, current + 1)
        PrBotApp.publishComment(url, s"@$username is on a rise! Karma: ${karma.get(username)}", token)
      case karmaFall(username) =>
        val current: Integer = karma.putIfAbsent(username, -1)
        if (current != null)
          karma.replace(username, current - 1)
        PrBotApp.publishComment(url, s"@$username took a hit! Ouch.. Karma: ${karma.get(username)}", token)
      case _ => //do nothing
    }
  }

}
