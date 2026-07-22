#!/usr/bin/env kotlin
@file:Import("../../lib.main.kts")

import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.pathString
import kotlin.io.path.readText
import kotlin.io.path.writeText

val majorFormat = System.getenv("INPUT_MAJOR")?.takeIf { it.isNotBlank() } ?: "yyyy"
val minorFormat = System.getenv("INPUT_MINOR")?.takeIf { it.isNotBlank() } ?: "MM"
val microFormat = System.getenv("INPUT_MICRO")?.takeIf { it.isNotBlank() } ?: "patch"

val tagPrefix = System.getenv("INPUT_TAG_PREFIX") ?: "v"
val extraFilesInput = System.getenv("INPUT_EXTRA_FILES") ?: ""
val artifactsInput = System.getenv("INPUT_ARTIFACTS") ?: ""
val commitMessageTemplate =
    System.getenv("INPUT_COMMIT_MESSAGE")?.takeIf { it.isNotBlank() } ?: "chore(release): {tag}"

val now: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC)

fun formatCalverPart(pattern: String): String {
  return try {
    DateTimeFormatter.ofPattern(pattern).format(now)
  } catch (_: Exception) {
    pattern
  }
}

fun fetchExistingTags(): List<String> {
  exec(ignoreExitCode = true) {
    command("git")
    args("fetch", "--tags")
  }
  val output =
      exec(ignoreExitCode = true) {
        command("git")
        args("tag", "-l")
      }
  return output.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
}

fun calculateNextMicroVersion(
    existingTags: List<String>,
    prefix: String,
    major: String,
    minor: String,
): Int {
  val prefixAndPeriod = "$prefix$major.$minor."
  var maxMicro = -1

  for (tag in existingTags) {
    if (tag.startsWith(prefixAndPeriod)) {
      val microPart = tag.substringAfter(prefixAndPeriod)
      val microVal = microPart.toIntOrNull()
      if (microVal != null && microVal > maxMicro) {
        maxMicro = microVal
      }
    }
  }
  return maxMicro + 1
}

val majorVersion = formatCalverPart(majorFormat)
val minorVersion = formatCalverPart(minorFormat)

val isMicroPatch =
    microFormat.equals("patch", ignoreCase = true) || microFormat.equals("micro", ignoreCase = true)
val existingTags = fetchExistingTags()

val microVersion =
    if (isMicroPatch) {
      calculateNextMicroVersion(existingTags, tagPrefix, majorVersion, minorVersion).toString()
    } else {
      formatCalverPart(microFormat)
    }

val versionStr = "$majorVersion.$minorVersion.$microVersion"
val fullTag = "$tagPrefix$versionStr"

println("Calculated CalVer Version: $versionStr")

println("Calculated Full Tag: $fullTag")

fun updateExtraFiles(filesInput: String, newVersion: String): List<String> {
  val files = filesInput.split(Regex("[\\r\\n,]+")).map { it.trim() }.filter { it.isNotEmpty() }

  val modifiedFiles = mutableListOf<String>()

  val versionRegexPatterns =
      listOf(
          Regex("""(?i)(version\s*=\s*)(["']?)[^"'\r\n]+(["']?)""") to "$1$2$newVersion$3",
          Regex("""(?i)(version\s*:\s*)(["']?)[^"'\r\n]+(["']?)""") to "$1$2$newVersion$3",
          Regex("""(?i)("version"\s*:\s*")([^"]+)(")""") to "$1$newVersion$3",
      )

  for (path in files) {
    val file = Path(path)
    if (file.exists() && file.isRegularFile()) {
      var content = file.readText()
      var isChanged = false
      for ((regex, replacement) in versionRegexPatterns) {
        if (regex.containsMatchIn(content)) {
          content = regex.replace(content, replacement)
          isChanged = true
        }
      }
      if (isChanged) {
        file.writeText(content)
        modifiedFiles.add(file.pathString)
        println("Updated version in file: ${file.pathString}")
      }
    } else {
      println("Warning: File not found: $path")
    }
  }
  return modifiedFiles
}

val modifiedFiles = updateExtraFiles(extraFilesInput, versionStr)

if (modifiedFiles.isNotEmpty()) {
  exec {
    command("git")
    args("add", *modifiedFiles.toTypedArray())
  }
  val commitMsg = commitMessageTemplate.replace("{tag}", fullTag).replace("{version}", versionStr)
  exec {
    command("git")
    args("commit", "--allow-empty", "-m", commitMsg)
  }
  println("Committed version updates to git.")
}

exec {
  command("git")
  args("tag", "-a", fullTag, "-m", "Release $fullTag")
}

println("Created git tag: $fullTag")

exec(ignoreExitCode = true) {
  command("git")
  args("push", "origin", fullTag)
}

println("Pushed git tag: $fullTag to remote.")

fun createGitHubRelease(tag: String, artifactsRaw: String) {
  val artifactPaths =
      artifactsRaw
          .split(Regex("[\\r\\n,]+"))
          .map { it.trim() }
          .filter { it.isNotEmpty() }
          .filter { Path(it).exists() }

  val ghArgs = mutableListOf("release", "create", tag, "--generate-notes", "--title", tag)
  ghArgs.addAll(artifactPaths)

  try {
    exec {
      command("gh")
      args(*ghArgs.toTypedArray())
    }
    println("Created GitHub Release: $tag")
  } catch (e: Exception) {
    println("Notice: GitHub Release creation step ended: ${e.message}")
  }
}

createGitHubRelease(fullTag, artifactsInput)

setOutput("version", versionStr)

setOutput("tag", fullTag)
