package com.jonnyzzz.teamcity.rr

import java.time.Duration


fun computeSnapshotFetch(defaultGit: GitRunner) {
    printProgress("Fetching changes from remote...")
    defaultGit.execGit(WithInheritSuccessfully, timeout = Duration.ofMinutes(10),
            command = "fetch",
            args = listOf(
                    "--prune", "--no-tags", "--keep",
                    "origin",
                    "refs/heads/master:refs/remotes/origin/master",
            ))
}
