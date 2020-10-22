package com.jonnyzzz.teamcity.rr

import java.time.Duration

data class GitRebaseResult(
        val newCommitId: String
)

//returns the new commit of the branch
fun GitRunner.gitRebase(branch: String, toHead: String): GitRebaseResult? {
    val branchCommit = gitHeadCommit(branch)
    val targetCommit = gitHeadCommit(toHead)

    if (branchCommit == targetCommit) {
        println("Branch $branch and $toHead are on the same commit. No rebase is needed")
        return GitRebaseResult(targetCommit)
    }

    if (listGitCurrentBranchName("HEAD") == listGitCurrentBranchName(branch)) {
        println("Rebasing local branch...")

        doUnderStash {
            return runRebaseAndHandleConflicts(targetCommit)
        }
    }

    GitWorktreeBase(this).use {
        val tempBranchName = "auto-rebase-${branchCommit.take(8)}-to-${targetCommit.take(8)}-${System.currentTimeMillis()}"

        execGit(WithInheritSuccessfully, timeout = Duration.ofMinutes(15),
                command = "checkout", args = listOf("-b", tempBranchName, branchCommit))

        val rebaseResult = runRebaseAndHandleConflicts(targetCommit) ?: return null

        val rebasedCommit = rebaseResult.newCommitId
        execGit(WithInheritSuccessfully,
                timeout = Duration.ofSeconds(5),
                command = "push",
                args = listOf(this@gitRebase.gitDir.toString(), "--force-with-lease=$branch:$branchCommit", "$rebasedCommit:$branch"))

        return rebaseResult
    }
}

private fun GitRunner.runRebaseAndHandleConflicts(targetCommit: String): GitRebaseResult? {
    val code = execGit(WithInherit, timeout = Duration.ofMinutes(15),
            command = "rebase", args = listOf(targetCommit))

    if (code.exitCode == 0) {
        return gitHeadCommit("HEAD").let(::GitRebaseResult)
    }

    println("Automatic rebase failed.")
    execGit(WithInheritSuccessfully, timeout = Duration.ofMinutes(5), command = "rebase", args = listOf("--abort"))
    return null
}

inline fun <Y> GitRunner.doUnderStash(action: GitRunner.() -> Y) : Y {
    val stash = execGit(WithInherit, timeout = Duration.ofMinutes(15),
            command = "stash", args = listOf("push"))

    try {
        return action()
    } finally {
        if (stash.exitCode == 0) {
            execGit(WithInheritSuccessfully, timeout = Duration.ofMinutes(15),
                    command = "stash", args = listOf("pop"))
        }
    }
}
