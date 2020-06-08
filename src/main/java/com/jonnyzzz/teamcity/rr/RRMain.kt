package com.jonnyzzz.teamcity.rr

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.collect
import org.apache.log4j.BasicConfigurator
import org.apache.log4j.Level
import org.apache.log4j.Logger
import org.jetbrains.teamcity.rest.*
import java.io.File
import kotlin.sequences.filter
import kotlin.sequences.forEach
import kotlin.system.exitProcess

const val rrVersion = "0.0.42"
const val teamCityURL = "https://buildserver.labs.intellij.net"
const val customBranchParameter = "reverse.dep.*.intellij.platform.vcs.branch"
const val customParameterMarker = "jonnyzzz.teamcity-rr-build"
const val customGitBranchNamePrefix = "refs/jonnyzzz-rr"
const val customTeamCityBranchNamePrefix = "jonnyzzz-rr/"
const val customTeamCityTagName = "jonnyzzz-rr"

val ijAggPerGitBranch = mapOf(
        BuildConfigurationId("ijplatform_master_Idea_Tests_AggregatorJdk11") to "master",
        BuildConfigurationId("ijplatform_IjPlatform201_Idea_Tests_AggregatorJdk11") to "201"
)

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
    println("Please select command:")
    println("  run  --- sends the current repository commit to RR")
    println("  show --- lists all pending remote runs")
    println()
    exitProcess(11)
  }

  when (val cmd = args.getOrNull(0)?.toLowerCase()) {
      "run" -> startNewBuild()
      "show" -> showPendingBuilds()
      else -> error("Unknown command: $cmd")
  }

  exitProcess(1)
}

private fun startNewBuild() {
  checkGitVersion()
  val branch = createRRBranch()
  val tc = connectToTeamCity()

  val build = tc.buildConfiguration(branch.targetBuildConfigurationId)
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

private fun Sequence<Build>.teamcityRRBuilds(scope: ProducerScope<Build>) = with(scope) {
  for (it in this@teamcityRRBuilds) {
    launch(Dispatchers.IO) {
      if (!ijAggPerGitBranch.containsKey(it.buildConfigurationId)) return@launch
      if (!it.parameters.any { it.name == customParameterMarker }) return@launch
      send(it)
    }
  }
}

private fun showPendingBuilds() = runBlocking {
  val tc = connectToTeamCity()

  val allBuilds = channelFlow<Build> {
    withContext(Dispatchers.IO) {
      launch {
        tc.buildQueue()
                .queuedBuilds() //TODO: implement per-build-type filter
                .teamcityRRBuilds(this@channelFlow)
      }

      ijAggPerGitBranch.keys.forEach { buildConfigurationId ->
        launch {
          tc.builds()
                  .fromConfiguration(buildConfigurationId)
                  .withAllBranches()
                  .onlyPersonal()
                  .includeRunning()
                  .includeFailed()
                  .limitResults(16)
                  .all()
                  .teamcityRRBuilds(this@channelFlow)
        }
      }
    }
  }

  coroutineScope {
    allBuilds.collect { build ->
      launch(Dispatchers.Default) {
        processOneBuild(tc, build)
      }
    }
  }
}

private fun CoroutineScope.loadFailedTestsAsync(build: Build) = async(Dispatchers.IO) {
  build.testRuns(TestStatus.FAILED).asFlow().flowOn(Dispatchers.IO)
          .filter { !it.muted }
          .filter { !it.currentlyMuted }
          .filter { !it.ignored }
          .toList(ArrayList())
          .sortedBy { it.name }
}

private fun CoroutineScope.resolveNearestMasterBuildAsync(tc: TeamCityInstance,
                                                          params: RRBranchInfo) = async(Dispatchers.IO) {
  val commits = runCatching {
    listGitCommits(params, 2048)
  }.getOrElse {
    return@async null
  }.toSet()

  val masterBuild = tc.builds()
          .fromConfiguration(params.targetBuildConfigurationId)
          .includeFailed()
          .all()
          .firstOrNull { it.revisions.any { rev -> commits.contains(rev.version) } }

  masterBuild ?: return@async null
  masterBuild to loadFailedTestsAsync(masterBuild)
}

private suspend fun processOneBuild(tc: TeamCityInstance, build: Build) = coroutineScope {
  val logMessage = buildString {
    buildMessage(tc, build)
  }

  println(logMessage)
}

private fun StringBuilder.appendFailures(name: String, data: List<TestRun>) {
  if (data.isNotEmpty()) {
    appendln("  $name:")

    val testBySuite = data.groupBy { it.name.split(":").first() }
    for ((suite, tests) in testBySuite) {
      appendln("    $suite:")
      tests.map { it.name.removePrefix("$suite:").trim() }.sortedWith(String.CASE_INSENSITIVE_ORDER).forEach {
        appendln("      $it")
      }
      appendln()
    }
  } else {
    appendln("  $name: NONE!")
  }

  appendln()
}

private suspend fun StringBuilder.buildMessage(tc: TeamCityInstance, build: Build) = coroutineScope {
  appendln("${build.id} in branch ${build.branch.name}. ${build.runningInfo?.percentageComplete ?: "??"}%. ${build.status} ${build.statusText} ")
  appendln("started on " + build.startDateTime + ", finished " + build.finishDateTime)
  val params = TeamCityRRState.loadFromBuild(build)
  appendln("  " + build.getHomeUrl())
  appendln("  RemoteRun for ${params.originalBranchName} @ ${params.commit} running as ${params.fullName}")
  appendln()

  val ourFailedTestsAsync = loadFailedTestsAsync(build)
  val masterFailedAndTestsAsync = resolveNearestMasterBuildAsync(tc, params)
  val ourFailedTests = ourFailedTestsAsync.await()
  val masterBuildAndFailedTests = masterFailedAndTestsAsync.await()

  if (masterBuildAndFailedTests != null) {
    val (masterBuild, masterFailedTestsAsync) = masterBuildAndFailedTests
    val masterFailedTests = masterFailedTestsAsync.await()

    appendln("  Comparing results with build #${masterBuild.id}")
    appendln("     " + masterBuild.getHomeUrl())
    appendln()

    val masterFailedTestNames = masterFailedTests.map { it.name }.toSet()
    val (newFailed, justFailed) = run {
      val group = ourFailedTests.groupBy { masterFailedTestNames.contains(it.name) }
      (group[false]?: listOf()) to (group[true] ?: listOf())
    }

    appendFailures("New tests Failures", newFailed)
    appendFailures("Other tests Failures", justFailed)

  } else {
    appendln("  Failed to find a Master build to compare results")
    appendFailures("Tests Failures", ourFailedTests)
  }

  appendln()
  appendln()
}
