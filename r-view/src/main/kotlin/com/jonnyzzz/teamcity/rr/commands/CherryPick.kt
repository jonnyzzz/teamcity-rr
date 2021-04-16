package com.jonnyzzz.teamcity.rr.commands

import com.jonnyzzz.teamcity.rr.UserErrorException
import com.jonnyzzz.teamcity.rr.WithInheritSuccessfully
import com.jonnyzzz.teamcity.rr.gitFetch
import com.jonnyzzz.teamcity.rr.listGitCommits
import java.time.Duration

object CherryPick : CommandBase() {
    override fun doTheCommand(args: List<String>) {
        val headCommits = defaultGit.listGitCommits("origin/master", 8192)
                .withIndex()
                .associate { (idx, k) -> k to idx }

        val errors = mutableMapOf<String, String>()

        val cherryPickArgs = args
                .toSet()
                .flatMap { it.split(",", ";", " ") }
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .mapNotNull { hash ->
                    //we need full commit IDs to make sure there is no clash of several objects with the same prefix
                    val commits = headCommits
                            .entries
                            .filter { (k, _) -> k.startsWith(hash) || k == hash }

                    when {
                        commits.isEmpty() -> {
                            errors[hash] = "commit is not found"
                            null
                        }
                        commits.size > 1 -> {
                            errors[hash] = "several candidates were found: $commits"
                            null
                        }
                        else -> commits.single()
                    }
                }
                //the cherry-pick command requires commits from the oldest to the newest
                .sortedByDescending { it.value }
                .map { it.key }

        if (errors.isNotEmpty()) {
            throw UserErrorException(buildString {
                appendLine("Failed to resolve ${errors.size} commits")
                for ((hash,error) in errors) {
                    append("  ${hash}: $error")
                }
            })
        }

        defaultGit.execGit(WithInheritSuccessfully,
                Duration.ofMinutes(30),
                "cherry-pick", args = cherryPickArgs)
    }
}
