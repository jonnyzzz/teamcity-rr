package com.jonnyzzz.teamcity.rr

import java.time.Duration

data class GitRebaseResult(
        val newCommitId: String
)

fun GitRunner.gitRebase(branch: String, toHead: String, isIncludedInHead: (String) -> Boolean = {false}): GitRebaseResult? {
    val branchCommit = gitHeadCommit(branch)
    val targetCommit = gitHeadCommit(toHead)

    if (branchCommit == targetCommit) {
        return GitRebaseResult(targetCommit)
    }

    if (listGitCurrentBranchName("HEAD") == listGitCurrentBranchName(branch)) {
        doUnderStash {
            return runRebaseAndHandleConflicts(targetCommit)
        }
    }

    val fullBranchName = "refs/heads/" + branch.removePrefix("refs/heads")

    // if the branch is on a commit reachable from toHead,
    // means it is possible to just move the reference, as
    // there is no new changes done by us
    if (isIncludedInHead(branchCommit)) {
        execGit(WithInheritSuccessfully, timeout = Duration.ofMinutes(1),
        command = "update-ref", args = listOf(fullBranchName, targetCommit))
        return GitRebaseResult(targetCommit)
    }

    GitWorktreeBase(this).use {
        val tempBranchName = "auto-rebase-${branchCommit.take(8)}-to-${targetCommit.take(8)}-${System.currentTimeMillis()}"

        execGit(WithNoOutputSuccessfully, timeout = Duration.ofMinutes(15),
                command = "checkout", args = listOf("-b", tempBranchName, branchCommit))

        val rebaseResult = runRebaseAndHandleConflicts(targetCommit) ?: return null

        val rebasedCommit = rebaseResult.newCommitId
        execGit(WithNoOutputSuccessfully,
                timeout = Duration.ofSeconds(5),
                command = "push",
                args = listOf(this@gitRebase.gitDir.toString(), "--force-with-lease=$fullBranchName:$branchCommit", "$rebasedCommit:$fullBranchName"))

        return rebaseResult
    }
}

private fun GitRunner.runRebaseAndHandleConflicts(targetCommit: String): GitRebaseResult? {
    val code = execGit(WithOutput, timeout = Duration.ofMinutes(15),
            command = "rebase", args = listOf(targetCommit))

    if (code.exitCode == 0) {
        return gitHeadCommit("HEAD").let(::GitRebaseResult)
    }

    execGit(WithNoOutputSuccessfully, timeout = Duration.ofMinutes(5), command = "rebase", args = listOf("--abort"))
    return null
}

fun GitRunner.getStashObjectId(): String {
    return execGit(WithOutput, timeout = Duration.ofMinutes(15),
            command = "rev-parse", args = listOf("-q", "--verify", "refs/stash"))
            .successfully().stdout.trim()
}

inline fun <Y> GitRunner.doUnderStash(action: GitRunner.() -> Y): Y {
    //https://stackoverflow.com/a/34116244/49811
    val oldStash = getStashObjectId()
    execGit(WithInheritSuccessfully, timeout = Duration.ofMinutes(15),
            command = "stash", args = listOf("push"))
    val newStash = getStashObjectId()

    try {
        return action()
    } finally {
        if (oldStash != newStash) {
            execGit(WithNoOutputSuccessfully, timeout = Duration.ofMinutes(15),
                    command = "stash", args = listOf("pop"))
        }
    }
}
