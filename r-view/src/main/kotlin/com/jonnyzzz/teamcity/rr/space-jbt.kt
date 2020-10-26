package com.jonnyzzz.teamcity.rr


fun generateSpaceCommitsLink(commits: List<CommitInfo>): String {
    val query = "id:" + commits.joinToString(",") { it.commitId }
    return "https://jetbrains.team/p/ij/code/intellij/commits" +
            "?" +
            "query=" + query.urlEncode()
}
