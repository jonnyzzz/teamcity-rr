package com.jonnyzzz.teamcity.rr.commands

import com.jonnyzzz.teamcity.rr.listGitCommitsEx
import java.time.LocalDateTime
import java.time.Month

object ListHistory : SnapshotCacheCommandBase() {
  override fun Session.doTheCommandImpl() {
    val allHistory = (
            defaultGit.listGitCommitsEx("origin/master", commits = 32_000).associateBy { it.commitId }  //+
//                    defaultGit.listGitCommitsEx("origin/211",commits = 32_000).associateBy { it.commitId }
            )


    val relevantHistory = allHistory
            .values
            .filter {
              it.author.contains("jonnyzzz.com", ignoreCase = true) || (it.author.contains("eugene", ignoreCase = true) && it.author.contains("petrenko", ignoreCase = true))
            }
            .filter { it.authorDateTime > LocalDateTime.of(2021, Month.JANUARY, 1, 0, 0,0, 0) }
            .filter { it.authorDateTime.hour !in 8..22 }
            .sortedByDescending { it.authorDate }

    println("Commits in unusual hours:")
    for (info in relevantHistory) {
      println("${info.commitId}  ${info.authorDateTime} ${info.subject}")
    }

    println("Total commits: ${relevantHistory.size}")
  }
}
