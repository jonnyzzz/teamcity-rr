package com.jonnyzzz.teamcity.rr


private val refsSafePushPrefix = "refs/safepush"

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
        return "$refsSafePushPrefix/Eugene.Petrenko-r${commit.take(8)}/$targetBranch/$branchNameInfix"
    }

    fun teamcityBranchLinkFromBranch(branch: String): String {
        val teamcityBranch = branch.removePrefix(refsSafePushPrefix).trim('/')
        return "https://buildserver.labs.intellij.net/buildConfiguration/" +
                teamcityBuildType +
                "?branch=" + teamcityBranch.urlEncode() +
                "&mode=builds"
    }
}
