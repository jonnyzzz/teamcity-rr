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
        println("Rebase failed/disabled for branches:")
        for ((branch, _) in rebaseFailedBranches) {
            println("  $branch")
        }
        println()
    }

    printWithHighlighting {
        "Current branch: " + bold(headBranch) + " " +
                when {
                    headToMasterCommits.isEmpty() -> "no new commits"
                    else -> "" + headToMasterCommits.size + " unique changes"
                } + "\n\n"
    }

    println("Use `r-view rebase <branch> disable` to remove unnecessary branches from regular rebasing")
}

private fun GitSnapshot.formatBranchWithInfo(branch: String): String {
    return buildString {
        append("  ")
        append(branch.padEnd(39))

        val uniqueCommits = branchToUniqueCommits[branch]
        if (uniqueCommits != null) {
            append("${uniqueCommits.size} unique commits".padStart(17))
        } else {
            append("".padStart(20))
        }

        val safePushes = branchToSafePushes[branch]
        if (safePushes != null && safePushes.isNotEmpty()) {
            append("  " + safePushes.size + " safe pushes")
        }
    }
}
