import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "1.3.60"
  application
}

group = "com.jonnyzzz.teamcity-rr"
version = System.getenv("BUILD_NUMBER") ?: "1.0-SNAPSHOT"

repositories {
  mavenCentral()
  jcenter()
}

dependencies {
  implementation(kotlin("stdlib-jdk8"))

  implementation("org.jetbrains.teamcity:teamcity-rest-client")

  val slfVersion = "1.7.28"
  implementation("org.slf4j:slf4j-api:$slfVersion")
  implementation("org.slf4j:jcl-over-slf4j:$slfVersion")
  implementation("org.slf4j:slf4j-log4j12:$slfVersion")
  implementation("log4j:log4j:1.2.17")


  implementation("com.fasterxml.jackson.core:jackson-core:2.10.0")
  implementation("com.fasterxml.jackson.core:jackson-databind:2.10.0")
  implementation("com.fasterxml.jackson.core:jackson-annotations:2.10.0")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.10.0")
}

tasks.wrapper {
  distributionType = Wrapper.DistributionType.ALL
}

tasks.withType<KotlinCompile> {
  kotlinOptions.jvmTarget = "11"
}

application {
  mainClassName = "com.jonnyzzz.teamcity.rr.RRMain"
}
