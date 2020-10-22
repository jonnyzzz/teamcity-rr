package com.jonnyzzz.teamcity.rr

private const val defaultWorktreeName = "intellij-rebase-helper"

fun GitRunner.gitRebase(branch: String, toHead: String): Boolean {
    val localBranch = listGitCurrentBranchName()
    if (localBranch != branch) {
        error("Rebase of non-local branch is not supported")
    }

    TODO()
}


private fun GitRunner.gitRebaseLocalBranch(toHead: String) : Boolean {
    TODO()
}


