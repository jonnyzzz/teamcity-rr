package com.jonnyzzz.teamcity.rr

import java.time.Duration
import java.util.*


private const val defaultBranchPrefix = "refs/heads/jonnyzzz/"  //TODO: configuration?

fun computeLightSnapshot(defaultGit: GitRunner): LightSnapshot {
    return LightSnapshot(
            masterCommit = defaultGit.gitHeadCommit("origin/master"),
            headCommit = defaultGit.gitHeadCommit("HEAD"),
            headBranch = defaultGit.listGitCurrentBranchName("HEAD").removePrefix("refs/heads/"),
    )
}

fun computeCurrentStatus(
        lightSnapshot: LightSnapshot,
        defaultGit: GitRunner,
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
                        "--prune", "--no-tags", "--keep",
                        "origin",
                        "refs/heads/master:refs/remotes/origin/master",
//                            "$defaultSafePushBranchPrefix/*:$defaultLocalPushBranchPrefix/*"
                ))
    }

    val recentCommits = defaultGit.listGitCommitsEx("origin/master", commits = 2048)
            .associateBy { it.commitId }

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

        val rebaseResult = defaultGit.gitRebase(branch = branch, toHead = lightSnapshot.masterCommit)
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

    val headToMasterCommits = when {
        lightSnapshot.headCommit == lightSnapshot.masterCommit -> listOf()
        else -> defaultGit.listGitCommitsEx(lightSnapshot.headBranch, notIn = "origin/master")
    }

    return GitSnapshot(
            lightSnapshot = lightSnapshot,
            headToMasterCommits = headToMasterCommits,
            masterCommits = recentCommits,
            alreadyMergedBranches = alreadyMergedBranches,
            rebaseFailedBranches = rebaseFailedBranches,
            pendingBranches = pendingBranches
    )
}
