package com.jonnyzzz.teamcity.rr

import com.fasterxml.jackson.annotation.JsonIgnore
import java.util.*

data class GitSnapshot(
        val masterCommits: Map<String, CommitInfo>,
        val headCommit: String,

        val alreadyMergedBranches: Map<String, String>,
        val rebaseFailedBranches: Map<String, String>,
        val pendingBranches: Map<String, String>,

        val branchToUniqueCommits: Map<String, List<CommitInfo>> = mapOf(),

        val created: Date = Date(),
) {
    @get:JsonIgnore
    val masterCommitInfos: Map<CommitInfo, CommitInfo> by lazy { masterCommits.values.toHashSet().associateBy { it } }
}
