package com.jonnyzzz.teamcity.rr.commands

import com.jonnyzzz.teamcity.rr.GitSnapshot
import com.jonnyzzz.teamcity.rr.showSnapshot

object UpdateCommand : CommandBase() {
    override fun Session.doTheCommandImpl(snapshot: GitSnapshot, args: List<String>) {
        snapshot.showSnapshot()
    }
}
