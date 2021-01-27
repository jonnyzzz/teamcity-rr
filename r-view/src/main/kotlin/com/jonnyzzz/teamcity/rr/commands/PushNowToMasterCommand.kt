package com.jonnyzzz.teamcity.rr.commands

import com.jonnyzzz.teamcity.rr.WithInheritSuccessfully
import java.time.Duration

object PushNowToMasterCommand : SnapshotOneBranchUpdatingCommandBase() {
    override fun Session.doTheCommandForBranch(branch: String, commit: String) {
        defaultGit.execGit(WithInheritSuccessfully, timeout = Duration.ofMinutes(5),
                command = "push", args = listOf(
                "origin",
                "$commit:refs/heads/master",
        ))
    }
}
