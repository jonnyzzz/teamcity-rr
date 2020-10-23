package com.jonnyzzz.teamcity.rr


private val safePushSuffix = "safepush/Eugene.Petrenko"
private val defaultSafePushBranchPrefix = "refs/$safePushSuffix"
private val defaultLocalPushBranchPrefix = "origin/safepush"

enum class SafePushMode(val branchNameInfix: String) {
    ALL(branchNameInfix = "all"),
    COMPILE(branchNameInfix = "compile"),
    ;

    fun safePushBranch(commit: String, targetBranch: String = "master") : String {
        return "$defaultSafePushBranchPrefix/$targetBranch/j${commit.take(8)}/$branchNameInfix"
    }
}
