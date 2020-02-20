import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "1.3.60"
  id("de.undercouch.download") version "4.0.4"
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

  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.3")
}

tasks.wrapper {
  distributionType = Wrapper.DistributionType.ALL
}

tasks.withType<KotlinCompile> {
  kotlinOptions.jvmTarget = "11"
  kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlinx.coroutines.ExperimentalCoroutinesApi"
}

application {
  mainClassName = "com.jonnyzzz.teamcity.rr.RRMain"
}

val distUnpacked by tasks.creating(Sync::class.java) {
  dependsOn(tasks.distZip)
  group = "distribution"
  from({ zipTree(tasks.distZip.get().archiveFile) })
  into("$buildDir/unpacked")
  includeEmptyDirs = false
  eachFile { path = path.split("/", limit = 2)[1] }
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

tasks.classes.configure {
  dependsOn(updateSources)
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

val teamcity by tasks.creating() {
  dependsOn(tasks.distZip)
}
