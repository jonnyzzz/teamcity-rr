
plugins {
    java
    kotlin("jvm")
}

dependencies {
    implementation(project(":common"))
}

val runShowCommand by tasks.creating(JavaExec::class.java) {
    classpath = sourceSets.getByName("main").runtimeClasspath
    main = "com.jonnyzzz.teamcity.rr.RViewMain"
    args = listOf("show", "--no-fetch")
    enableAssertions = true
    workingDir = File("/Users/jonnyzzz/Work/intellij")
}
