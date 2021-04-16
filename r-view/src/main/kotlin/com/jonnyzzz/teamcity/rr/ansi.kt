package com.jonnyzzz.teamcity.rr

import com.github.ajalt.mordant.TermColors
import com.sun.jna.Function.*
import com.sun.jna.platform.win32.WinDef.BOOL
import com.sun.jna.platform.win32.WinDef.DWORD
import com.sun.jna.platform.win32.WinDef.DWORDByReference
import com.sun.jna.platform.win32.WinNT.HANDLE

private val isWindowsTerminal = System.getenv("WT_SESSION") != null

@Suppress("LocalVariableName")
private fun setupWindowsTerminalLazy() {
    //https://stackoverflow.com/a/52767586
    if (!System.getProperty("os.name").contains("windows", ignoreCase = true)) return

    // Set output mode to handle virtual terminal sequences
    val GetStdHandleFunc = getFunction("kernel32", "GetStdHandle")
    val STD_OUTPUT_HANDLE = DWORD(-11)
    val hOut = GetStdHandleFunc.invoke(HANDLE::class.java, arrayOf<Any>(STD_OUTPUT_HANDLE)) as HANDLE
    val p_dwMode = DWORDByReference(DWORD(0))
    val GetConsoleModeFunc = getFunction("kernel32", "GetConsoleMode")
    GetConsoleModeFunc.invoke(BOOL::class.java, arrayOf<Any>(hOut, p_dwMode))
    val ENABLE_VIRTUAL_TERMINAL_PROCESSING = 4
    val dwMode = p_dwMode.value
    dwMode.setValue((dwMode.toInt() or ENABLE_VIRTUAL_TERMINAL_PROCESSING).toLong())

    val SetConsoleModeFunc = getFunction("kernel32", "SetConsoleMode")
    SetConsoleModeFunc.invoke(BOOL::class.java, arrayOf<Any>(hOut, dwMode))
    //TODO: this may fail, we'd need error handling
}

private val termColors: TermColors
    get() {
        setupWindowsTerminalLazy()
        return when {
            isWindowsTerminal -> TermColors(TermColors.Level.ANSI256)
            //TODO: support IntelliJ console
            else -> TermColors()
        }
    }

fun printProgress(text: String) = println("\n" + termColors.bold(text))
fun printFinalMessage(text: String) = println("\n" + termColors.bold(text))

fun printWithHighlighting(build: TermColors.() -> String) {
    println(termColors.build())
}

val supportsLinks by lazy {
    // https://gist.github.com/egmontkob/eb114294efbcd5adb1944c9f3cb5feda
    if (System.getenv("TERM_PROGRAM") == "iTerm.app") return@lazy true
    // windows terminal, https://stackoverflow.com/a/59734130
    if (isWindowsTerminal) return@lazy true
    //TODO: support Linux and other terminals here
    false
}

fun formatLinkIfSupported(url: String, text: String) : String {
    if (!supportsLinks) return text
    return "\u001B]8;;$url\u001B\\$text\u001B]8;;\u001B\\"
}
