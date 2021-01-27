package com.jonnyzzz.teamcity.rr

fun GitSnapshot.showSnapshot() {
    println()

    if (rebaseFailedBranches.isNotEmpty()) {
        val text = buildString {
            appendLine("Rebase failed/disabled for branches:")
            for ((branch, _) in rebaseFailedBranches) {
                appendLine("  $branch")
            }
            appendLine()
        }
        printWithHighlighting { dim(text) }
    }

    println()
    println()
    println()

    if (pendingBranches.isNotEmpty()) {
        printWithHighlighting {
            yellow(bold(underline("Pending branches:")))
        }

        for ((branch, _) in pendingBranches) {
            println(formatBranchWithInfo(branch))
        }
        println()
        println()
    }

    if (alreadyMergedBranches.isNotEmpty()) {
        printWithHighlighting {
            green(bold(underline("Already completed and merged branches:")))
        }
        println()
        for ((branch, _) in alreadyMergedBranches) {
            println(formatBranchWithInfo(branch))
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

        val cellSize = 20
        val uniqueCommits = branchToUniqueCommits[branch]
        if (uniqueCommits != null && uniqueCommits.isNotEmpty()) {
            val text = "${uniqueCommits.size} unique commits"
            append(" ".repeat((cellSize - text.length).coerceAtLeast(0)))

            if (supportsLinks && uniqueCommits.all { it.commitId in masterCommits }) {
                append(formatLinkIfSupported(generateSpaceCommitsLink(uniqueCommits), text))
            } else {
                append(text)
            }
        } else {
            append("".padStart(20))
        }

        val safePushes = branchToSafePushes[branch]
        if (safePushes != null && safePushes.isNotEmpty()) {
            append("  ")
            append("${safePushes.size} safe pushes")
            if (supportsLinks) {
                val lastPush = safePushes.last()
                append(": ")
                append(formatLinkIfSupported(lastPush.formatTeamCityLink(), "last push on TeamCity"))
            }
        }
    }
}
