package com.jonnyzzz.teamcity.rr.commands

import com.jonnyzzz.teamcity.rr.GitSnapshot
import com.jonnyzzz.teamcity.rr.UserErrorException
import com.jonnyzzz.teamcity.rr.printFinalMessage

object ToggleRebaseMode : CommandBase() {
    override fun runRebase(args: List<String>): Boolean = false
    override fun runFetch(args: List<String>): Boolean = false

    override fun Session.doTheCommandImpl() {
        when {
            "disable" in args -> {
                val (branch, commit) = getBranchFromArgs(snapshot.pendingBranches)
                history.logRebaseFailed(commit)
                printFinalMessage("Branch $branch is disabled for rebase")
            }
            "enable" in args -> {
                val (branch, commit) = getBranchFromArgs(snapshot.rebaseFailedBranches)
                history.removeRebaseFailed(commit)
                printFinalMessage("Branch $branch is enabled for rebase")
            }
            else -> throw UserErrorException("Failed to select rebase mode from args")
        }

        invalidateSnapshot()
    }
}
