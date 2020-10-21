package com.jonnyzzz.teamcity.rr

import java.io.File
import java.nio.file.Path

inline fun catchAll(action: () -> Unit) {
  try {
    action()
  }
  catch (t: Throwable) {
    //NOP
  }
}

operator fun File.div(path: String) = File(this, path)

operator fun Path.div(path: String): Path = resolve(path)

val WorkDir: File by lazy { File(".").canonicalFile }

