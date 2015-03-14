package ru.bugzmanov.prcheck

import java.io.File
import javax.json.JsonObject
import javax.ws.rs.core.{HttpHeaders, MediaType}
import javax.xml.bind.DatatypeConverter

import com.jcabi.github.Coordinates.Simple
import com.jcabi.github.RtGithub
import com.jcabi.http.Request
import com.jcabi.http.request.ApacheRequest
import com.jcabi.http.response.{JsonResponse, RestResponse}
import com.jcabi.manifests.Manifests
import org.apache.commons.io.Charsets
import org.apache.http.HttpStatus

case class PullRequest(
  author: String,
  fromBranch: String,
  intoBranch: String,
  fromCommit: String,
  toCommit: String
)

class GithubApi(account: String, repo: String, header: String, username: String, pass: String, github: RtGithub) {

  private val githubrepo = github.repos().get(new Simple(account, repo))

  private val USER_AGENT = s"jcabi-github" +
    s" ${Manifests.read("JCabi-Version")}" +
    s" ${Manifests.read("JCabi-Build")}" +
    s" ${Manifests.read("JCabi-Date")}"

  def downloadDiff(requestId: Int): String = github.entry()
      .header(HttpHeaders.CONTENT_TYPE, "application/vnd.github.3.diff")
      .header(HttpHeaders.ACCEPT, "application/vnd.github.3.diff")
      .uri().path(s"repos/$account/$repo/pulls/$requestId").back()
      .fetch()
      .as(classOf[RestResponse])
      .assertStatus(HttpStatus.SC_OK)
      .toString

  def publishPrComment(pr: Int, body: String) = githubrepo.issues().get(pr).comments().post(body)

  def publishComment(pr: Int, body: String, commit: String, path: String, position: Int) = githubrepo
    .pulls().get(pr)
    .comments()
    .post(body, commit, path, position)

  def describePR(pr: Int): PullRequest = {
    val json = github.entry()
      .uri().path(s"repos/$account/$repo/pulls/$pr").back()
      .fetch()
      .as(classOf[RestResponse])
      .assertStatus(HttpStatus.SC_OK)
      .as(classOf[JsonResponse])
      .json().readObject()

    val from: JsonObject = json.getJsonObject("head")

    val to: JsonObject = json.getJsonObject("base")
    new PullRequest(
      author = json.getJsonObject("user").getString("login"),
      fromBranch = from.getString("ref"),
      intoBranch = to.getString("ref"),
      fromCommit = from.getString("sha"),
      toCommit = to.getString("sha")
    )
  }

  private def baseRequest(url: String): Request = new ApacheRequest(url)
    .header(HttpHeaders.USER_AGENT, USER_AGENT)
    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)

  def cloneTo(directory: File, branchName: String = "master"): GitApi = {
    val api: GitApi = new GitApi(directory,
      overrideExistingDir = true,
      s"https://github.com/$account/$repo",
      username = username,
      password = pass
    )
    api.cloneTo(branchName)
    api

  }
}

object GithubApi {

  def tokenBased(account: String, repo: String, token: String) =
    new GithubApi(account,
      repo,
      String.format("token %s", token),
      token,
      "",
      new RtGithub(token)
    )

  def username(account: String, repo: String, username: String, password: String) =
    new GithubApi(account,
      repo,
      String.format("Basic %s",
        DatatypeConverter.printBase64Binary(s"$username:$password".getBytes(Charsets.UTF_8))),
      username,
      password,
      new RtGithub(username, password)
    )
}
