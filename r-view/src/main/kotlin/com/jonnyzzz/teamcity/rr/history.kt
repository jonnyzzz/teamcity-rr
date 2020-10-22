package com.jonnyzzz.teamcity.rr

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.util.*

class TheHistory {
    private val om = jacksonObjectMapper()
    private val cacheLocation by lazy { DiskCaches.branchesCacheDir }
    private val rebaseFailedFile by lazy { cacheLocation / "rebase-failed-commits.txt"}

    private val brokenForRebase by lazy {
        runCatching { rebaseFailedFile.readText() }.getOrElse { "" }
                .splitToSequence("\n")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .toCollection(TreeSet())
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
        val branchFile = cacheLocation / "branch-" + branch.sha256()
        if (uniqueCommits.isEmpty()) {
            branchFile.delete()
            return
        }

        val text = om.writerWithDefaultPrettyPrinter().writeValueAsString(uniqueCommits)
        branchFile.parentFile?.mkdirs()
        branchFile.writeText(text)
    }

    fun lookupCommitsFor(branch: String) : List<CommitInfo> {
        val branchFile = cacheLocation / "branch-" + branch.sha256()
        if (!branchFile.isFile) return listOf()

        return try {
            om.readValue(branch, object: TypeReference<List<CommitInfo>>(){})
        } catch (t: Throwable) {
            branchFile.delete()
            listOf()
        }
    }
}
