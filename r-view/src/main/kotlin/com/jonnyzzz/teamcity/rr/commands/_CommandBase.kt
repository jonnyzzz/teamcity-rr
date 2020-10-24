package com.jonnyzzz.teamcity.rr.commands

import com.jonnyzzz.teamcity.rr.*

abstract class CommandBase {
    protected val defaultGit by lazy {
        val git = GitRunner(workdir = WorkDir)
        git.checkGitVersion()
        git
    }

    protected val history by lazy { TheHistory() }

    fun doTheCommand(args: List<String>) {
        Session(args).doTheCommandImpl()
    }

    protected fun invalidateSnapshot() {
        history.invalidateSnapshot()
    }

    protected open fun preferSnapshot(args: List<String>): Boolean = "--snapshot" in args
    protected open fun runRebase(args: List<String>): Boolean = true
    protected open fun runFetch(args: List<String>): Boolean = "--no-fetch" !in args

    inner class Session(val args: List<String>) {
        val preferSnapshot = preferSnapshot(args)
        val runFetch = !preferSnapshot && runFetch(args)
        val runRebase = !preferSnapshot && runFetch && runRebase(args)

        val snapshot by lazy {
            val lightSnapshot = computeLightSnapshot(defaultGit)

            if (preferSnapshot) {
                val snapshot = history.loadSnapshot()
                if (snapshot != null && snapshot.hasSameLight(lightSnapshot)) {
                    println("Using persisted snapshot from ${snapshot.created}")
                    return@lazy snapshot
                }
            }

            var snapshot =
                    computeCurrentStatus(
                            lightSnapshot = lightSnapshot,
                            runFetch = runFetch,
                            defaultGit = defaultGit,
                            doRebase = runRebase,
                            history = history,
                    )

            snapshot = collectChangesForPendingBranches(defaultGit, history, snapshot)
            snapshot = collectSafePushInfoForBranches(defaultGit, history, snapshot)
            history.saveSnapshot(snapshot)
            snapshot
        }

        fun getBranchFromArgs(branches: Map<String, String>): Pair<String, String> {
            return findBranchFromArgs(branches)
                    ?: throw UserErrorException("Failed to select unique branch for the command")
        }

        fun findBranchFromArgs(branches: Map<String, String>): Pair<String, String>? {
            val (branch, commit) = branches.entries.singleOrNull { (branch) ->
                args.any { branch.contains(it, ignoreCase = true) }
            } ?: return null
            return branch to commit
        }
    }

    protected abstract fun Session.doTheCommandImpl()
}

abstract class SnapshotCommandBase : CommandBase() {
    override fun preferSnapshot(args: List<String>) = "--no-snapshot" !in args
}
