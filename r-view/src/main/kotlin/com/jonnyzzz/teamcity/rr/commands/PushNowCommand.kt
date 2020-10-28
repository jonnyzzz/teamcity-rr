package com.jonnyzzz.teamcity.rr.commands

import com.jonnyzzz.teamcity.rr.WithInheritSuccessfully
import java.time.Duration

object PushNowCommand: CommandBase() {
    override fun Session.doTheCommandImpl() {
        val (_, commit) = getBranchFromArgs(snapshot.pendingBranches)
        defaultGit.execGit(WithInheritSuccessfully, timeout = Duration.ofMinutes(5),
                command = "push", args = listOf(
                "origin",
                "$commit:refs/heads/master",
        ))

        history.invalidateSnapshot()
    }
}