package com.jonnyzzz.teamcity.rr

import java.util.*

fun collectChangesForPendingBranches(defaultGit: GitRunner,
                                     history: TheHistory,
                                     snapshot: GitSnapshot): GitSnapshot {
    val branches = TreeMap<String, List<CommitInfo>>()
    for ((branch, _) in snapshot.pendingBranches) {
        val uniqueCommits = defaultGit.listGitCommitsEx(branch, notIn = "origin/master")
        history.updateCommitsFor(branch, uniqueCommits)
        branches[branch] = uniqueCommits
    }

    for ((branch, _) in snapshot.alreadyMergedBranches) {
        val uniqueCommits = history.lookupCommitsFor(branch)
                .filter { it in snapshot.masterCommitInfos }

        if (uniqueCommits.isNotEmpty()) {
            branches[branch] = uniqueCommits
        }
    }

    return snapshot.copy(branchToUniqueCommits = branches)
}
