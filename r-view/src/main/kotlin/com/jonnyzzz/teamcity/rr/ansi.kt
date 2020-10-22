package com.jonnyzzz.teamcity.rr

import com.github.ajalt.mordant.TermColors


private val termColors by lazy { TermColors() }
fun printProgress(text: String) = println("\n" + termColors.bold(text))

