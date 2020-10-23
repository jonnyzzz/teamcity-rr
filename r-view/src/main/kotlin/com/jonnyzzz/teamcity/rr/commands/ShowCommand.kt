package com.jonnyzzz.teamcity.rr.commands

import com.jonnyzzz.teamcity.rr.GitSnapshot
import com.jonnyzzz.teamcity.rr.showSnapshot

object ShowCommand : CommandBase() {
    override fun preferSnapshot(args: List<String>) = "--no-snapshot" !in args

    override fun Session.doTheCommandImpl(snapshot: GitSnapshot, args: List<String>) {
        snapshot.showSnapshot()
    }
}
