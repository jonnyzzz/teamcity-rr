
plugins {
    kotlin("jvm")
}

dependencies {
    api(kotlin("stdlib-jdk8"))

    api(kotlin("stdlib-jdk8"))

    api("org.jetbrains.teamcity:teamcity-rest-client")

    val slfVersion = "1.7.28"
    api("org.slf4j:slf4j-api:$slfVersion")
    implementation("org.slf4j:jcl-over-slf4j:$slfVersion")
    implementation("org.slf4j:slf4j-log4j12:$slfVersion")
    implementation("log4j:log4j:1.2.17")


    api("com.fasterxml.jackson.core:jackson-core:2.11.3")
    api("com.fasterxml.jackson.core:jackson-databind:2.11.3")
    api("com.fasterxml.jackson.core:jackson-annotations:2.11.3")
    api("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.3")

    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.3.9")
}
