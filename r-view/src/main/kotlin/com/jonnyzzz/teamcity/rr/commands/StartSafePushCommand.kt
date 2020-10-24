package com.jonnyzzz.teamcity.rr.commands

import com.jonnyzzz.teamcity.rr.*
import java.time.Duration

object StartSafePushCommand : SnapshotCommandBase() {
    override fun Session.doTheCommandImpl() {
        val (branch, commit) = getBranchFromArgs(snapshot.pendingBranches)

        val mode = when {
            "all" in args -> SafePushMode.ALL
            "compile" in args -> SafePushMode.COMPILE
            else -> null
        }

        run {
            val gitHeadCommit = defaultGit.gitHeadCommit(branch)
            if (commit != gitHeadCommit) {
                history.invalidateSnapshot()
                throw UserErrorException("Invalid state for branch $branch. " +
                        "We assume it was at $commit but it is actually at $gitHeadCommit"
                )
            }
        }

        val pushBranchName = branch.removePrefix("refs/heads/")
        defaultGit.execGit(WithInheritSuccessfully, timeout = Duration.ofMinutes(5),
                command = "push", args = listOf(
                "--force-with-lease",
                "--set-upstream",
                "origin",
                pushBranchName,
        ))

        if (mode == null) return

        val safePushBranch = mode.safePushBranch(commit)
        defaultGit.execGit(WithInheritSuccessfully, timeout = Duration.ofMinutes(5),
                command = "push", args = listOf(
                "origin",
                "$pushBranchName:$safePushBranch",
        ))

        history.addSafePushBranch(SafePushBranchInfo(
                branch = branch,
                safePushBranch = safePushBranch,
                commitId = commit,
                mode = mode,
        ))

        history.invalidateSnapshot()
    }
}
