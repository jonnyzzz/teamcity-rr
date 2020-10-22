@file:Suppress("UnstableApiUsage")

package com.jonnyzzz.teamcity.rr

import okio.ByteString.Companion.encodeUtf8
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*
import java.nio.file.Files
import java.nio.file.StandardCopyOption

// some of our operations are time consuming,
// e.g. require to download files or validate checksums
// we use local cache and remote Redis to speed up
// both the local experience and TeamCity builds
//
// this is adjusted code from Toolbox FeedGenerator,

fun getOrComputeFromCacheLong(key: String, action: () -> Long): Long {
  return getOrComputeFromCacheT(key + "_LONG", { it.toString() }, { it.toLong() }) { action() }
}

fun getOrComputeFromCache(key: String, action: () -> String) = getOrComputeFromCacheT(key, { it }, { it }, action)

private fun <T : Any> getOrComputeFromCacheT(key: String,
                                             serialize: (T) -> String,
                                             deserialize: (String) -> T,
                                             action: () -> T): T {
  val fullKey = "RRs_v1_$key"
  val result = PersistentCache.get(fullKey)

  if (result != null) {
    try {
      return deserialize(result)
    } catch (e: Throwable) {
      LoggerFactory.getLogger(PersistentCache::class.java).error("Failed to deserialize: $fullKey! ${e.message}", e)
    }
  }

  val computed = action()
  PersistentCache.set(fullKey, serialize(computed))
  return computed
}

fun String.sha256() = this.encodeUtf8().sha256().hex().toLowerCase()

private object PersistentCache {
  private val LOG = LoggerFactory.getLogger(javaClass)

  // Strategy: remove unused keys after 7 days automatically
  private const val redisKeyExpireInSeconds = 3600 * 24 * 7

  private val localCache = mutableMapOf<String, String>()

  private val diskCachePath get() = DiskCaches.redisCacheDir

  private fun diskCacheKey(key: String): String = key.sha256()

  private fun diskCacheGet(key: String): String? {
    val diskCacheFile = diskCachePath / diskCacheKey(key)
    if (!diskCacheFile.exists()) return null
    return Base64.getDecoder().decode(diskCacheFile.readText()).toString(Charsets.UTF_8)
  }

  private fun diskCachePut(key: String, value: String) {
    val diskCacheFile = diskCachePath / diskCacheKey(key)
    diskCacheFile.parentFile?.mkdirs()
    val diskCacheFileTmp = diskCacheFile + "tmp${System.currentTimeMillis()}"
    diskCacheFileTmp.writeText(Base64.getEncoder().encodeToString(value.toByteArray(Charsets.UTF_8)))
    Files.move(diskCacheFileTmp.toPath(), diskCacheFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
  }

  fun get(key: String): String? {
    val localValue = localCache[key]
    if (localValue != null) return localValue

    val diskValue = diskCacheGet(key)
    if (diskValue != null) {
      localCache[key] = diskValue
      return diskValue
    }

    return null
  }

  fun set(key: String, value: String) {
    LOG.info("PersistentCache: SET $key <- ${value.cutTooLongString()}")
    localCache[key] = value
    diskCachePut(key, value)
  }
}

object DiskCaches {
  private val baseCachePath by lazyDir {
    val path = File(WorkDir, ".git/teamcity-rr-cache").canonicalFile
    println("Using cache dir: $path")
    path
  }

  val branchesCacheDir by lazyDir { baseCachePath / "branches" }

  val redisCacheDir by lazyDir {
    baseCachePath / "kv-cache"
  }

  private fun lazyDir(action: () -> File) = lazy<File> {
    val file = action()
    file.mkdirs()
    if (file.isFile) throw Error("Cannot create directory: $file, same file is already created")
    if (!file.isDirectory) throw Error("Cannot create directory: $file.")
    file.canonicalFile
  }
}

private fun String.cutTooLongString(): String = if (length > 512) (substring(0, 512) + "\nWARN! (and more, total length = $length)") else this
