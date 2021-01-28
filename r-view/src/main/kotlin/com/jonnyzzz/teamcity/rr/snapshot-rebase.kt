package com.jonnyzzz.teamcity.rr


class SnapshotRebaseDriver(
        private val defaultGit: GitRunner,
        private val history: TheHistory,
) {
    private val masterCommit by lazy { computeSnapshotMasterCommit(defaultGit) }
    private val recentMasterCommits by lazy { defaultGit.listGitCommits(masterCommit).toSet() }

    fun rebaseAll() {
        for (branchInfo in computeSnapshotBranches(defaultGit)) {
            val commit = branchInfo.commit
            val branch = branchInfo.branch

            if (history.isBrokenForRebase(commit, branch)) continue

            rebaseBranch(branch, commit)
        }
    }

    fun rebaseBranch(branch: String, commit: String): GitRebaseResult? {
        printWithHighlighting { "Rebasing " + bold(branch) + "..." }

        val maxDistance = 128
        val branchCommits = defaultGit.listGitCommits(commit, commits = maxDistance + 12)

        if (masterCommit in branchCommits) {
            println("Branch $branch already contains the commit from master head. No rebase.")
            // there is no need to rebase - the branch is up-to-date
            return GitRebaseResult(commit)
        }

        val distanceToMaster = branchCommits.withIndex().firstOrNull { (_, commit: String) -> commit in recentMasterCommits }?.index
        if (distanceToMaster == null || distanceToMaster > maxDistance) {
            println("Branch $branch is more than $maxDistance commits away from the `master`: $distanceToMaster. Automatic rebase will not run.")
            history.logRebaseFailed(commit, branch = null)
            return null
        }

        val rebaseResult = defaultGit.gitRebase(
                branch = branch,
                toHead = masterCommit,
                isIncludedInHead = { it in recentMasterCommits }
        )

        if (rebaseResult == null) {
            println("Branch $branch rebase failed. Auto-rebase is disabled for it. No rebase.")
            history.logRebaseFailed(commit, branch = null)
            return null
        }

        history.removeRebaseFailed(commit, branch)
        history.removeRebaseFailed(rebaseResult.newCommitId, branch)
        return rebaseResult
    }
}
