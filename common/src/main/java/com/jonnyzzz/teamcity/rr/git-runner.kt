package com.jonnyzzz.teamcity.rr

import java.io.File
import java.time.Duration

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
                    args: List<String> = listOf()): T = execProcess(mode,
            workDir = workdir,
            timeout = timeout,
            args = listOf(GIT_COMMAND) +
                    (if (bare) listOf() else listOf("--git-dir=$gitDir")) +
                    command + args
    )

    fun throwFailedToExecuteGit(): Nothing =
            throw UserErrorException("Failed to execute `$GIT_COMMAND version` command. " +
                    "Please check you have configured git in system path or set the " +
                    "`$ENV_GIT_COMMAND` environment variable with the correct path.")


    @PublishedApi
    internal val gitWorktreeBase: GitWorktreeBase by lazy {
        val wt = GitWorktreeBase(this@GitRunner)
        wt.initWorktreeIfNeeded()
        wt
    }

    inline fun <Y> useWorktree(action: GitRunner.() -> Y): Y {
        return synchronized(gitWorktreeBase) {
            gitWorktreeBase.use(action)
        }
    }
}

class GitWorktreeBase(
        private val mainGit: GitRunner
) {
    val tempGit = GitRunner((mainGit.workdir.parentFile / (mainGit.workdir.name + "-rebase-helper")).absoluteFile)

    fun initWorktreeIfNeeded() {
        tempGit.run {
            if (!isValidGitCheckout) {
                workdir.mkdirs()
                execGit(WithNoOutputSuccessfully, bare = true, timeout = Duration.ofSeconds(5), command = "init")
            }

            //it's worth to rewrite alternates, just in case
            val alternates = gitDir / "objects" / "info" / "alternates"
            alternates.parentFile?.mkdirs()
            val mainObjects = (mainGit.gitDir / "objects").canonicalFile
            alternates.writeText("$mainObjects")

            copyConfig(
                    "core.fsmonitor",
                    "merge.renameLimit",
                    "core.splitIndex",
                    "feature.manyFiles",
                    "index.threads"
            )

            println("Temp Git repo for merges it ready at $tempGit")
        }
    }

    private fun copyConfig(vararg keys: String) {
        for (key in keys) {
            val value = mainGit.getConfig(key) ?: continue
            tempGit.setConfig(key, value)
        }
    }

    inline fun <Y> use(action: GitRunner.() -> Y): Y {
        initWorktreeIfNeeded()
        try {
            return tempGit.run(action)
        } catch (t: Throwable) {
            //trying to fix the repository
            tempGit.execGit(WithInherit, timeout = Duration.ofMinutes(15), command = "rebase", args = listOf("--abort"))
            tempGit.execGit(WithInherit, timeout = Duration.ofMinutes(15), command = "cherry-pick", args = listOf("--abort"))
            tempGit.execGit(WithInherit, timeout = Duration.ofMinutes(15), command = "merge", args = listOf("--abort"))
            tempGit.execGit(WithInherit, timeout = Duration.ofMinutes(15), command = "reset", args = listOf("--hard"))
            throw t
        }
    }
}
