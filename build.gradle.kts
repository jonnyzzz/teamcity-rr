@file:Suppress("HasPlatformType", "PropertyName")

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "1.4.10"
  id("de.undercouch.download") version "4.0.4"
  application
}

group = "com.jonnyzzz.teamcity-rr"
version = System.getenv("BUILD_NUMBER") ?: "1.0-SNAPSHOT"

allprojects {
  group = "com.jonnyzzz.teamcity-rr"
  version = System.getenv("BUILD_NUMBER") ?: "1.0-SNAPSHOT"

  repositories {
    mavenCentral()
    jcenter()
  }
}

dependencies {
  implementation(kotlin("stdlib-jdk8"))
  implementation(project(":rr"))
  implementation(project(":r-view"))
}

tasks.wrapper {
  distributionType = Wrapper.DistributionType.ALL
}

allprojects {
  tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
    kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlinx.coroutines.ExperimentalCoroutinesApi"
  }
}

application {
  mainClass.set("com.jonnyzzz.teamcity.rr.RRMain")
}

val distUnpacked by tasks.creating(Sync::class.java) {
  dependsOn(tasks.distZip)
  group = "distribution"
  from({ zipTree(tasks.distZip.get().archiveFile) })
  into("$buildDir/unpacked")
  includeEmptyDirs = false
  eachFile { path = path.split("/", limit = 2)[1] }
  doFirst { delete("$buildDir/unpacked") }
}

fun JavaExec.copyFromApplicationRun(vararg extraArgs: String) {
  val runTask = tasks.getByName<JavaExec>("run")
  dependsOn(tasks.classes)
  group = runTask.group
  enableAssertions = true

  doFirst {
    classpath = runTask.classpath
    main = runTask.main

    @Suppress("UNNECESSARY_SAFE_CALL", "USELESS_ELVIS")
    args = (runTask.args?.toList() ?: listOf()) + (args?.toList() ?: listOf()) + extraArgs
  }
}

val updateSources by tasks.creating(Exec::class.java) {
  workingDir(rootProject.rootDir)
  commandLine("git", "pull")
}

val `rr-run` by tasks.creating(JavaExec::class) {
  dependsOn(updateSources)
  copyFromApplicationRun("run")
  group = "teamcity-rr"
}

val `rr-show` by tasks.creating(JavaExec::class) {
  dependsOn(updateSources)
  copyFromApplicationRun("show")
  group = "teamcity-rr"
}

val teamcity by tasks.creating {
  dependsOn(tasks.distZip)

  doLast {
    println(" ##teamcity[publishArtifacts '${tasks.distZip.get().archiveFile.get().asFile}'] ")
  }
}

distributions {
  main {
    contents {
      from("README.md") {
        into("")
      }
    }
  }
}
