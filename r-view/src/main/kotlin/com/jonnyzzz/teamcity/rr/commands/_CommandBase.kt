package com.jonnyzzz.teamcity.rr.commands

import com.jonnyzzz.teamcity.rr.*
import java.time.Instant
import java.time.temporal.ChronoUnit

abstract class CommandBase {
    protected val defaultGit by lazy {
        val git = GitRunner(workdir = WorkDir)
        git.checkGitVersion()
        git
    }

    protected val history by lazy { TheHistory() }

    fun doTheCommand(args: List<String>) {
        Session(args).run {
            val snapshot = buildSnapshot()
            doTheCommandImpl(snapshot, args)
        }
    }

    protected fun invalidateSnapshot() {
        history.invalidateSnapshot()
    }

    protected open fun preferSnapshot(args: List<String>): Boolean = "--snapshot" in args
    protected open fun runRebase(args: List<String>): Boolean = true
    protected open fun runFetch(args: List<String>): Boolean = "--no-fetch" !in args

    protected inner class Session(private val args: List<String>) {
        val preferSnapshot = preferSnapshot(args)
        val runFetch = !preferSnapshot && runFetch(args)
        val runRebase = !preferSnapshot && runFetch && runRebase(args)
    }

    private fun Session.buildSnapshot(): GitSnapshot {
        if (preferSnapshot) {
            val snapshot = history.loadSnapshot()
            if (snapshot != null && snapshot.created.toInstant().until(Instant.now(), ChronoUnit.HOURS) < 2) {
                println("Using persisted snapshot from ${snapshot.created}")
                return snapshot
            }
        }

        var snapshot =
                computeCurrentStatus(
                        runFetch = runFetch,
                        defaultGit = defaultGit,
                        doRebase = runRebase,
                        history = history,
                )

        snapshot = collectChangesForPendingBranches(defaultGit, history, snapshot)
        history.saveSnapshot(snapshot)
        return snapshot
    }

    protected abstract fun Session.doTheCommandImpl(snapshot: GitSnapshot, args: List<String>)

    protected fun findBranchFromArgs(branches: Map<String, String>, args: List<String>): Pair<String, String> {
        val (branch, commit) = branches.entries.singleOrNull { (branch) ->
            args.any { branch.contains(it, ignoreCase = true) }
        } ?: throw UserErrorException("Failed to select unique branch for the command")
        return branch to commit
    }
}
