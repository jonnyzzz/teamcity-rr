package com.jonnyzzz.teamcity.rr


fun computeSnapshotBranches(defaultGit: GitRunner): List<RepositoryBranchInfo> {
    return defaultGit.listGitBranches()
            .filter { it.startsWith(defaultBranchPrefix) }
            //TODO: use defaultBranchPrefix as the prefix here (it may break other logic)
            .map { it.removePrefix("refs/heads/") }
            .toSortedSet()
            .map { branch ->
                val commitId = defaultGit.gitHeadCommit(branch)
                RepositoryBranchInfo(
                        commit = commitId,
                        branch = branch,
                )
            }
}
