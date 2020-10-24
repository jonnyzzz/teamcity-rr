package com.jonnyzzz.teamcity.rr.commands

import com.jonnyzzz.teamcity.rr.*
import com.jonnyzzz.teamcity.rr.commands.CommandBase.Session

object UpdateCommand : CommandBase() {
    override fun Session.doTheCommandImpl() {
        showCommand()
    }
}

object ShowCommand : SnapshotCommandBase() {
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
    printWithHighlighting {
        "Branch: " + bold(selectedBranch) + " " +
                when {
                    commits.isEmpty() -> "no new commits"
                    else -> "" + commits.size + " unique changes"
                } + "\n\n" +
                generateSpaceCommitsLink(commits)
    }
}
