package com.jonnyzzz.teamcity.rr

fun GitSnapshot.showSnapshot() {
    println()
    if (alreadyMergedBranches.isNotEmpty()) {
        println("Already completed and merged branches:")
        for ((branch, _) in alreadyMergedBranches) {
            println(formatBranchWithInfo(branch))
        }
        println()
    }

    if (pendingBranches.isNotEmpty()) {
        println("Pending branches:")
        for ((branch, _) in pendingBranches) {
            println(formatBranchWithInfo(branch))
        }
        println()
    }

    if (rebaseFailedBranches.isNotEmpty()) {
        println("Rebase failed for branches:")
        for ((branch, _) in rebaseFailedBranches) {
            println("  $branch")
        }
        println()
    }
}

private fun GitSnapshot.formatBranchWithInfo(branch: String): String {
    return buildString {
        append("  ")
        append(branch.padEnd(39))
        val uniqueCommits = get(GitLogKey)?.get(branch)
        if (uniqueCommits != null) {
            append(" ")
            append(uniqueCommits.size)
            append(" unique commits")
        }
    }
}

