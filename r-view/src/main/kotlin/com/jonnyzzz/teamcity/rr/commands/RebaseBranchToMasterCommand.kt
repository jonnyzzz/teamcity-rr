package com.jonnyzzz.teamcity.rr.commands

import com.jonnyzzz.teamcity.rr.SnapshotRebaseDriver
import com.jonnyzzz.teamcity.rr.UserErrorException
import com.jonnyzzz.teamcity.rr.computeSnapshotFetch
import com.jonnyzzz.teamcity.rr.printWithHighlighting

object RebaseBranchToMasterCommand : CommandBase() {
    override fun doTheCommand(args: List<String>) {
        val (branch, commit) = Session(args).run { getBranchFromArgs(snapshot.allBranches) }

        computeSnapshotFetch(defaultGit)
        val rebaseResult = SnapshotRebaseDriver(defaultGit, history).rebaseBranch(branch, commit)
        if (rebaseResult == null) {
            printWithHighlighting { bold("Rebase failed. Push will not run.") }
            throw UserErrorException("Rebase failed for branch $branch")
        }
        history.invalidateSnapshot()
    }
}
