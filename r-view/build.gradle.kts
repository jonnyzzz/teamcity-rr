import java.util.Date

plugins {
    java
    application
    kotlin("jvm")
}

dependencies {
    implementation(project(":common"))
    implementation("com.github.ajalt:mordant:1.2.1")
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
}

val entryClassName = "com.jonnyzzz.teamcity.rr.RViewMain"
val runShowCommand by tasks.creating(JavaExec::class.java) {
    classpath = sourceSets.getByName("main").runtimeClasspath
    main = entryClassName
    args = listOf("show")
    enableAssertions = true
    workingDir = File("/Users/jonnyzzz/Work/intellij")
}

application {
    mainClass.set(entryClassName)
    applicationName = "r-view"
}

val installRoot = File(rootProject.projectDir, "production/r-view")

val installToOs by tasks.creating(Copy::class.java) {
    dependsOn(tasks.distZip)
    group = "distribution"
    from({ zipTree(tasks.distZip.get().archiveFile) })
    into("$installRoot")
    includeEmptyDirs = false
    eachFile { path = path.split("/", limit = 2)[1] }
    doFirst { delete("$buildDir/unpacked") }

    doLast {
        val javaHome = File(System.getProperty("java.home")).canonicalPath
        val script = File("/usr/local/bin/r-view")

        script.writeText("#!/bin/bash\nexport JAVA_HOME='$javaHome'\n$installRoot/bin/${application.applicationName} \"$@\"")
        script.setExecutable(true)

        File(installRoot, "version.txt").writeText(Date().toString())
    }
}

distributions {
    main {
        contents {
            from(File(rootProject.projectDir, "README.md")) {
                into("")
            }
        }
    }
}
