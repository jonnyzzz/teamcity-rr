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

  implementation("org.jetbrains.teamcity:teamcity-rest-client:1.7.27")
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
