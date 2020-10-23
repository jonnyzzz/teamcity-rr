package com.jonnyzzz.teamcity.rr

import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

abstract class RViewCommandBase {
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

    protected open fun preferSnapshot(args: List<String>): Boolean = "--snapshot" in args
    protected open fun runRebase(args: List<String>): Boolean = true
    protected open fun runFetch(args: List<String>): Boolean = "--no-fetch" !in args

    protected inner class Session(private val args: List<String>) {
        val preferSnapshot = preferSnapshot(args)
        val runFetch = !preferSnapshot && runFetch(args)
        val runRebase = !preferSnapshot && !runFetch && runRebase(args)
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

    protected fun findBranchFromArgs(snapshot: GitSnapshot, args: List<String>): Pair<String, String> {
        val (branch, commit) = snapshot.pendingBranches.entries.singleOrNull { (branch) ->
            args.any { branch.contains(it, ignoreCase = true) }
        } ?: throw UserErrorException("Failed to select unique branch for the command")
        return branch to commit
    }
}

object ShowCommand : RViewCommandBase() {
    override fun Session.doTheCommandImpl(snapshot: GitSnapshot, args: List<String>) {
        snapshot.showSnapshot()
    }
}

object ToggleRebaseMode : RViewCommandBase() {
    override fun runRebase(args: List<String>): Boolean = false
    override fun runFetch(args: List<String>): Boolean = false

    override fun Session.doTheCommandImpl(snapshot: GitSnapshot, args: List<String>) {
        val (_, commit) = findBranchFromArgs(snapshot, args)
        when {
            "disable" in args -> history.logRebaseFailed(commit)
            "enable" in args -> history.removeRebaseFailed(commit)
            else -> throw UserErrorException("Failed to select rebase mode from args")
        }
    }
}

object StartSafePushCommand : RViewCommandBase() {
    override fun preferSnapshot(args: List<String>): Boolean = true
    override fun runRebase(args: List<String>): Boolean = false
    override fun runFetch(args: List<String>): Boolean = false

    override fun Session.doTheCommandImpl(snapshot: GitSnapshot, args: List<String>) {
        val (branch, commit) = findBranchFromArgs(snapshot, args)

        val mode = when {
            "all" in args -> "all"
            "compile" in args -> "compile"
            else -> throw UserErrorException("Failed to select safe-push type: [all, compile]")
        }

        defaultGit.execGit(WithInheritSuccessfully, timeout = Duration.ofMinutes(5),
                command = "push", args = listOf(
                "origin", "-f",
                "$commit:$defaultSafePushBranchPrefix/master/j${commit.take(8)}/$mode",
                "$commit:refs/heads/${branch.removePrefix("refs/heads/")}",
        ))

        ///TODO: log created branch name into the history (to use it to check status on TeamCity)
    }

}
