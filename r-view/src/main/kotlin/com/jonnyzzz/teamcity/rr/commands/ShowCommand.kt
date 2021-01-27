package com.jonnyzzz.teamcity.rr.commands

import com.jonnyzzz.teamcity.rr.*
import com.jonnyzzz.teamcity.rr.commands.CommandBase.Session

object UpdateCommand : SnapshotUpdatingCommandBase() {
    override fun Session.doTheCommandImpl() {
        showCommand()
    }
}

object ShowCommand : SnapshotCacheCommandBase() {
    override fun Session.doTheCommandImpl() {
        showCommand()
    }
}

private fun Session.showCommand() {
    val selectedBranch = findBranchFromArgs(snapshot.allBranches)?.first

    if (selectedBranch == null) {
        snapshot.showSnapshot()
        return
    }

    return showSelectedBranch(selectedBranch)
}

private fun Session.showSelectedBranch(selectedBranch: String) {
    val commits = snapshot.branchToUniqueCommits[selectedBranch] ?: listOf()
    val lastPush = snapshot.branchToSafePushes[selectedBranch]?.lastOrNull()
    printWithHighlighting {
        "Branch: " + bold(selectedBranch) + " " +
                when {
                    commits.isEmpty() -> "no new commits"
                    else -> "" + commits.size + " unique changes"
                } + "\n\n" +

                run {
                    if (commits.all { it.commitId in snapshot.masterCommits }) {
                        generateSpaceCommitsLink(commits)
                    } else ""
                } +

                run {
                    if (lastPush != null) {
                        "\n\nLast safe-pushed (${lastPush.mode}) on " +
                                underline("${lastPush.created}") +
                                "\n\n" +
                                lastPush.formatTeamCityLink()
                    } else ""
                } + "\n\n"
    }
}
