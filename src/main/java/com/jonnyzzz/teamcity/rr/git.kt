package com.jonnyzzz.teamcity.rr

import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

private val ENV_GIT_COMMAND = "TEAMCITY_RR_GIT"
private val GIT_COMMAND = System.getenv(ENV_GIT_COMMAND) ?: "git"

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


data class RRBranchInfo(
        val fullName: String,
        val commit: String,
        val shortName: String,
        val originalBranchName: String
)

fun createRRBranch(): RRBranchInfo {
  val headCommit = execWithOutput(args = listOf(GIT_COMMAND, "rev-parse", "HEAD"),
          timeout = 5,
          timeoutUnit = TimeUnit.SECONDS
  ).successfully().stdout.trim()

  require(headCommit.length > 10) { "Failed to resolve current commit head" }
  val headName = execWithOutput(args = listOf(GIT_COMMAND, "rev-parse", "--symbolic-full-name", "HEAD"),
          timeout = 5,
          timeoutUnit = TimeUnit.SECONDS
  ).successfully().stdout.trim().removePrefix("refs/heads/")

  require(headName.isNotBlank()) { "Failed to resolve current commit branch name" }
  println("Current state: $headCommit from branch $headName")

  val targetShortBranch = "$headName-${headCommit.take(8)}"
  val targetBranchName = "refs/jonnyzzz-rr/$targetShortBranch"
  exec(args = listOf(GIT_COMMAND, "push", "origin", "$headCommit:$targetBranchName"),
          timeout = 5,
          timeoutUnit = TimeUnit.MINUTES
  )

  println("Pushed branch: $targetBranchName on commit $headCommit")
  return RRBranchInfo(
          fullName = targetBranchName,
          shortName = targetShortBranch,
          commit = headCommit,
          originalBranchName = headName
  )
}
