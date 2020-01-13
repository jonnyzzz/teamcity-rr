package com.jonnyzzz.teamcity.rr

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.log4j.BasicConfigurator
import org.apache.log4j.Level
import org.apache.log4j.Logger
import org.jetbrains.teamcity.rest.BuildConfigurationId
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.system.exitProcess

const val rrVersion = "0.0.42"
const val teamCityURL = "https://buildserver.labs.intellij.net"
const val customBranchParameter = "reverse.dep.*.intellij.platform.vcs.branch"
const val customParameterMarker = "jonnyzzz.teamcity-rr-build"

val ijAggBuild = BuildConfigurationId("ijplatform_master_Idea_Tests_AggregatorJdk11")


object RRMain {
  private val LOG = Logger.getLogger(javaClass)

  @JvmStatic
  fun main(args: Array<String>) {
    BasicConfigurator.configure()
    Logger.getRootLogger().level = when {
      System.getenv("TEAMCITY_RR_DEBUG") != null -> Level.DEBUG
      else -> Level.INFO
    }

    try {
      theMain(args)
    } catch (e: UserErrorException) {
      LOG.error(e.message, e)
      exitProcess(1)
    } catch (t: Throwable) {
      LOG.error("Unexpected failure: ${t.message}", t)
      exitProcess(2)
    }
  }
}

val WorkDir: File by lazy { File(".").canonicalFile }

class UserErrorException(message: String, cause: Throwable? = null) : Exception(message, cause)

private fun theMain(args: Array<String>) {
  println("TeamCity RR v$rrVersion by @jonnyzzz")
  println()
  println("Running in $WorkDir...")

  if (args.isEmpty()) {
    println("Please select command!")
    exitProcess(11)
  }

  if (args.getOrNull(0).equals("run", ignoreCase = true)) {
    checkGitVersion()
    val branch = createRRBranch()

    val tc = connectToTeamCity()

    val build = tc.buildConfiguration(ijAggBuild)
            .runBuild(
                    parameters = mapOf(
                            customBranchParameter to branch.fullName,
                            customParameterMarker to TeamCityRRState(branch).toParameterString()
                    ),
                    personal = true,
                    queueAtTop = true,
                    logicalBranchName = "teamcity-rr/"
            )
    println("Started build on TeamCity with ID=${build.id}\n${build.getHomeUrl()}\n\n")
  }
}

class TeamCityRRState(
        val branch: RRBranchInfo
) {
  fun toParameterString(): String {
    val om = ObjectMapper()
    val root = om.createObjectNode()
    root.put("user", System.getProperty("user.name"))
    root.put("commit", branch.commit)
    root.put("date", DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now()))
    root.put("full-branch", branch.fullName)
    root.put("short-branch", branch.shortName)
    root.put("local-branch", branch.originalBranchName)
    root.put("rr-version", rrVersion)
    return om.writerWithDefaultPrettyPrinter().writeValueAsString(root)
  }
}
