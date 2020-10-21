package com.jonnyzzz.teamcity.rr

import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

private val ENV_GIT_COMMAND = "TEAMCITY_RR_GIT"
val GIT_COMMAND = System.getenv(ENV_GIT_COMMAND) ?: "git"

private class Git
private val LOG = LoggerFactory.getLogger(Git::class.java)

fun checkGitVersion() {
  val result = execWithOutput(
          args = listOf(GIT_COMMAND, "version"),
          timeout = 5,
          timeoutUnit = TimeUnit.SECONDS
  )

  if (result.exitCode != 0) {
    throw UserErrorException("Failed to execute `$GIT_COMMAND version` command. " +
            "Please check you have configured git in system path or set the " +
            "`$ENV_GIT_COMMAND` environment variable with the correct path.")
  }

  val gitVersion = result.stdout.trim().removePrefix("git version").trim()
  println("Using git version: $gitVersion")
}

fun listGitCommits(head: String, commits: Int = 2048) : List<String> {
  //git log --topo-order --no-abbrev-commit --format='%H' 01f6cfd510ae51e6a8fa22046843a121737c8fdc
  return execWithOutput(
          args = listOf(GIT_COMMAND, "log", "-$commits", "--topo-order", "--format=%H", head),
          timeout = 5,
          timeoutUnit = TimeUnit.MINUTES
  ).successfully().stdout.split("\n").map { it.trim() }.filter { it.isNotBlank() }
}

fun showCommitShort(commit: String) : String {
  //git log --topo-order --no-abbrev-commit --format='%H' 01f6cfd510ae51e6a8fa22046843a121737c8fdc
  val info = execWithOutput(
          args = listOf(GIT_COMMAND, "show", "--pretty=oneline", commit),
          timeout = 15,
          timeoutUnit = TimeUnit.SECONDS
  ).successfully().stdout.split("\n").map { it.trim() }.filter { it.isNotBlank() }.first()
  require(info.startsWith(commit))
  return info
}

fun generateDiffStat(commits: List<String>) : String {
  //git log --topo-order --no-abbrev-commit --format='%H' 01f6cfd510ae51e6a8fa22046843a121737c8fdc
  return execWithOutput(
          args = listOf(GIT_COMMAND, "diff", "--stat", *commits.toTypedArray()),
          timeout = 15,
          timeoutUnit = TimeUnit.SECONDS
  ).successfully().stdout
}
