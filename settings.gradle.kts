rootProject.name = "teamcity-rr"

val teamcityRestHome = File(rootProject.projectDir, "../teamcity-rest-client")

if (System.getenv("TEAMCITY_VERSION") == null && teamcityRestHome.isDirectory) {

  includeBuild(teamcityRestHome) {
    dependencySubstitution {
       substitute(module("org.jetbrains.teamcity:teamcity-rest-client")).with(project(":"))
    }
  }
}


