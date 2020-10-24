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

        val pushBranchName = branch.removePrefix("refs/heads/")
        defaultGit.execGit(WithInheritSuccessfully, timeout = Duration.ofMinutes(5),
                command = "push", args = listOf(
                "--force-with-lease=$pushBranchName:$commit",
                "--set-upstream",
                "origin",
                pushBranchName,
        ))

        if (mode == null) return

        val safePushBranch = mode.safePushBranch(commit)
        defaultGit.execGit(WithInheritSuccessfully, timeout = Duration.ofMinutes(5),
                command = "push", args = listOf(
                "origin",
                "$commit:$safePushBranch",
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
