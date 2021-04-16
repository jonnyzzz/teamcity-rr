package com.jonnyzzz.teamcity.rr.commands

import com.jonnyzzz.teamcity.rr.UserErrorException
import com.jonnyzzz.teamcity.rr.printFinalMessage

object ToggleRebaseMode : SnapshotCacheCommandBase() {
    override fun Session.doTheCommandImpl() {
        when {
            "disable" in args -> {
                val (branch, commit) = getBranchFromArgs(snapshot.allBranches)
                history.logRebaseFailed(commit, branch)
                printFinalMessage("Branch $branch is disabled for rebase")
            }
            "enable" in args -> {
                val (branch, commit) = getBranchFromArgs(snapshot.allBranches)
                history.removeRebaseFailed(commit, branch)
                printFinalMessage("Branch $branch is enabled for rebase")
            }
            else -> throw UserErrorException("Failed to select rebase mode from args")
        }

        history.invalidateSnapshot()
    }
}
