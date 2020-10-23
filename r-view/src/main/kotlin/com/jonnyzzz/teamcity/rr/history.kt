package com.jonnyzzz.teamcity.rr

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.util.*

class TheHistory {
    private val om = jacksonObjectMapper()
    private val cacheLocation by lazy { DiskCaches.branchesCacheDir }
    private val rebaseFailedFile by lazy { cacheLocation / "rebase-failed-commits.txt" }
    private val snapshotFile by lazy { cacheLocation / "state.json" }
    private fun branchForCommitsFile(branch: String) = cacheLocation / "branch-commits" + branch.replace(Regex("[^\\w\\d\\-]+"), "-") + ".txt"

    private val brokenForRebase by lazy {
        runCatching { rebaseFailedFile.readText() }.getOrElse { "" }
                .splitToSequence("\n")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .toCollection(TreeSet())
    }

    fun saveSnapshot(snapshot: GitSnapshot) {
        snapshotFile.parentFile?.mkdirs()
        om.writerWithDefaultPrettyPrinter().writeValue(snapshotFile, snapshot)
    }

    fun loadSnapshot() : GitSnapshot? {
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
        return try {
            om.readValue(branch, object : TypeReference<List<CommitInfo>>() {})
        } catch (t: Throwable) {
            branchFile.delete()
            listOf()
        }
    }
}
