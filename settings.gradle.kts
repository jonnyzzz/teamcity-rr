rootProject.name = "teamcity-rr"

val restClient = File(rootProject.projectDir, "teamcity-rest-client")


if (!restClient.isDirectory) {
    println()
    println()
    println("TeamCity Rest Client is missing, please checkout the ")
    println("https://github.com/jonnyzzz/teamcity-rest-client")
    println("and use the `teamcity-rr` branch")
    println(restClient)
    println()
    println()
    error("See above")
}

includeBuild(restClient) {
    dependencySubstitution {
        substitute(module("org.jetbrains.teamcity:teamcity-rest-client")).with(project(":"))
    }
}

include("common")
include("rr")
include("r-view")
