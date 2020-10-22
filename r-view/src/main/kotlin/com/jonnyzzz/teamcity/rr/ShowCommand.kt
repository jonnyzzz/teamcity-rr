package com.jonnyzzz.teamcity.rr

fun showPendingBuilds(args: List<String>) = ShowCommand().showPendingBuilds(args)

private class ShowCommand {
    private val defaultGit by lazy {
        val git = GitRunner(workdir = WorkDir)
        git.checkGitVersion()
        git
    }

    private val history by lazy { TheHistory() }

    fun showPendingBuilds(args: List<String>) {
        var snapshot = computeCurrentStatus(runFetch = "--no-fetch" !in args, defaultGit = defaultGit, history = history)
        snapshot = collectChangesForPendingBranches(defaultGit, history, snapshot)
        snapshot.showSnapshot()
    }
}
