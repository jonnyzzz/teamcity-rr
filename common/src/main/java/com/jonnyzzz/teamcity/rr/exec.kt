package com.jonnyzzz.teamcity.rr

import java.io.File
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

open class ProcessCode(val args: List<String>,
                  val exitCode: Int) {
  open fun successfully() = when (exitCode) {
    0 -> this
    else -> error("Command failed with code $exitCode: " + args.joinToString(" ") { "'$it'" } + stderrMessage())
  }

  protected open fun stderrMessage() = ""
}

class ProcessResult(args: List<String>,
                    exitCode: Int,
                    val stdout: String,
                    val stderr: String) : ProcessCode(args, exitCode) {

  override fun successfully() = super.successfully() as ProcessResult

  override fun stderrMessage() = "\n" + stderr
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
    return ProcessResult(args, process.exitValue(), processOutputText.get().trim(), processErrorText.get().trim())
  }
}

object WithInheritSuccessfully : ProcessExecMode<Unit>() {
  override fun execProcess(workDir: File, timeout: Duration, args: List<String>) {
    WithInherit.execProcess(workDir, timeout, args).successfully()
  }
}

object WithNoOutputSuccessfully : ProcessExecMode<Unit>() {
  override fun execProcess(workDir: File, timeout: Duration, args: List<String>) {
    WithOutput.execProcess(workDir, timeout, args).successfully()
  }
}

object WithNoOutput : ProcessExecMode<Unit>() {
  override fun execProcess(workDir: File, timeout: Duration, args: List<String>) {
    WithOutput.execProcess(workDir, timeout, args)
  }
}

object WithInherit : ProcessExecMode<ProcessCode>() {
  override fun execProcess(workDir: File, timeout: Duration, args: List<String>) : ProcessCode {

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
    return ProcessCode(args, code)
  }
}

