package com.jonnyzzz.teamcity.rr.commands

import com.jonnyzzz.teamcity.rr.*
import java.time.Duration

object StartSafePushCommand : CommandBase() {
    override fun preferSnapshot(args: List<String>): Boolean = true
    override fun runRebase(args: List<String>): Boolean = false
    override fun runFetch(args: List<String>): Boolean = false

    override fun Session.doTheCommandImpl(snapshot: GitSnapshot, args: List<String>) {
        val (branch, commit) = findBranchFromArgs(snapshot.pendingBranches, args)

        val mode = when {
            "all" in args -> "all"
            "compile" in args -> "compile"
            else -> throw UserErrorException("Failed to select safe-push type: [all, compile]")
        }

        val safePushBranch = "$defaultSafePushBranchPrefix/master/j${commit.take(8)}/$mode"
        defaultGit.execGit(WithInheritSuccessfully, timeout = Duration.ofMinutes(5),
                command = "push", args = listOf(
                "origin",
                "$commit:$safePushBranch",
                "-f",
                "$commit:refs/heads/${branch.removePrefix("refs/heads/")}",
        ))

        history.addSafePushBranch(SafePushBranchInfo(
                branch = branch,
                safePushBranch = safePushBranch,
                commitId = commit
        ))

        invalidateSnapshot()
    }
}
