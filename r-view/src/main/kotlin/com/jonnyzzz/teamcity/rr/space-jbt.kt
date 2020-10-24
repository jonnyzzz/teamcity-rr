package com.jonnyzzz.teamcity.rr


fun generateSpaceCommitsLink(commits: List<CommitInfo>): String {
    return "https://jetbrains.team/p/ij/code/intellij/commits" +
            "?" +
            "commits=" +
            "" + commits.joinToString(",") { it.commitId }

}