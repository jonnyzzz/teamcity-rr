package com.jonnyzzz.teamcity.rr

import java.util.*

class GitBranchHistory {
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

}

