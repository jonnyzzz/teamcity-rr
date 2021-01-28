package com.jonnyzzz.teamcity.rr

import com.github.ajalt.mordant.TermColors

private val isWindowsTerminal = System.getenv("WT_SESSION") != null

private val termColors by lazy {
    when {
//        isWindowsTerminal -> TermColors(TermColors.Level.ANSI16)
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
