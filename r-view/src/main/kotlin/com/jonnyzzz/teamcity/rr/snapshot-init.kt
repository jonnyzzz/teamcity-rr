package com.jonnyzzz.teamcity.rr

import java.time.Duration
import java.util.*


const val defaultBranchPrefix = "refs/heads/jonnyzzz/"  //TODO: configuration?

fun computeLightSnapshot(defaultGit: GitRunner): LightSnapshot {
    return LightSnapshot(
            masterCommit = defaultGit.gitHeadCommit("origin/master"),
            headCommit = defaultGit.gitHeadCommit("HEAD"),
            headBranch = defaultGit.listGitCurrentBranchName("HEAD").removePrefix("refs/heads/"),
    )
}

class RepositoryBranchInfo(
    val branch: String,
    val commit: String,
)

fun computeCurrentStatus(
        defaultGit: GitRunner,
        history: TheHistory,
        runFetch: Boolean,
        doRebase: Boolean,
): GitSnapshot {
    printProgress("Checking current status...")
    println()

    val masterCommit by lazy { defaultGit.gitHeadCommit("origin/master") }

    val branches by lazy {
        defaultGit.listGitBranches()
                .filter { it.startsWith(defaultBranchPrefix) }
                //TODO: use defaultBranchPrefix as the prefix here (it may break other logic)
                .map { it.removePrefix("refs/heads/") }
                .toSortedSet()
                .map { branch ->
                    val commitId = defaultGit.gitHeadCommit(branch)
                    RepositoryBranchInfo(
                        commit = commitId,
                        branch = branch,
                    )
                }
    }

    val recentMasterCommits: Map<String, CommitInfo> by lazy {
        defaultGit
                .listGitCommitsEx(masterCommit, commits = 2048)
                .associateBy { it.commitId }
    }


    if (runFetch) {
        printProgress("Fetching changes from remote...")
        defaultGit.execGit(WithInheritSuccessfully, timeout = Duration.ofMinutes(10),
                command = "fetch",
                args = listOf(
                        "--prune", "--no-tags", "--keep",
                        "origin",
                        "refs/heads/master:refs/remotes/origin/master",
                ))

        return computeCurrentStatus(
                defaultGit = defaultGit,
                history = history,
                runFetch = false,
                doRebase = doRebase
        )
    }

    if (doRebase) {
        var didRebase = false

        for (branchInfo in branches) {
            val commit = branchInfo.commit
            val branch = branchInfo.branch

            if (history.isBrokenForRebase(commit, branch)) continue

            val maxDistance = 128
            val branchCommits = defaultGit.listGitCommits(commit, commits = maxDistance + 12)
            val distanceToMaster = branchCommits.withIndex().firstOrNull { (_, commit: String) -> commit in recentMasterCommits }?.index

            if (distanceToMaster == null || distanceToMaster > maxDistance) {
                println("Branch $branch is more than $maxDistance commits away from the `master`: $distanceToMaster. Automatic rebase will not run.")
                continue
            }

            didRebase = true
            printWithHighlighting { "Rebasing " + bold(branch) + "..." }

            val rebaseResult = defaultGit.gitRebase(
                    branch = branch,
                    toHead = masterCommit,
                    isIncludedInHead = { it in recentMasterCommits }
            )

            if (rebaseResult == null) {
                history.logRebaseFailed(commit, branch = null)
            }
        }

        if (didRebase) {
            history.invalidateSnapshot()
        }

        return computeCurrentStatus(
                defaultGit = defaultGit,
                history = history,
                runFetch = runFetch,
                doRebase = false
        )
    }

    return computeCurrentStatusStatic(defaultGit, history, masterCommit, branches, recentMasterCommits)
}

private fun computeCurrentStatusStatic(defaultGit: GitRunner,
                                       history: TheHistory,
                                       masterCommit: String,
                                       branches: List<RepositoryBranchInfo>,
                                       recentMasterCommits: Map<String, CommitInfo>): GitSnapshot {

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
