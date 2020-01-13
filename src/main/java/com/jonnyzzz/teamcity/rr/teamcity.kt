package com.jonnyzzz.teamcity.rr

import org.jetbrains.teamcity.rest.TeamCityInstance
import org.jetbrains.teamcity.rest.TeamCityInstanceFactory
import java.io.File

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
    println("OK!")
    return tc
  } catch (t: Throwable) {
    throw UserErrorException("Failed to connect to TeamCity to $teamCityURL. ${t.message}", t)
  }
}
