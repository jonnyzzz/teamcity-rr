package com.jonnyzzz.teamcity.rr

import java.time.Duration
import java.util.*


const val branchPrefix = "jonnyzzz"
private const val defaultBranchPrefix = "refs/heads/$branchPrefix/"  //TODO: configuration?

fun computeLightSnapshot(defaultGit: GitRunner): LightSnapshot {
    return LightSnapshot(
            masterCommit = defaultGit.gitHeadCommit("origin/master"),
            headCommit = defaultGit.gitHeadCommit("HEAD"),
            headBranch = defaultGit.listGitCurrentBranchName("HEAD").removePrefix("refs/heads/"),
    )
}

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
                .associateWith { defaultGit.gitHeadCommit(it) }
                .toSortedMap()
    }

    val recentMasterCommits by lazy {
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

        for ((branch, commit) in branches) {
            if (history.isBrokenForRebase(commit)) continue

            didRebase = true
            printWithHighlighting { "Rebasing " + bold(branch) + "..." }

            val rebaseResult = defaultGit.gitRebase(
                    branch = branch,
                    toHead = masterCommit,
                    isIncludedInHead = { it in recentMasterCommits }
            )

            if (rebaseResult == null) {
                history.logRebaseFailed(commit)
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
                                       branches: Map<String, String>,
                                       recentMasterCommits: Map<String, CommitInfo>): GitSnapshot {

    val alreadyMergedBranches = TreeMap<String, String>()
    val rebaseFailedBranches = TreeMap<String, String>()
    val pendingBranches = TreeMap<String, String>()

    for ((branch, commit) in branches) {
        if (commit == masterCommit) {
            alreadyMergedBranches += branch to commit
            continue
        }

        if (history.isBrokenForRebase(commit)) {
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
