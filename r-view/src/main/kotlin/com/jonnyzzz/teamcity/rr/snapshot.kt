package com.jonnyzzz.teamcity.rr

import com.fasterxml.jackson.annotation.JsonIgnore

data class GitSnapshot(
        val masterCommits: Map<String, CommitInfo>,
        val headCommit: String,

        val alreadyMergedBranches: Map<String, String>,
        val rebaseFailedBranches: Map<String, String>,
        val pendingBranches: Map<String, String>,

        val branchToUniqueCommits: Map<String, List<CommitInfo>> = mapOf(),
) {
    @JsonIgnore
    val masterCommitInfos : Set<CommitInfo> = masterCommits.values.toHashSet()
}
