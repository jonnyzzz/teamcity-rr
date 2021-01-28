package com.jonnyzzz.teamcity.rr.commands

import com.jonnyzzz.teamcity.rr.*


abstract class CommandBase {
    abstract fun doTheCommand(args: List<String>)

    val defaultGit by lazy {
        val git = GitRunner(workdir = WorkDir)
        git.checkGitVersion()
        git
    }

    val history by lazy { TheHistory() }

    inner class Session(
            val args: List<String>
    ) {
        val snapshot by lazy {
            run {
                val snapshot = history.loadSnapshot()
                if (snapshot != null && snapshot.hasSameLight(computeLightSnapshot(defaultGit))) {
                    println("Using persisted snapshot from ${snapshot.created}")
                    return@lazy snapshot
                }
            }

            var snapshot = computeRawSnapshot(defaultGit = defaultGit, history = history)
            snapshot = collectChangesForPendingBranches(defaultGit, history, snapshot)
            snapshot = collectSafePushInfoForBranches(defaultGit, history, snapshot)
            history.saveSnapshot(snapshot)
            snapshot
        }
    }
}

fun CommandBase.Session.getBranchFromArgs(branches: Map<String, String>): Pair<String, String> {
    val allBranches = findAllBranchFromArgs(branches)
    return allBranches.singleOrNull()
            ?: throw UserErrorException("Failed to select unique branch for the command, candidates were: ${allBranches.keys.joinToString(", ")}")
}

private fun <K, V> Map<K, V>.singleOrNull() = entries.singleOrNull()?.let { it.key to it.value }

fun CommandBase.Session.findBranchFromArgs(branches: Map<String, String>): Pair<String, String>? {
    return findAllBranchFromArgs(branches).singleOrNull()
}

private fun CommandBase.Session.findAllBranchFromArgs(branches: Map<String, String>): Map<String, String> {
    if ("HEAD" in args) {
        return mapOf(snapshot.headBranch to snapshot.headCommit)
    }

    val preciseNames = args.filter { it.startsWith("=") || it.endsWith("=") }.map { it.trim('=') }
    if (preciseNames.isNotEmpty()) {
        return branches.entries.filter { (branch) -> branch in preciseNames }
                .map { it.key to it.value }.toMap().toSortedMap()
    }

    return branches.entries
            .filter { (branch) -> args.any { branch.contains(it, ignoreCase = true) } }
            .map { it.key to it.value }.toMap().toSortedMap()
}

abstract class SnapshotCacheCommandBase : CommandBase() {
    override fun doTheCommand(args: List<String>) {
        Session(args = args).doTheCommandImpl()
    }

    protected abstract fun Session.doTheCommandImpl()
}

abstract class SnapshotUpdatingCommandBase : CommandBase() {
    override fun doTheCommand(args: List<String>) {
        computeSnapshotFetch(defaultGit)
        SnapshotRebaseDriver(defaultGit, history).rebaseAll()

        Session(args = args).doTheCommandImpl()
    }

    protected abstract fun Session.doTheCommandImpl()
}

abstract class SnapshotOneBranchUpdatingCommandBase : CommandBase() {
    final override fun doTheCommand(args: List<String>) {
        run {
            val (branch, commit) = Session(args).run { getBranchFromArgs(snapshot.pendingBranches) }

            computeSnapshotFetch(defaultGit)
            val rebaseResult = SnapshotRebaseDriver(defaultGit, history).rebaseBranch(branch, commit)
            if (rebaseResult == null) {
                printWithHighlighting { bold("Rebase failed. Push will not run.") }
                throw UserErrorException("Rebase failed for branch $branch")
            }

            history.invalidateSnapshot()
        }

        run {
            val (branch, commit) = Session(args).run { getBranchFromArgs(snapshot.pendingBranches) }
            Session(args).doTheCommandForBranch(branch, commit)
            history.invalidateSnapshot()
        }
    }

    abstract fun Session.doTheCommandForBranch(branch: String, commit: String)
}
