package com.jonnyzzz.teamcity.rr

import java.io.File
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

fun execWithOutput(workDir: File = WorkDir,
                   timeout: Long,
                   timeoutUnit: TimeUnit,
                   args: List<String>): ProcessResult {
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

  if (runCatching { process.waitFor(timeout, timeoutUnit) }.getOrNull() != true) {
    catchAll { process.destroyForcibly() }
    futures.forEach { it.interrupt() }
    error("Failed to wait for the process to complete!")
  }

  futures.forEach { it.join() }
  return ProcessResult(process.exitValue(), processOutputText.get().trim(), processErrorText.get().trim())
}

fun exec(workDir: File = WorkDir,
         timeout: Long,
         timeoutUnit: TimeUnit,
         args: List<String>) {
  println("Running ${args.toList()}...")

  val process = ProcessBuilder()
          .directory(workDir)
          .command(*args.toTypedArray())
          .inheritIO()
          .redirectInput(ProcessBuilder.Redirect.PIPE)
          .start()

  catchAll { process.outputStream.close() }

  if (!process.waitFor(timeout, timeoutUnit)) {
    catchAll { process.destroyForcibly() }
    error("Failed to wait for the process ${args.toList()} to complete in ${TimeUnit.MINUTES.convert(timeout, timeoutUnit)} minutes")
  }

  val code = process.exitValue()
  println("Command ${args.toList()} exited with code: $code")

  if (code != 0) {
    error("command ${args.toList()} failed with code: $code")
  }
}
