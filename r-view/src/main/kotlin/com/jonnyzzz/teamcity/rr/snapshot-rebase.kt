package com.jonnyzzz.teamcity.rr


class SnapshotRebaseDriver(
        private val defaultGit: GitRunner,
        private val history: TheHistory,
) {
    private val masterCommit by lazy { computeSnapshotMasterCommit(defaultGit) }
    private val recentMasterCommits by lazy { computeSnapshotRecentMasterCommits(defaultGit, masterCommit = masterCommit) }

    fun rebaseAll() {
        for (branchInfo in computeSnapshotBranches(defaultGit)) {
            val commit = branchInfo.commit
            val branch = branchInfo.branch

            rebaseBranch(branch, commit)
        }
    }

    fun rebaseBranch(branch: String, commit: String): GitRebaseResult? {
        if (history.isBrokenForRebase(commit, branch)) return null

        val maxDistance = 128
        val branchCommits = defaultGit.listGitCommits(commit, commits = maxDistance + 12)

        if (masterCommit in branchCommits) {
            // there is no need to rebase - the branch is up-to-date
            return null
        }

        val distanceToMaster = branchCommits.withIndex().firstOrNull { (_, commit: String) -> commit in recentMasterCommits }?.index
        if (distanceToMaster == null || distanceToMaster > maxDistance) {
            println("Branch $branch is more than $maxDistance commits away from the `master`: $distanceToMaster. Automatic rebase will not run.")
            history.logRebaseFailed(commit, branch = null)
            return null
        }

        printWithHighlighting { "Rebasing " + bold(branch) + "..." }

        val rebaseResult = defaultGit.gitRebase(
                branch = branch,
                toHead = masterCommit,
                isIncludedInHead = { it in recentMasterCommits }
        )

        if (rebaseResult == null) {
            history.logRebaseFailed(commit, branch = null)
        }

        return rebaseResult
    }
}
