package com.jonnyzzz.teamcity.rr

fun gitRebase(branch: String, toHead: String): Boolean {
    val localBranch = listGitCurrentBranchName()
    if (localBranch != branch) {
        error("Rebase of non-local branch is not supported")
    }

    TODO()
}


private fun gitRebaseLocalBranch(toHead: String) : Boolean {
    TODO()
}

