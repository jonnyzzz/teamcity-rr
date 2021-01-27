package com.jonnyzzz.teamcity.rr

import java.io.File
import java.time.Duration
import kotlin.system.measureTimeMillis

class GitRunner(
        val workdir: File,
        val gitDir: File = workdir / ".git"
) {
    private val ENV_GIT_COMMAND = "TEAMCITY_RR_GIT"
    private val GIT_COMMAND = System.getenv(ENV_GIT_COMMAND) ?: "git"

    override fun toString() = "GitRunner(workdir=$workdir)"

    val isValidGitCheckout
        get() = workdir.isDirectory && gitDir.isDirectory && (gitDir / "config").isFile

    fun <T> execGit(mode: ProcessExecMode<T>,
                    timeout: Duration,
                    command: String,
                    bare: Boolean = false,
                    args: List<String> = listOf()): T {
        val result: T

        val time = measureTimeMillis {
            result = execProcess(mode,
                    workDir = workdir,
                    timeout = timeout,
                    args = listOf(GIT_COMMAND) +
                            (if (bare) listOf() else listOf("--git-dir=$gitDir")) +
                            command + args
            )
        }

        if (command == "push" || command == "fetch") {
            println("Git $command took: ${Duration.ofMillis(time).seconds} seconds")
        }

        return result
    }

    fun throwFailedToExecuteGit(): Nothing =
            throw UserErrorException("Failed to execute `$GIT_COMMAND version` command. " +
                    "Please check you have configured git in system path or set the " +
                    "`$ENV_GIT_COMMAND` environment variable with the correct path.")

    fun <Y> withMirrorCheckout(onCommit: String, action: GitRunner.() -> Y) : Y {
        return GitWorktreeBase(this).use(onCommit, action)
    }
}

private class GitWorktreeBase(
        private val mainGit: GitRunner
) {
    val tempGit = GitRunner((mainGit.workdir.parentFile / (mainGit.workdir.name + "-rebase-helper")).absoluteFile)

    private fun initWorktreeIfNeeded() {
        if (!tempGit.isValidGitCheckout) {
            tempGit.workdir.mkdirs()
            tempGit.execGit(WithNoOutputSuccessfully, bare = true, timeout = Duration.ofMinutes(15), command = "init")
        }

        //it's worth to rewrite alternates, just in case
        val alternates = tempGit.gitDir / "objects" / "info" / "alternates"
        alternates.parentFile?.mkdirs()
        val mainObjects = (mainGit.gitDir / "objects").canonicalFile
        alternates.writeText("$mainObjects")

        copyConfig(
                "core.fsmonitor",
                "merge.renameLimit",
                "core.splitIndex",
                "feature.manyFiles",
                "index.threads",
                "user.name",
                "user.email",
                "oh-my-zsh.hide-status",
                "oh-my-zsh.hide-dirty",
        )
    }

    private fun copyConfig(vararg keys: String) {
        for (key in keys) {
            val value = mainGit.getConfig(key) ?: continue
            tempGit.setConfig(key, value)
        }
    }

    fun <Y> use(commitToSet: String, action: GitRunner.() -> Y): Y {
        initWorktreeIfNeeded()
        recoverTempGit(commitToSet)
        try {
            return tempGit.run(action)
        } finally {
            recoverTempGit(commitToSet)
        }
    }

    private fun recoverTempGit(commitToSet: String) {
        //trying to fix the repository
        tempGit.execGit(WithNoOutput, timeout = Duration.ofMinutes(15), command = "rebase", args = listOf("--abort"))
        tempGit.execGit(WithNoOutput, timeout = Duration.ofMinutes(15), command = "cherry-pick", args = listOf("--abort"))
        tempGit.execGit(WithNoOutput, timeout = Duration.ofMinutes(15), command = "merge", args = listOf("--abort"))
        tempGit.execGit(WithNoOutput, timeout = Duration.ofMinutes(15), command = "reset", args = listOf("--hard", commitToSet))

        //cleanup stale branches
        val currentBranch = tempGit.listGitCurrentBranchName()
        val branchesToDrop = tempGit.listGitBranches().filter { it != currentBranch }
        tempGit.execGit(WithNoOutput, timeout = Duration.ofMinutes(15), command = "branch", args = listOf("-D") + branchesToDrop)

        //at that point the repo has to be on a commit from the alternates,
        //thus objects is OK to be removed (it's cheaper to kill files, not GC)
        (tempGit.gitDir / "objects").listFiles()
            ?.filter { it.isDirectory && it.name.length == 2 }
            ?.forEach { it.deleteRecursively() }
    }
}
