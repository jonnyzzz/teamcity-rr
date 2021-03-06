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

    val fullBranchName = "refs/heads/" + branch.removePrefix("refs/heads")

    // if the branch is on a commit reachable from toHead,
    // means it is possible to just move the reference, as
    // there is no new changes done by us
    if (isIncludedInHead(branchCommit)) {
        smartUpdateRef(fullBranchName, targetCommit)
        return GitRebaseResult(targetCommit)
    }

    val rebaseResult = this.withMirrorCheckout(targetCommit) {
        val tempBranchName = "auto-rebase-${branchCommit.take(8)}-to-${targetCommit.take(8)}-${System.currentTimeMillis()}"

        execGit(WithInheritSuccessfully, timeout = Duration.ofMinutes(15),
                command = "checkout", args = listOf("-b", tempBranchName, branchCommit))

        val rebaseResult = runRebaseAndHandleConflicts(targetCommit) ?: return@withMirrorCheckout null

        val rebasedCommit = rebaseResult.newCommitId
        this@gitRebase.execGit(WithNoOutputSuccessfully,
                timeout = Duration.ofSeconds(15),
                command = "fetch",
                args = listOf(gitDir.toString(), rebasedCommit))

        rebaseResult
    } ?: return null

    smartUpdateRef(fullBranchName, rebaseResult.newCommitId)
    return rebaseResult
}

private fun GitRunner.smartUpdateRef(branch: String, targetCommit: String) {
    if (listGitCurrentBranchName() == listGitCurrentBranchName(branch)) {
        doUnderStash {
            //theoretically, we may do rebase here instead of hard reset,
            //the current approach may leak some files along the way
            execGit(
                WithInheritSuccessfully, timeout = Duration.ofMinutes(5),
                command = "reset", args = listOf("--hard", targetCommit)
            )
        }
    } else {
        execGit(
            WithInheritSuccessfully, timeout = Duration.ofMinutes(1),
            command = "update-ref", args = listOf(branch, targetCommit, "-m", "r-view rebase")
        )
    }
}

private fun GitRunner.runRebaseAndHandleConflicts(targetCommit: String): GitRebaseResult? {
    val code = execGit(WithOutput, timeout = Duration.ofMinutes(15),
            command = "rebase", args = listOf(targetCommit))

    if (code.exitCode == 0) {
        return gitHeadCommit("HEAD").let(::GitRebaseResult)
    }

    println("Rebase failed: ")
    println(code.stdout)
    println()
    println(code.stderr)
    println()

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
            execGit(WithInheritSuccessfully, timeout = Duration.ofMinutes(15),
                    command = "stash", args = listOf("pop"))
        }
    }
}
