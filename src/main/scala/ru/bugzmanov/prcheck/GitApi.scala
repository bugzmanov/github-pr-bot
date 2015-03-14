package ru.bugzmanov.prcheck

import java.io.File

import org.eclipse.jgit.api.{CreateBranchCommand, Git}
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider

class GitApi(directory: File,
             overrideExistingDir: Boolean = true,
             url: String,
             username: String,
             password: String) {

  if (directory.exists() && overrideExistingDir) {
    deleteFile(directory)
  }

  private def deleteFile(dfile : File) : Unit = {
    if(dfile.isDirectory){
      val subfiles = dfile.listFiles
      if(subfiles != null)
        subfiles.foreach{ f => deleteFile(f) }
    }
    dfile.delete
  }

  def cloneTo(branchName: String = "master") {
    val git: Git = Git.cloneRepository()
      .setURI(url)
      .setDirectory(directory)
      .setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password))
      .call()

    git.checkout()
      .setName(s"origin/$branchName")
      .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
      .setStartPoint(s"origin/$branchName")
      .call()
  }

  def changeBranch(branchName: String): Unit = {
    Git.open(directory)
      .checkout()
      .setName(s"origin/$branchName")
      .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
      .setStartPoint(s"origin/$branchName")
      .call()
  }

  def merge(branchName: String): Unit = {
    val open: Git = Git.open(directory)
    val walk: RevWalk = new RevWalk(open.getRepository)
    val ref: Ref = open.getRepository.getRef(s"refs/remotes/origin/$branchName")
    open.merge().include(ref).call()
  }

  def clean(): Unit ={
    deleteFile(directory)
  }
}
