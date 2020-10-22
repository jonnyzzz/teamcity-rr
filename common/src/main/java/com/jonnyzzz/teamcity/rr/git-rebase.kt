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
        return GitRebaseResult(targetCommit)
    }

    if (listGitCurrentBranchName("HEAD") == listGitCurrentBranchName(branch)) {
        doUnderStash {
            return runRebaseAndHandleConflicts(branch, targetCommit)
        }
    }

    GitWorktreeBase(this).use {
        val tempBranchName = "auto-rebase-${branchCommit.take(8)}-to-${targetCommit.take(8)}-${System.currentTimeMillis()}"

        execGit(WithNoOutputSuccessfully, timeout = Duration.ofMinutes(15),
                command = "checkout", args = listOf("-b", tempBranchName, branchCommit))

        val rebaseResult = runRebaseAndHandleConflicts(branch, targetCommit) ?: return null

        val rebasedCommit = rebaseResult.newCommitId
        execGit(WithNoOutputSuccessfully,
                timeout = Duration.ofSeconds(5),
                command = "push",
                args = listOf(this@gitRebase.gitDir.toString(), "--force-with-lease=$branch:$branchCommit", "$rebasedCommit:$branch"))

        return rebaseResult
    }
}

private fun GitRunner.runRebaseAndHandleConflicts(branch: String, targetCommit: String): GitRebaseResult? {
    val code = execGit(WithOutput, timeout = Duration.ofMinutes(15),
            command = "rebase", args = listOf(targetCommit))

    if (code.exitCode == 0) {
        return gitHeadCommit("HEAD").let(::GitRebaseResult)
    }

    execGit(WithNoOutputSuccessfully, timeout = Duration.ofMinutes(5), command = "rebase", args = listOf("--abort"))
    return null
}

inline fun <Y> GitRunner.doUnderStash(action: GitRunner.() -> Y): Y {
    val stash = execGit(WithOutput, timeout = Duration.ofMinutes(15),
            command = "stash", args = listOf("push"))

    try {
        return action()
    } finally {
        if (stash.exitCode == 0) {
            execGit(WithNoOutputSuccessfully, timeout = Duration.ofMinutes(15),
                    command = "stash", args = listOf("pop"))
        }
    }
}
