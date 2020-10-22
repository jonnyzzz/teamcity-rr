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
        println("  show --- lists all pending safe push branches")
        println()
        exitProcess(11)
    }

    when (val cmd = args.getOrNull(0)?.toLowerCase()) {
        "show" -> ShowCommand.doTheCommand(args)
        "safepush" -> StartSafePushCommand.doTheCommand(args)
        else -> throw UserErrorException("Unknown command: $cmd")
    }
}

