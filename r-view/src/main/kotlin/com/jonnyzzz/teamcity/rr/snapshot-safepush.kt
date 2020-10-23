package com.jonnyzzz.teamcity.rr

import java.util.*


fun collectSafePushInfoForBranches(defaultGit: GitRunner,
                                   history: TheHistory,
                                   snapshot: GitSnapshot): GitSnapshot {
    val safePushInfo = TreeMap<String, List<SafePushBranchInfo>>()

    for (branch in snapshot.allBranchNames) {
        val h = history.getSafePushBranches(branch)
        if (h.isNotEmpty()) {
            safePushInfo += branch to h.toList()
        }
    }

    return snapshot.copy(branchToSafePushes = safePushInfo)
}
