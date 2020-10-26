package com.jonnyzzz.teamcity.rr.commands

import com.jonnyzzz.teamcity.rr.UserErrorException
import com.jonnyzzz.teamcity.rr.WithInheritSuccessfully
import com.jonnyzzz.teamcity.rr.branchPrefix
import com.jonnyzzz.teamcity.rr.printWithHighlighting
import java.time.Duration

object NewBranchCommand : SnapshotCommandBase() {
    override fun Session.doTheCommandImpl() {
        val branchName = args.singleOrNull { !it.startsWith("-") }
                ?: throw UserErrorException("Branch is not specified in arguments")

        val fullBranchName = "$branchPrefix/$branchName"

        defaultGit.execGit(WithInheritSuccessfully, Duration.ofMinutes(5), command = "stash")
        defaultGit.execGit(WithInheritSuccessfully, Duration.ofMinutes(5),
                command = "checkout", args = listOf("-b", fullBranchName, snapshot.masterCommit))

        println()
        printWithHighlighting {
            "Switched to " + bold(fullBranchName) + " from " + snapshot.headBranch
        }

        history.invalidateSnapshot()
    }
}
