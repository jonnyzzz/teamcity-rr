package com.jonnyzzz.teamcity.rr

import com.github.ajalt.mordant.TermColors


private val termColors by lazy { TermColors() }
fun printProgress(text: String) = println("\n" + termColors.bold(text))
fun printFinalMessage(text: String) = println("\n" + termColors.bold(text))

fun printWithHighlighting(build: TermColors.() -> String) {
    println(termColors.build())
}

val supportsLinks by lazy {
    // https://gist.github.com/egmontkob/eb114294efbcd5adb1944c9f3cb5feda
    if (System.getenv("TERM_PROGRAM") == "iTerm.app") return@lazy true
    //TODO: support Linux and other terminals here
    false
}

fun formatLinkIfSupported(url: String, text: String) : String {
    if (!supportsLinks) return text
    return "\u001B]8;;$url\u001B\\$text\u001B]8;;\u001B\\"
}
