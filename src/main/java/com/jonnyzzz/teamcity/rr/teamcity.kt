package com.jonnyzzz.teamcity.rr

import com.fasterxml.jackson.databind.ObjectMapper
import org.jetbrains.teamcity.rest.TeamCityInstance
import org.jetbrains.teamcity.rest.TeamCityInstanceFactory
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private fun readTeamCityAccessToken(): String {
  val tokenFile = File(System.getProperty("user.home"), ".teamcity-rr")
  try {
    return tokenFile.readText().trim()
  } catch (t: Throwable) {
    throw UserErrorException("" +
            "TeamCity-RR tool uses TeamCity access tokens to do TeamCity API requests on your behalf. " +
            "The token was not found with the error:\n${t.message}\n\n" +
            "Configure your TeamCity access token and save it to the $tokenFile.\n" +
            "See more info under\nhttps://www.jetbrains.com/help/teamcity/managing-your-user-account.html#Managing-Access-Tokens")

  }
}

fun connectToTeamCity(): TeamCityInstance {
  val token = readTeamCityAccessToken()

  println("Checking connection to TeamCity...")
  try {
    val tc = TeamCityInstanceFactory.tokenAuth(
            teamCityURL,
            token
    )
    //test connection
    tc.rootProject()
    return tc
  } catch (t: Throwable) {
    throw UserErrorException("Failed to connect to TeamCity to $teamCityURL. ${t.message}", t)
  }
}

class TeamCityRRState(
        private val branch: RRBranchInfo
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
