package com.jonnyzzz.teamcity.rr

import java.time.*
import java.util.*

fun GitRunner.checkGitVersion() {
    val result = execGit(
            WithOutput,
            command = "version",
            timeout = Duration.ofSeconds(5),
    )

    if (result.exitCode != 0) {
        throwFailedToExecuteGit()
    }

    val gitVersion = result.stdout.trim().removePrefix("git version").trim()
    println("Using git version: $gitVersion")

    val versionElements = gitVersion.split(".").map { it.toIntOrNull() }

    require(versionElements.first() == 2 && versionElements.getOrNull(1)?.let { it >= 29 } == true) {
        "You must have git at least of 2.29 and less than 3.x, but was: $gitVersion"
    }
}


fun GitRunner.gitFetch() {
    execGit(WithInheritSuccessfully,
            command = "fetch",
            timeout = Duration.ofMinutes(10)
    )
}

fun GitRunner.setConfig(key: String, value: String) {
    execGit(WithOutput, timeout = Duration.ofMinutes(1),
            command = "config", args = listOf(key, value)).successfully()
}

fun GitRunner.getConfig(key: String): String? {
    val result = execGit(WithOutput, timeout = Duration.ofMinutes(1),
            command = "config", args = listOf("--get", key))
    if (result.exitCode == 0) {
        return result.stdout.trim()
    }
    return null
}

fun GitRunner.listGitBranches() = listGitBranchesImpl()
fun GitRunner.listGitRemoteBranches() = listGitBranchesImpl("-r")

private fun GitRunner.listGitBranchesImpl(vararg extra: String): Set<String> {
    return execGit(WithOutput,
            command = "branch",
            //%(objectname) returns incorrect commitId here
            args = listOf("--format=%(refname)") + extra,
            timeout = Duration.ofMinutes(10)
    ).successfully().stdout.split("\n").map { it.trim() }.filter { it.isNotBlank() }
            .toSortedSet()
}

fun GitRunner.listGitLsRemote(): Map<String, String> {
    return execGit(
            WithOutput,
            command = "ls-remote",
            timeout = Duration.ofMinutes(1),
    ).successfully().stdout.split("\n").map { it.trim() }.filter { it.isNotBlank() }
            .mapNotNull {
                val split = it.split(" ", limit = 2)
                if (split.size != 2) return@mapNotNull null
                val (hash, branch) = split
                branch to hash
            }
            .toMap().toSortedMap()
}

fun GitRunner.listGitCurrentBranchName(ref: String = "HEAD"): String {
    return execGit(
            WithOutput,
            command = "rev-parse",
            args = listOf("--symbolic-full-name", ref),
            timeout = Duration.ofSeconds(5),
    ).successfully().stdout.trim()
}

fun GitRunner.listGitCommits(head: String, commits: Int = 2048): List<String> {
    //git log --topo-order --no-abbrev-commit --format='%H' 01f6cfd510ae51e6a8fa22046843a121737c8fdc
    return execGit(
            WithOutput,
            command = "log",
            args = listOf("-$commits", "--topo-order", "--format=%H", head),
            timeout = Duration.ofMinutes(5),
    ).successfully().stdout.split("\n").map { it.trim() }.filter { it.isNotBlank() }
}

data class CommitInfo(
        val commitId: String,
        val authorDate: Long,
        val author: String,
        val subject: String
) {
    val authorDateTime : LocalDateTime
      get() = LocalDateTime.ofInstant(Instant.ofEpochSecond(authorDate), ZoneOffset.UTC)

    override fun hashCode() = Objects.hash(authorDate, author)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CommitInfo) return false

        //commitId is not included here to make it rebase-safe
        if (authorDate != other.authorDate) return false
        if (author != other.author) return false
        return true
    }
}

fun GitRunner.listGitCommitsEx(head: String, notIn: String? = null, commits: Int = 2048): List<CommitInfo> {
    val blockSep = "THIS_IS_NEXT_COMMIT"
    val dateSep = "THIS_IS_DATE_SEP"
    val authorSep = "THIS_IS_AUTHOR"
    val subjectSep = "THIS_IS_COMMIT_SUBJECT"
    val format = blockSep +
                    "%H" +
                    "$dateSep%ad" +
                    "$authorSep%an <%ae>" +
                    "$subjectSep%s"

    val text = execGit(
            WithOutput,
            command = "log",
            args = listOf("-$commits", "--date-order", "--date=unix", "--format=$format") + when {
                notIn != null -> "$notIn..$head"
                else -> head
            },
            timeout = Duration.ofMinutes(5),
    ).successfully().stdout

    return text
            .splitToSequence(blockSep)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .mapNotNull { block ->
                val elements = block.splitToSequence(dateSep, authorSep, subjectSep).toList()
                if (elements.size != 4) return@mapNotNull null
                val (commit, date, author, subject) = elements
                CommitInfo(
                        commitId = commit.trim(),
                        authorDate = date.toLongOrNull() ?: error("Failed to parse commit date: $date for $commit"),
                        author = author.trim(),
                        subject = subject.replace(Regex("(\n|\r|\t|\\s)+"), " ").trim()
                )
            }.toList()
}

fun GitRunner.showCommitShort(commit: String): String {
    //git log --topo-order --no-abbrev-commit --format='%H' 01f6cfd510ae51e6a8fa22046843a121737c8fdc
    val info = execGit(
            WithOutput,
            command = "show",
            args = listOf("--pretty=oneline", commit),
            timeout = Duration.ofSeconds(5),
    ).successfully().stdout.split("\n").map { it.trim() }.first { it.isNotBlank() }
    require(info.startsWith(commit))
    return info
}

fun GitRunner.generateDiffStat(commits: List<String>): String {
    //git log --topo-order --no-abbrev-commit --format='%H' 01f6cfd510ae51e6a8fa22046843a121737c8fdc
    return execGit(
            WithOutput,
            command = "diff",
            args = listOf("--stat", *commits.toTypedArray()),
            timeout = Duration.ofSeconds(5),
    ).successfully().stdout
}

fun GitRunner.gitPushCommit(headCommit: String, targetBranchName: String) {
    execGit(
            WithInheritSuccessfully,
            command = "push",
            args = listOf("origin", "$headCommit:$targetBranchName"),
            timeout = Duration.ofMinutes(5),
    )
}

fun GitRunner.gitHeadCommit(head : String = "HEAD"): String {
    return execGit(
            WithOutput,
            command = "rev-parse",
            args = listOf(head),
            timeout = Duration.ofSeconds(5),
    ).successfully().stdout.trim()
}

data class GitUserEmail(val email: String) {
    val user = email.split("@").first()
}

fun GitRunner.getUserEmail(): GitUserEmail {
    val output =
            execGit(WithOutput,
                    command = "config", args = listOf("--get", "user.email"),
                    timeout = Duration.ofSeconds(4)
            ).successfully().stdout.trim()

    require(output.contains("@")) {
        "Git user.email is bogus: $output"
    }
    return GitUserEmail(output)
}
