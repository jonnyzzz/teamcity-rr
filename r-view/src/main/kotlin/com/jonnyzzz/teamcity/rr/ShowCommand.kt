package com.jonnyzzz.teamcity.rr

import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*

fun showPendingBuilds(args: List<String>) = ShowCommand().showPendingBuilds(args)

private class ShowCommand {
    private val defaultGit by lazy {
        val git = GitRunner(workdir = WorkDir)
        git.checkGitVersion()
        git
    }

    private val safePushSuffix = "safepush/Eugene.Petrenko"
    private val defaultSafePushBranchPrefix = "refs/$safePushSuffix"
    private val defaultLocalPushBranchPrefix = "origin/safepush"

    private val defaultBranchPrefix = "refs/heads/jonnyzzz/"  //TODO: configuration?

    private val gitBranchHistory by lazy { GitBranchHistory() }

    fun showPendingBuilds(args: List<String>) {
        println("Checking current status...")
        println()

        if ("--no-fetch" !in args) {
            println("Fetching changes from remote...")
            defaultGit.execGit(WithInheritSuccessfully, timeout = Duration.ofMinutes(10),
                    command = "fetch",
                    args = listOf("--prune", "origin",
                            "refs/heads/master:origin/master",
//                            "$defaultSafePushBranchPrefix/*:$defaultLocalPushBranchPrefix/*"
                    ))
        }

        val recentCommits = defaultGit.listGitCommitsEx("origin/master", commits = 2048)
                .associateBy { it.commitId }

        val headCommit = defaultGit.gitHeadCommit("origin/master")

        println("Listed ${recentCommits.size} recent commits")

        val alreadyMergedBranches = TreeMap<String, String>()
        val rebaseFailedBranches = TreeMap<String, String>()
        val otherBranches = TreeMap<String, String>()

        for ((branch, commit) in defaultGit.listGitBranches().toSortedMap()) {
            if (!branch.startsWith(defaultBranchPrefix)) continue

            if (commit in recentCommits) {
                alreadyMergedBranches += branch to commit
                continue
            }

            println("Rebasing $branch...")
            if (gitBranchHistory.isBrokenForRebase(commit)) {
                println("Rebasing failed already")
                rebaseFailedBranches += branch to commit
                continue
            }

            val rebaseResult = defaultGit.gitRebase(branch = branch, toHead = headCommit)
            if (rebaseResult != null) {
                val newCommitId = rebaseResult.newCommitId
                if (newCommitId in recentCommits) {
                    alreadyMergedBranches += branch to newCommitId
                    continue
                }

                otherBranches += branch to newCommitId
                continue
            }


            gitBranchHistory.logRebaseFailed(commit)
            rebaseFailedBranches += branch to commit
        }

        println("Collected ${alreadyMergedBranches.size + rebaseFailedBranches.size + otherBranches.size} local Git branches with $defaultBranchPrefix")
        println()
        if (alreadyMergedBranches.isNotEmpty()) {
            println("Already completed and merged branches:")
            for ((branch, _) in alreadyMergedBranches) {
                println("  $branch")
            }
            println()
        }

        if (otherBranches.isNotEmpty()) {
            println("Pending branches:")
            for ((branch, _) in otherBranches) {
                println("  $branch")
            }
            println()
        }

        if (rebaseFailedBranches.isNotEmpty()) {
            println("Rebase failed for branches:")
            for ((branch, _) in rebaseFailedBranches) {
                println("  $branch")
            }
            println()
        }
    }
}
