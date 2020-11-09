package com.jonnyzzz.teamcity.rr

import com.jonnyzzz.teamcity.rr.commands.*
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

private const val rrVersion = "0.0.42"
private val LOG = LoggerFactory.getLogger(RViewMain::class.java)

object RViewMain {
    @JvmStatic
    fun main(args: Array<String>) {
        setupLoggers()

        try {
            theMain(args.toList())
        } catch (e: UserErrorException) {
            LOG.error(e.message)
            exitProcess(1)
        } catch (t: Throwable) {
            LOG.error("Unexpected failure: ${t.message}", t)
            exitProcess(2)
        }
    }
}

private fun theMain(allArgs: List<String>) {
    println("TeamCity R-View v$rrVersion by @jonnyzzz")
    println()
    println("Running in $WorkDir...")

    if (allArgs.isEmpty()) {
        println("Please select command:")
        println("  up[date] [branch] [--no-fetch]          --- fetch, rebase, show the current state")
        println("  show [branch]                           --- shows current status, if possible")
        println("  push <branch> all                       --- starts a safe-push build with all tests, push the branch")
        println("  push <branch> compile                   --- starts a safe-push build with compile, push the branch")
        println("  push <branch> [local]                   --- push the branch")
        println("  push-now-master <branch>                --- rebase & push current branch to master")
        println()
        println("  auto-rebase <branch> [enable|disable]   --- enabled or disabled a branch from rebase")
        println("  delete <branch>                         --- removes local and remote branch")
        println("  reset <branch>                          --- removes all collected of the given branch")
        println("  new <branch>                            --- creates new branch with the given name, stashes current work, switches")
        println()
        exitProcess(11)
    }

    val cmd = allArgs.first().toLowerCase()
    val args = allArgs.drop(1)

    when (cmd) {
        "up", "update" -> UpdateCommand.doTheCommand(args)
        "show" -> ShowCommand.doTheCommand(args)
        "push" -> SafePushCommand.doTheCommand(args)
        "push-now-master" -> PushNowToMasterCommand.doTheCommand(args)
        "auto-rebase" -> ToggleRebaseMode.doTheCommand(args)
        "delete" -> DeleteBranchCommand.doTheCommand(args)
        "reset" -> ResetBranchCommand.doTheCommand(args)
        "new" -> NewBranchCommand.doTheCommand(args)
        else -> throw UserErrorException("Unknown command: $cmd")
    }
}

