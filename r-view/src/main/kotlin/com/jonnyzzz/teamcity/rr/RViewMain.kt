package com.jonnyzzz.teamcity.rr

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
            LOG.error(e.message, e)
            exitProcess(1)
        } catch (t: Throwable) {
            LOG.error("Unexpected failure: ${t.message}", t)
            exitProcess(2)
        }
    }
}

private fun theMain(args: List<String>) {
    println("TeamCity R-View v$rrVersion by @jonnyzzz")
    println()
    println("Running in $WorkDir...")

    if (args.isEmpty()) {
        println("Please select command:")
        println("  up[date]  [--no-fetch]             --- fetch, rebase, show the current state")
        println("  show                               --- shows current status, if possible")
        println("  push <branch> [all|compile]        --- starts a safe-push build")
        println("  rebase <branch> [enable|disable]   --- enabled or disabled a branch from rebase")
        println()
        exitProcess(11)
    }

    when (val cmd = args.getOrNull(0)?.toLowerCase()) {
        "up", "update" -> UpdateCommand.doTheCommand(args)
        "show" -> ShowCommand.doTheCommand(args)
        "safepush", "push" -> StartSafePushCommand.doTheCommand(args)
        "rebase" -> ToggleRebaseMode.doTheCommand(args)
        else -> throw UserErrorException("Unknown command: $cmd")
    }
}

