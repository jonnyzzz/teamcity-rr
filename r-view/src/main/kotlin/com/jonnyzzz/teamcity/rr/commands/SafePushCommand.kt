package com.jonnyzzz.teamcity.rr.commands

import com.jonnyzzz.teamcity.rr.*
import java.time.Duration

object SafePushCommand : SnapshotOneBranchUpdatingCommandBase() {
    override fun Session.doTheCommandForBranch(branch: String, commit: String) {
        val mode = run {
            val lowerCaseArgs = args.map { it.toLowerCase() }
            when {
                "all" in lowerCaseArgs -> SafePushMode.ALL
                "test" in lowerCaseArgs -> SafePushMode.ALL
                "tests" in lowerCaseArgs -> SafePushMode.ALL
                "compile" in lowerCaseArgs -> SafePushMode.COMPILE
                else -> null
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
    }
}
