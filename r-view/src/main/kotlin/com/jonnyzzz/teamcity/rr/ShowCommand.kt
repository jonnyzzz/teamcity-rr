package com.jonnyzzz.teamcity.rr

import org.slf4j.LoggerFactory

fun showPendingBuilds(args: List<String>) = ShowCommand().showPendingBuilds(args)

private class ShowCommand {
    private val LOG = LoggerFactory.getLogger(ShowCommand::class.java)

    private val defaultGit by lazy {
        val git = GitRunner(workdir = WorkDir)
        git.checkGitVersion()
        git
    }
    private val userEmail by lazy { defaultGit.getUserEmail() }

    private val defaultSafePushBranchPredicate: (String) -> Boolean by lazy {
        val username = userEmail.user
        val text = "refs/heads/safepush/${username}/"
        { it.equals(text, ignoreCase = true)}
    }

    private val defaultBranchPrefix = "refs/heads/jonnyzzz/"  //TODO: configuration?

    fun showPendingBuilds(args: List<String>) {
        println("Checking current status...")
        println()


        val history = defaultGit.listGitCommitsEx("exp1", notIn = "origin/master")
        println(history)

        defaultGit.gitRebase("exp1", "origin/master")
        defaultGit.gitRebase("exp3", "origin/master")

        return

        if ("--no-fetch" !in args) {
            //TODO: check if there were changes to avoid useless rebase
            println("Fetching changes from remote...")
            defaultGit.gitFetch()
        }

        val recentCommits: Set<String> = defaultGit.listGitCommits("origin/master", 4096).toHashSet()
        println("Listed ${recentCommits.size} recent commits")

        var branches = defaultGit.listGitBranches()
                .filterKeys { it.startsWith(defaultBranchPrefix) }

        println("Collected ${branches.size} local Git branches with $defaultBranchPrefix:")
        println()

        val alreadyMergedBranches = branches
                //branches that are in a commit from origin/master are fully ready
                .filter { (_, commit) -> commit !in recentCommits }

        println("Already completed branches:")
        for ((branch, _) in alreadyMergedBranches) {
            println("  $branch")
        }
        println()

        branches = branches.filterKeys { it !in alreadyMergedBranches }
        if (branches.isEmpty()) return

        println("Rebasing ${branches.size} active branches...")
        println("Rebase is not yet implemented.")
        println()

        val remoteBranches = defaultGit.listGitLsRemote()
                .filterKeys { defaultSafePushBranchPredicate(it) }
        val hashToSafePushBranches = remoteBranches.entries.groupBy({ it.value }, { it.key })

        println("Collected ${remoteBranches.size} pending safe-push branches")
        println()

        val pendingBranches = branches
                .filter { it.value in hashToSafePushBranches }
        branches = branches.filterKeys { it !in pendingBranches }

        if (pendingBranches.isNotEmpty()) {
            //TODO: it is hard to guess if a current, but rebased branch is/was running or not
            println("Currently pending branches:")
            for (pendingBranch in pendingBranches) {
                println("  ${pendingBranch.key}")
            }
            println()
        }

        if (branches.isEmpty()) return

        println("There are several still incomplete branches:")
        for (pendingBranch in branches) {
            println("  ${pendingBranch.key}")
        }
        println()
    }
}
