package com.jonnyzzz.teamcity.rr.commands

import com.jonnyzzz.teamcity.rr.UserErrorException
import com.jonnyzzz.teamcity.rr.WithInherit
import com.jonnyzzz.teamcity.rr.WithInheritSuccessfully
import com.jonnyzzz.teamcity.rr.printWithHighlighting
import java.time.Duration

object DeleteBranchCommand : SnapshotCacheCommandBase() {
    override fun Session.doTheCommandImpl() {
        val (branch, commit) = getBranchFromArgs(snapshot.allBranches)

        if (snapshot.headBranch == branch) {
            throw UserErrorException("You are on the same branch. Please change branch to delete this branch")
        }

        defaultGit.execGit(WithInheritSuccessfully, timeout = Duration.ofMinutes(1),
                command = "branch", args = listOf("-D", branch))

        //we do not know if this branch exists, so give it a try to kill it
        defaultGit.execGit(WithInherit, timeout = Duration.ofMinutes(1),
                command = "push", args = listOf("origin", ":$branch"))

        printWithHighlighting {
            "The branch " + bold(branch) + " was removed\n\n" +
                    "To restore it use the commit " + underline(commit)
        }

        history.branchRemoved(branch, commit)
        history.invalidateSnapshot()
    }
}

object ResetBranchCommand : SnapshotCacheCommandBase() {
    override fun Session.doTheCommandImpl() {
        val (branch, commit) = getBranchFromArgs(snapshot.allBranches)

        defaultGit.execGit(WithInheritSuccessfully, timeout = Duration.ofMinutes(15), command = "stash")

        defaultGit.execGit(WithInheritSuccessfully, timeout = Duration.ofMinutes(15),
                command = "reset", args = listOf("--hard", snapshot.masterCommit))

        //we do not know if this branch exists, so give it a try to kill it
        defaultGit.execGit(WithInherit, timeout = Duration.ofMinutes(1),
                command = "push", args = listOf("origin", ":$branch"))

        printWithHighlighting {
            "The branch " + bold(branch) + " was reset \n\n" +
                    "To restore it use the commit " + underline(commit) +

                    if (commit in snapshot.masterCommits) {
                        "  (commit is not unique, it is pushed to master)"
                    } else {
                        "  (commit is UNIQUE)"
                    }
        }

        history.branchRemoved(branch, commit)
        history.invalidateSnapshot()
    }
}
