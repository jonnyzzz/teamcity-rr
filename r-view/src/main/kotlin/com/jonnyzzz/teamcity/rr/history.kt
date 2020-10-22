package com.jonnyzzz.teamcity.rr

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.util.*

class TheHistory {
    private val om = jacksonObjectMapper()
    private val cacheLocation by lazy { DiskCaches.branchesCacheDir }
    private val rebaseFailed by lazy { cacheLocation / "rebase-failed-commits.txt"}

    private val brokenForRebase by lazy {
        val newSet = runCatching { rebaseFailed.readText() }.getOrElse { "" }.split("\n")
        TreeSet(newSet)
    }

    fun isBrokenForRebase(commitId: String) = commitId in brokenForRebase

    @Synchronized
    fun logRebaseFailed(commitId: String) {
        if (brokenForRebase.add(commitId)) {
            rebaseFailed.parentFile?.mkdirs()
            rebaseFailed.writeText(brokenForRebase.joinToString("\n"))
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
