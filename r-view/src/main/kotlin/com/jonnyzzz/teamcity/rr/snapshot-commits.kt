package com.jonnyzzz.teamcity.rr

fun computeSnapshotMasterCommit(defaultGit: GitRunner): String {
    return defaultGit.gitHeadCommit("origin/master")
}

fun computeSnapshotRecentMasterCommits(defaultGit: GitRunner,
                                       masterCommit: String = computeSnapshotMasterCommit(defaultGit)): Map<String, CommitInfo> {
    return defaultGit
                .listGitCommitsEx(masterCommit, commits = 2048)
                .associateBy { it.commitId }

}
