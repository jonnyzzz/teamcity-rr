package com.jonnyzzz.teamcity.rr

import org.apache.log4j.BasicConfigurator
import org.apache.log4j.Level
import org.apache.log4j.Logger
import org.jetbrains.teamcity.rest.Build
import org.jetbrains.teamcity.rest.BuildConfigurationId
import org.jetbrains.teamcity.rest.TestStatus
import java.io.File
import kotlin.system.exitProcess

const val rrVersion = "0.0.42"
const val teamCityURL = "https://buildserver.labs.intellij.net"
const val customBranchParameter = "reverse.dep.*.intellij.platform.vcs.branch"
const val customParameterMarker = "jonnyzzz.teamcity-rr-build"
const val customGitBranchNamePrefix = "refs/jonnyzzz-rr"
const val customTeamCityBranchNamePrefix = "jonnyzzz-rr/"
const val customTeamCityTagName = "jonnyzzz-rr"

val ijAggBuild = BuildConfigurationId("ijplatform_master_Idea_Tests_AggregatorJdk11")

val WorkDir: File by lazy { File(".").canonicalFile }
class UserErrorException(message: String, cause: Throwable? = null) : Exception(message, cause)

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

private fun theMain(args: Array<String>) {
  println("TeamCity RR v$rrVersion by @jonnyzzz")
  println()
  println("Running in $WorkDir...")

  if (args.isEmpty()) {
    println("Please select command!")
    exitProcess(11)
  }

  val cmd = args.getOrNull(0)?.toLowerCase()
  when {
    cmd == "run" -> startNewBuild()
    cmd == "show" -> showPendingBuilds()
  }
}

private fun startNewBuild() {
  checkGitVersion()
  val branch = createRRBranch()
  val tc = connectToTeamCity()

  val build = tc.buildConfiguration(ijAggBuild)
          .runBuild(
                  parameters = mapOf(
                          customBranchParameter to branch.fullName,
                          customParameterMarker to TeamCityRRState.toParameterString(branch)
                  ),
                  personal = true,
                  queueAtTop = true,
                  logicalBranchName = "$customTeamCityBranchNamePrefix${branch.shortName}"
          )
  println("Started build on TeamCity with ID=${build.id}\n${build.getHomeUrl()}\n\n")
  build.addTag(customTeamCityTagName)
}

private fun Sequence<Build>.teamcityRRBuilds() = this
        .filter { it.buildConfigurationId == ijAggBuild }
        .filter { it.parameters.any { it.name == customParameterMarker } }

private fun showPendingBuilds() {
  val tc = connectToTeamCity()

  val ourProjectId = tc.buildConfiguration(ijAggBuild).projectId

  val allBuilds = (
          tc.buildQueue()
                  .queuedBuilds(ourProjectId /*TODO: implement per configuration REST API CALL*/)
                  +
                  tc.builds()
                          .fromConfiguration(ijAggBuild)
                          .withAllBranches()
                          .onlyPersonal()
                          .includeRunning()
                          .includeFailed()
                          .all()
          ).teamcityRRBuilds()

  allBuilds.forEach { build ->
    println("${build.id} in branch ${build.branch.name}. ${build.runningInfo?.percentageComplete ?: "??"}%. ${build.status} ${build.statusText} ")
    val params = TeamCityRRState.loadFromBuild(build)
    println("  " + build.getHomeUrl())
    println("  RemoteRun for ${params.originalBranchName} @ ${params.commit} running as ${params.fullName}")
    println()

    build.testRuns(TestStatus.FAILED)
            .filter { !it.muted }
            .filter { !it.currentlyMuted }
            .filter { !it.ignored }
            .forEach {
              println("  " + it.name + "  FAILED")
            }
    println()
    println()
  }
}
