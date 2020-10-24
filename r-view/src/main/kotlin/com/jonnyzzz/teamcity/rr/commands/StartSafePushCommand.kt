package com.jonnyzzz.teamcity.rr.commands

import com.jonnyzzz.teamcity.rr.*
import java.time.Duration

object StartSafePushCommand : CommandBase() {
    override fun preferSnapshot(args: List<String>): Boolean = true
    override fun runRebase(args: List<String>): Boolean = false
    override fun runFetch(args: List<String>): Boolean = false

    override fun Session.doTheCommandImpl() {
        val (branch, commit) = getBranchFromArgs(snapshot.pendingBranches)

        val mode = when {
            "all" in args -> SafePushMode.ALL
            "compile" in args -> SafePushMode.COMPILE
            else -> throw UserErrorException("Failed to select safe-push type: [all, compile]")
        }

        val safePushBranch = mode.safePushBranch(commit)
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
                commitId = commit,
                mode = mode,
        ))

        invalidateSnapshot()
    }
}
