package com.jonnyzzz.teamcity.rr

import com.fasterxml.jackson.annotation.JsonIgnore
import java.util.*

data class LightSnapshot(
        val masterCommit: String,
        val headBranch: String,
        val headCommit: String,
)

data class GitSnapshot(
        val lightSnapshot: LightSnapshot,
        val headToMasterCommits : List<CommitInfo>,

        val alreadyMergedBranches: Map<String, String>,
        val rebaseFailedBranches: Map<String, String>,
        val pendingBranches: Map<String, String>,

        val branchToUniqueCommits: Map<String, List<CommitInfo>> = mapOf(),
        val branchToSafePushes : Map<String, List<SafePushBranchInfo>> = mapOf(),

        val created: Date = Date(),
        val masterCommits: Map<String, CommitInfo>,
) {

    fun hasSameLight(snapshot: LightSnapshot) = this.lightSnapshot == snapshot

    @get:JsonIgnore
    val masterCommit by lightSnapshot::masterCommit

    @get:JsonIgnore
    val headCommit by lightSnapshot::headCommit

    @get:JsonIgnore
    val headBranch by lightSnapshot::headBranch

    @get:JsonIgnore
    val allBranches : Map<String, String> = (alreadyMergedBranches + rebaseFailedBranches + pendingBranches).toSortedMap()

    @get:JsonIgnore
    val allBranchNames : Set<String> = allBranches.keys.toSortedSet()

    @get:JsonIgnore
    val masterCommitInfos: Map<CommitInfo, CommitInfo> by lazy { masterCommits.values.toHashSet().associateBy { it } }
}
