package com.jonnyzzz.teamcity.rr

import com.fasterxml.jackson.databind.ObjectMapper
import org.jetbrains.teamcity.rest.Build
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object TeamCityRRState {
    fun loadFromBuild(build: Build): RRBranchInfo {
        val state = build.parameters.first { it.name == customParameterMarker }
        val om = ObjectMapper()
        val root = om.readTree(state.value)
        return RRBranchInfo(
                fullName =  root.get("full-branch").asText(),
                shortName = root.get("short-branch").asText(),
                commit = root.get("commit").asText(),
                originalBranchName = root.get("local-branch").asText(),
                //TODO: is it the same configuration as we had?
                targetBuildConfigurationId = build.buildConfigurationId
        )
    }

    fun toParameterString(branch: RRBranchInfo): String {
        val om = ObjectMapper()
        val root = om.createObjectNode()
        root.put("user", System.getProperty("user.name"))
        root.put("commit", branch.commit)
        root.put("date", DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now()))
        root.put("full-branch", branch.fullName)
        root.put("short-branch", branch.shortName)
        root.put("local-branch", branch.originalBranchName)
        root.put("rr-version", rrVersion)
        return om.writerWithDefaultPrettyPrinter().writeValueAsString(root)
    }
}
