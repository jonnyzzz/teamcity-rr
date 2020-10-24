package com.jonnyzzz.teamcity.rr

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.util.*

data class SafePushBranchInfo(
        val branch: String,
        val safePushBranch: String,
        val commitId: String,
        val mode: SafePushMode,
        val created: Date = Date()
) {
    fun formatTeamCityLink(): String = mode.teamcityBranchLinkFromBranch(safePushBranch)
}

private inline fun <reified Y> typeRef() = object : TypeReference<Y>() {}

class TheHistory {
    private val om = jacksonObjectMapper()
    private val cacheLocation by lazy { DiskCaches.branchesCacheDir }
    private val rebaseFailedFile by lazy { cacheLocation / "rebase-failed-commits.txt" }
    private val snapshotFile by lazy { cacheLocation / "state.json" }
    private fun branchForCommitsFile(branch: String) = cacheLocation / "branch-commits-" + branch.replace(Regex("[^\\w\\d\\-]+"), "-") + ".txt"
    private fun branchForSafePushesFile(branch: String) = cacheLocation / "branch-safe-push-" + branch.replace(Regex("[^\\w\\d\\-]+"), "-") + ".txt"

    private val brokenForRebase by lazy {
        runCatching { rebaseFailedFile.readText() }.getOrElse { "" }
                .splitToSequence("\n")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .toCollection(TreeSet())
    }

    fun addSafePushBranch(info: SafePushBranchInfo) {
        val newBranches = (getSafePushBranches(info.branch) + info).distinct()
        val path = branchForSafePushesFile(info.branch)
        path.parentFile?.mkdirs()
        om.writerWithDefaultPrettyPrinter().writeValue(path, newBranches)
    }

    fun getSafePushBranches(branch: String) : List<SafePushBranchInfo> {
        val path = branchForSafePushesFile(branch)
        if (!path.isFile) return listOf()
        return try {
            om.readValue(path, typeRef())
        } catch (t: Throwable) {
            listOf()
        }
    }

    fun saveSnapshot(snapshot: GitSnapshot) {
        snapshotFile.parentFile?.mkdirs()
        om.writerWithDefaultPrettyPrinter().writeValue(snapshotFile, snapshot)
    }

    fun invalidateSnapshot() {
        snapshotFile.delete()
    }

    fun loadSnapshot() : GitSnapshot? {
        if (!snapshotFile.isFile) return null
        return try {
            om.readValue(snapshotFile, GitSnapshot::class.java)
        } catch (t: Throwable) {
            snapshotFile.delete()
            null
        }
    }

    @Synchronized
    fun isBrokenForRebase(commitId: String) = commitId in brokenForRebase

    @Synchronized
    fun logRebaseFailed(commitId: String) {
        if (brokenForRebase.add(commitId)) {
            rebaseFailedFile.parentFile?.mkdirs()
            rebaseFailedFile.writeText(brokenForRebase.joinToString("\n"))
        }
    }

    @Synchronized
    fun removeRebaseFailed(commitId: String) {
        if (brokenForRebase.remove(commitId)) {
            rebaseFailedFile.parentFile?.mkdirs()
            rebaseFailedFile.writeText(brokenForRebase.joinToString("\n"))
        }
    }

    fun updateCommitsFor(branch: String, uniqueCommits: List<CommitInfo>) {
        val branchFile = branchForCommitsFile(branch)
        if (uniqueCommits.isEmpty()) {
            branchFile.delete()
            return
        }

        branchFile.parentFile?.mkdirs()
        om.writerWithDefaultPrettyPrinter().writeValue(branchFile, uniqueCommits)
    }

    fun lookupCommitsFor(branch: String): List<CommitInfo> {
        val branchFile = branchForCommitsFile(branch)
        if (!branchFile.isFile) return listOf()
        return try {
            om.readValue(branchFile, typeRef())
        } catch (t: Throwable) {
            branchFile.delete()
            listOf()
        }
    }

    fun branchRemoved(branch: String, commit: String) {
        branchForCommitsFile(branch).delete()
        removeRebaseFailed(commit)
        branchForSafePushesFile(branch).delete()
    }
}
