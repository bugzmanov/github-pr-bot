package service

import ru.bugzmanov.prcheck.PullRequestBot

class JiraLinkerService(prbot: PullRequestBot, projectCodes: Set[String], jiraUrl: String) {

  val reg = "([a-zA-Z]+).([0-9]+).*".r

  def handlePullRequest(url: String, title: String) = {
    title match {
      case reg(ticket, number) if projectCodes.contains(ticket.toUpperCase) =>
        val ticketUrl: String = s"$jiraUrl/$ticket-$number"
        prbot.updateDescriprtion(url, s"""Link: <a href="$ticketUrl" target="_blank">jira $ticket-$number</a>""" )
      case _ => // do nothing
    }
  }

}
