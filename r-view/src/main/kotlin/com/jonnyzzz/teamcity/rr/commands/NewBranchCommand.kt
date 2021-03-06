package com.jonnyzzz.teamcity.rr.commands

import com.jonnyzzz.teamcity.rr.*
import java.time.Duration

object NewBranchCommand : SnapshotCacheCommandBase() {
    override fun Session.doTheCommandImpl() {
        val branchName = args.singleOrNull { !it.startsWith("-") }
                ?: throw UserErrorException("Branch is not specified in arguments")

        val fullBranchName = branchName.removePrefix("refs/heads/")
        val defaultPrefix = defaultBranchPrefix.removePrefix("refs/heads/")

        if (!fullBranchName.startsWith(defaultPrefix)) {
            throw UserErrorException("Branch $branchName must start with $defaultBranchPrefix")
        }

        defaultGit.execGit(WithInheritSuccessfully, Duration.ofMinutes(15), command = "stash")
        defaultGit.execGit(WithInheritSuccessfully, Duration.ofMinutes(5),
                command = "checkout", args = listOf("-b", fullBranchName, snapshot.masterCommit))

        println()
        printWithHighlighting {
            "Switched to " + bold(fullBranchName) + " from " + snapshot.headBranch
        }

        history.invalidateSnapshot()
    }
}
