package com.jonnyzzz.teamcity.rr.commands

import com.jonnyzzz.teamcity.rr.*
import java.time.Duration

object SyncRemoteBranches : CommandBase() {
    override fun doTheCommand(args: List<String>) {
        computeSnapshotFetch(defaultGit, defaultFetchSpec)

        val localBranches = defaultGit.listGitBranches().filterPersonalBranches()
        val remoteBranches = defaultGit.listGitRemoteBranches().filterPersonalRemoteBranches()

        val branchesToProcess = remoteBranches - localBranches

        if (branchesToProcess.isNotEmpty()) {
            history.invalidateSnapshot()
        }

        for (toCheckout in branchesToProcess) {
            println("Creating new branch: $toCheckout")
            defaultGit.execGit(
                WithInherit, Duration.ofMinutes(10),
                command = "branch",
                args = listOf(toCheckout, "origin/$toCheckout")
            )
        }

        UpdateCommand.doTheCommand(listOf())
    }
}
