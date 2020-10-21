package com.jonnyzzz.teamcity.rr

import org.jetbrains.teamcity.rest.BuildConfigurationId
import java.util.concurrent.TimeUnit


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

    val headName = listGitCurrentBranchName().removePrefix("refs/heads/")

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

fun listGitCommits(info: RRBranchInfo, commits: Int = 2048) : List<String>  = listGitCommits(info.commit, commits)

private fun <T> firstIndexOfAny(inList: List<T>, from: Collection<T>): Int? {
    val fromSet = from.toSet()
    val idx = inList.indexOfFirst { fromSet.contains(it) }
    return if (idx >= 0) idx else null
}

