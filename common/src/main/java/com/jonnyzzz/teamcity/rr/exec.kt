package com.jonnyzzz.teamcity.rr

import java.io.File
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

data class ProcessResult(val exitCode: Int,
                         val stdout: String,
                         val stderr: String) {

  fun successfully() = when (exitCode) {
    0 -> this
    else -> error("Command failed with code $exitCode")
  }
}

fun <T> execProcess(mode: ProcessExecMode<T>,
                    workDir: File, timeout: Duration, args: List<String>): T = mode.execProcess(workDir, timeout, args)

sealed class ProcessExecMode<T> {
  abstract fun execProcess(workDir: File,
                           timeout: Duration,
                           args: List<String>): T
}

object WithOutput: ProcessExecMode<ProcessResult>() {
  override fun execProcess(workDir: File, timeout: Duration, args: List<String>): ProcessResult {
    val process = ProcessBuilder()
            .directory(workDir)
            .command(args.toList())
            .redirectInput(ProcessBuilder.Redirect.PIPE)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

    catchAll { process.outputStream.close() }

    val processOutputText = AtomicReference<String>()
    val processErrorText = AtomicReference<String>()

    val futures = listOf(
            thread(name = "process-stdin") { processOutputText.set(process.inputStream.bufferedReader().readText()) },
            thread(name = "process-stdout") { processErrorText.set(process.errorStream.bufferedReader().readText()) }
    )

    if (runCatching { process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS) }.getOrNull() != true) {
      catchAll { process.destroyForcibly() }
      futures.forEach { it.interrupt() }
      error("Failed to wait for the process to complete in ${timeout.toMinutes()} minutes")
    }

    futures.forEach { catchAll { it.join() } }
    return ProcessResult(process.exitValue(), processOutputText.get().trim(), processErrorText.get().trim())
  }
}

object WithInherit : ProcessExecMode<Unit>() {
  override fun execProcess(workDir: File, timeout: Duration, args: List<String>) {
    println("Running ${args.toList()}...")

    val process = ProcessBuilder()
            .directory(workDir)
            .command(*args.toTypedArray())
            .inheritIO()
            .redirectInput(ProcessBuilder.Redirect.PIPE)
            .start()

    catchAll { process.outputStream.close() }

    if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
      catchAll { process.destroyForcibly() }
      error("Failed to wait for the process ${args.toList()} to complete in ${timeout.toMinutes()} minutes")
    }

    val code = process.exitValue()
    println("Command ${args.toList()} exited with code: $code")

    if (code != 0) {
      error("command ${args.toList()} failed with code: $code")
    }
  }
}

