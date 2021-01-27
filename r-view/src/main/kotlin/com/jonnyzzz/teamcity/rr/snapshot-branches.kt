package com.jonnyzzz.teamcity.rr


fun computeSnapshotBranches(defaultGit: GitRunner): List<RepositoryBranchInfo> {
    return defaultGit.listGitBranches().loadBranches(defaultGit)
}

fun Set<String>.filterPersonalBranches() = this
    .filter { it.startsWith(defaultBranchPrefix) }
    //TODO: use defaultBranchPrefix as the prefix here (it may break other logic)
    .map { it.removePrefix("refs/heads/") }

fun Set<String>.filterPersonalRemoteBranches() = this
    .filter { it.startsWith(defaultRemoteBranchPrefix) }
    //TODO: use defaultBranchPrefix as the prefix here (it may break other logic)
    .map { it.removePrefix("refs/remotes/origin/") }

private fun Set<String>.loadBranches(defaultGit: GitRunner) = this
    .filterPersonalBranches()
    .toSortedSet()
    .map { branch ->
        val commitId = defaultGit.gitHeadCommit(branch)
        RepositoryBranchInfo(
            commit = commitId,
            branch = branch,
        )
    }
