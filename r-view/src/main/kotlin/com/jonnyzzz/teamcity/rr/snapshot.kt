package com.jonnyzzz.teamcity.rr

import com.fasterxml.jackson.annotation.JsonIgnore
import java.util.*

data class LightSnapshot(
        val masterCommit: String,
        val headBranch: String,
        val headCommit: String,
)

data class GitSnapshot(
        val masterCommits: Map<String, CommitInfo>,
        private val lightSnapshot: LightSnapshot,
        val headToMasterCommits : List<CommitInfo>,

        val alreadyMergedBranches: Map<String, String>,
        val rebaseFailedBranches: Map<String, String>,
        val pendingBranches: Map<String, String>,

        val branchToUniqueCommits: Map<String, List<CommitInfo>> = mapOf(),
        val branchToSafePushes : Map<String, List<SafePushBranchInfo>> = mapOf(),

        val created: Date = Date(),
) {

    fun hasSameLight(snapshot: LightSnapshot) = this.lightSnapshot == lightSnapshot

    @get:JsonIgnore
    val masterCommit by lightSnapshot::masterCommit

    @get:JsonIgnore
    val headCommit by lightSnapshot::headCommit

    @get:JsonIgnore
    val headBranch by lightSnapshot::headBranch

    @get:JsonIgnore
    val allBranchNames : Set<String> = listOf(alreadyMergedBranches.keys, rebaseFailedBranches.keys, pendingBranches.keys).flatMapTo(TreeSet()) {it}

    @get:JsonIgnore
    val masterCommitInfos: Map<CommitInfo, CommitInfo> by lazy { masterCommits.values.toHashSet().associateBy { it } }
}
