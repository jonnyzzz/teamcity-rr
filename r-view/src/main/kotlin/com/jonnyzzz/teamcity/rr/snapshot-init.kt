package com.jonnyzzz.teamcity.rr

import java.util.*


const val defaultBranchPrefix = "refs/heads/jonnyzzz/"                  //TODO: configuration?
const val defaultRemoteBranchPrefix = "refs/remotes/origin/jonnyzzz/"   //TODO: configuration?
const val defaultFetchSpec = "$defaultBranchPrefix*:$defaultRemoteBranchPrefix*"

fun computeLightSnapshot(defaultGit: GitRunner): LightSnapshot {
    return LightSnapshot(
            masterCommit = defaultGit.gitHeadCommit("origin/master"),
            headCommit = defaultGit.gitHeadCommit("HEAD"),
            headBranch = defaultGit.listGitCurrentBranchName().removePrefix("refs/heads/"),
    )
}

class RepositoryBranchInfo(
    val branch: String,
    val commit: String,
)

fun computeRawSnapshot(
        defaultGit: GitRunner,
        history: TheHistory,
): GitSnapshot {
    printProgress("Checking current status...")
    println()

    val masterCommit = computeSnapshotMasterCommit(defaultGit)
    val recentMasterCommits = computeSnapshotRecentMasterCommits(defaultGit, masterCommit = masterCommit)
    val branches = computeSnapshotBranches(defaultGit)

    val alreadyMergedBranches = TreeMap<String, String>()
    val rebaseFailedBranches = TreeMap<String, String>()
    val pendingBranches = TreeMap<String, String>()

    for (branchInfo in branches) {
        val commit = branchInfo.commit
        val branch = branchInfo.branch

        if (commit == masterCommit) {
            alreadyMergedBranches += branch to commit
            continue
        }

        if (history.isBrokenForRebase(commit, branch)) {
            rebaseFailedBranches += branch to commit
            continue
        }

        pendingBranches += branch to commit
    }

    printProgress("Collected ${alreadyMergedBranches.size + rebaseFailedBranches.size + pendingBranches.size} local Git branches with $defaultBranchPrefix")
    println()

    val lightSnapshot = computeLightSnapshot(defaultGit)
    require(masterCommit == lightSnapshot.masterCommit) {
        "Unexpected change of origin/master: $masterCommit != $lightSnapshot"
    }

    val headToMasterCommits = when {
        lightSnapshot.headCommit == lightSnapshot.masterCommit -> listOf()
        else -> defaultGit.listGitCommitsEx(lightSnapshot.headBranch, notIn = masterCommit)
    }

    return GitSnapshot(
            lightSnapshot = lightSnapshot,
            headToMasterCommits = headToMasterCommits,
            masterCommits = recentMasterCommits,
            alreadyMergedBranches = alreadyMergedBranches,
            rebaseFailedBranches = rebaseFailedBranches,
            pendingBranches = pendingBranches
    )
}
