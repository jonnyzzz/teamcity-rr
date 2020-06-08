package com.jonnyzzz.teamcity.rr

import org.jetbrains.teamcity.rest.BuildConfigurationId
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
        val originalBranchName: String,
        val targetBuildConfigurationId: BuildConfigurationId
)

fun createRRBranch(): RRBranchInfo {
  val headCommit = execWithOutput(args = listOf(GIT_COMMAND, "rev-parse", "HEAD"),
          timeout = 5,
          timeoutUnit = TimeUnit.SECONDS
  ).successfully().stdout.trim()

  require(headCommit.length > 10) { "Failed to resolve current commit head" }

  val userName = (System.getProperty("user.name") ?: error("'user.name' is not set")).map {
    if (it.isLetterOrDigit() || it == '-' || it == '_') it else '_'
  }.joinToString("")

  val headName = execWithOutput(args = listOf(GIT_COMMAND, "rev-parse", "--symbolic-full-name", "HEAD"),
          timeout = 5,
          timeoutUnit = TimeUnit.SECONDS
  ).successfully().stdout.trim().removePrefix("refs/heads/")

  require(headName.isNotBlank()) { "Failed to resolve current commit branch name" }
  println("Current state: $headCommit from branch $headName")

  val recentCommits = listGitCommits(headCommit)

  val buildConfiguration = ijAggPerGitBranch.entries
          .map { it.key to listGitCommits("origin/${it.value}") }
          .mapNotNull { (build, commits) ->  firstIndexOfAny(commits, recentCommits)?.let { build to it} }
          .minBy { it.second }?.first ?: error("No common commits were found with ${ijAggPerGitBranch.values.toSortedSet()}")

  println("Resolved aggregator build configuration: $buildConfiguration")

  val targetShortBranch = "$userName-${headCommit.take(8)}"
  val targetBranchName = "$customGitBranchNamePrefix/$targetShortBranch"
  exec(args = listOf(GIT_COMMAND, "push", "origin", "$headCommit:$targetBranchName"),
          timeout = 5,
          timeoutUnit = TimeUnit.MINUTES
  )

  println("Pushed branch: $targetBranchName on commit $headCommit")
  return RRBranchInfo(
          fullName = targetBranchName,
          shortName = targetShortBranch,
          commit = headCommit,
          originalBranchName = headName,
          targetBuildConfigurationId = buildConfiguration
  )
}

private fun <T> firstIndexOfAny(inList: List<T>, from: Collection<T>): Int? {
  val fromSet = from.toSet()
  val idx = inList.indexOfFirst { fromSet.contains(it) }
  return if (idx >= 0) idx else null
}

fun listGitCommits(info: RRBranchInfo, commits: Int = 2048) : List<String>  = listGitCommits(info.commit, commits)

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
