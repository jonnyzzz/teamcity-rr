package com.jonnyzzz.teamcity.rr

data class GitSnapshot(
        val masterCommits: Map<String, CommitInfo>,
        val headCommit: String,

        val alreadyMergedBranches: Map<String, String>,
        val rebaseFailedBranches: Map<String, String>,
        val pendingBranches: Map<String, String>,

        val userData: Map<UserDataKey<*>, Any?> = mapOf()
) {

    val masterCommitInfos : Set<CommitInfo> = masterCommits.values.toHashSet()

    fun <T> withUserData(key: UserDataKey<T>, value: T) = copy(
            userData = this.userData + (key as UserDataKey<*> to value as Any)
    )

    operator fun <T : Any> get(key: UserDataKey<T>) : T? = this.userData.get(key)?.let(key::cast)

    fun <T> updateKey(key: UserDataKey<T>, update: (T) -> T?): GitSnapshot {
        val oldValue = this.userData[key]?.let(key::cast) ?: key.default()
        val newValue = update(oldValue)
        if (oldValue == newValue || newValue == null) return this
        return withUserData(key, newValue)
    }
}

class UserDataKey<T>(private val t: Class<T>, val default: () -> T) {
    fun cast(value: Any?): T = t.cast(value)
}

inline fun <reified T : Any> newUserDataKey(noinline default: () -> T) = UserDataKey<T>(T::class.java, default)
