package com.jonnyzzz.teamcity.rr


private val refsSafePushPrefix = "refs/safepush"
private val defaultSafePushBranchPrefix = "$refsSafePushPrefix/Eugene.Petrenko"

enum class SafePushMode(
        val branchNameInfix: String,
        val teamcityBuildType: String,
) {
    ALL(
            branchNameInfix = "all",
            teamcityBuildType = "ijplatform_master_SafePushWithTests_Push"
    ),

    COMPILE(
            branchNameInfix = "compile",
            teamcityBuildType = "ijplatform_master_SafePushWithCompilation_Push"
    ),
    ;

    fun safePushBranch(commit: String, targetBranch: String = "master"): String {
        return "$defaultSafePushBranchPrefix/$targetBranch/r${commit.take(8)}/$branchNameInfix"
    }

    fun teamcityBranchLinkFromBranch(branch: String): String {
        val teamcityBranch = branch.removePrefix(refsSafePushPrefix).trim('/')
        return "https://buildserver.labs.intellij.net/buildConfiguration/" +
                teamcityBuildType +
                "?branch=" + teamcityBranch.urlEncode() +
                "&mode=builds"
    }
}
