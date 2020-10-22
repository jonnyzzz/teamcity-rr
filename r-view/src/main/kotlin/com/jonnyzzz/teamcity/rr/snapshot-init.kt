package com.jonnyzzz.teamcity.rr

import java.time.Duration
import java.util.*


private val safePushSuffix = "safepush/Eugene.Petrenko"
val defaultSafePushBranchPrefix = "refs/$safePushSuffix"
private val defaultLocalPushBranchPrefix = "origin/safepush"

private val defaultBranchPrefix = "refs/heads/jonnyzzz/"  //TODO: configuration?

fun computeCurrentStatus(defaultGit: GitRunner,
                         history: TheHistory,
                         runFetch: Boolean,
                         doRebase: Boolean,
                         ): GitSnapshot {
    printProgress("Checking current status...")
    println()

    if (runFetch) {
        printProgress("Fetching changes from remote...")
        defaultGit.execGit(WithInheritSuccessfully, timeout = Duration.ofMinutes(10),
                command = "fetch",
                args = listOf(
                        "--prune", "origin",
                        "refs/heads/master:origin/master",
//                            "$defaultSafePushBranchPrefix/*:$defaultLocalPushBranchPrefix/*"
                ))
    }

    val recentCommits = defaultGit.listGitCommitsEx("origin/master", commits = 2048)
            .associateBy { it.commitId }

    val headCommit = defaultGit.gitHeadCommit("origin/master")

    val alreadyMergedBranches = TreeMap<String, String>()
    val rebaseFailedBranches = TreeMap<String, String>()
    val pendingBranches = TreeMap<String, String>()

    for ((fullBranchName, commit) in defaultGit.listGitBranches().toSortedMap()) {
        if (!fullBranchName.startsWith(defaultBranchPrefix)) continue
        val branch = fullBranchName.removePrefix("refs/heads/")

        if (commit in recentCommits) {
            alreadyMergedBranches += branch to commit
            continue
        }

        if (history.isBrokenForRebase(commit)) {
            rebaseFailedBranches += branch to commit
            continue
        }

        if (!doRebase) {
            pendingBranches += branch to commit
            continue
        }

        val rebaseResult = defaultGit.gitRebase(branch = branch, toHead = headCommit)
        if (rebaseResult == null) {
            history.logRebaseFailed(commit)
            rebaseFailedBranches += branch to commit
            continue
        }

        val newCommitId = rebaseResult.newCommitId
        if (newCommitId in recentCommits) {
            alreadyMergedBranches += branch to newCommitId
            continue
        }

        pendingBranches += branch to newCommitId
    }

    printProgress("Collected ${alreadyMergedBranches.size + rebaseFailedBranches.size + pendingBranches.size} local Git branches with $defaultBranchPrefix")
    println()

    return GitSnapshot(
            masterCommits = recentCommits,
            headCommit = headCommit,
            alreadyMergedBranches = alreadyMergedBranches,
            rebaseFailedBranches = rebaseFailedBranches,
            pendingBranches = pendingBranches
    )
}
