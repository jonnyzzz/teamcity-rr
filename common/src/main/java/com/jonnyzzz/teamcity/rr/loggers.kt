package com.jonnyzzz.teamcity.rr

import org.apache.log4j.BasicConfigurator
import org.apache.log4j.Level
import org.apache.log4j.Logger

fun setupLoggers() {
    BasicConfigurator.configure()
    Logger.getRootLogger().level = when {
        System.getenv("TEAMCITY_RR_DEBUG") != null -> Level.DEBUG
        else -> Level.INFO
    }
}
