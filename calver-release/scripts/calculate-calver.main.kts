#!/usr/bin/env kotlin
@file:Suppress("KotlinPrintToLogpoint")

import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.io.path.Path
import kotlin.io.path.appendText

fun executeCommand(vararg command: String): String {
  val process = ProcessBuilder(*command).redirectError(ProcessBuilder.Redirect.INHERIT).start()
  val output = process.inputStream.bufferedReader().use { it.readText().trim() }
  check(process.waitFor() == 0) {
    "❌ Command failed: ${command.joinToString(" ")}"
  }
  return output
}

fun env(name: String): String =
    requireNotNull(System.getenv(name)?.takeIf { it.isNotBlank() }) {
      "❌ Missing $name environment variable."
    }

fun envOrDefault(name: String, defaultValue: String): String =
    System.getenv(name)?.takeIf { it.isNotBlank() } ?: defaultValue

private val githubOutputPath = Path(env("GITHUB_OUTPUT"))

fun setOutput(name: String, value: String) {
  githubOutputPath.appendText("$name=$value\n")
}

fun formatCalverPart(pattern: String, now: ZonedDateTime): String {
  return try {
    DateTimeFormatter.ofPattern(pattern).format(now)
  } catch (_: Exception) {
    pattern
  }
}

fun fetchExistingTags(): List<String> {
  try {
    executeCommand("git", "fetch", "--tags")
  } catch (_: Exception) {}
  val output =
      try {
        executeCommand("git", "tag", "-l")
      } catch (_: Exception) {
        ""
      }
  return output.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
}

fun calculateNextMicroVersion(
    existingTags: List<String>,
    prefix: String,
    modifier: String,
    major: String,
    minor: String,
): Int {
  val prefixAndPeriod = "$prefix$major.$minor."
  var maxMicro = -1

  for (tag in existingTags) {
    if (tag.startsWith(prefixAndPeriod) && (modifier.isEmpty() || tag.endsWith(modifier))) {
      val microPart = tag.substring(prefixAndPeriod.length, tag.length - modifier.length)
      val microVal = microPart.toIntOrNull()
      if (microVal != null && microVal > maxMicro) {
        maxMicro = microVal
      }
    }
  }
  return maxMicro + 1
}

fun main() {
  val majorFormat = envOrDefault("INPUT_MAJOR", "yyyy")
  val minorFormat = envOrDefault("INPUT_MINOR", "MM")
  val microFormat = envOrDefault("INPUT_MICRO", "patch")
  val tagPrefix = envOrDefault("INPUT_TAG_PREFIX", "v")
  val modifier = envOrDefault("INPUT_MODIFIER", "")

  val now: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC)

  val majorVersion = formatCalverPart(majorFormat, now)
  val minorVersion = formatCalverPart(minorFormat, now)

  val isMicroPatch =
      microFormat.equals("patch", ignoreCase = true) ||
          microFormat.equals("micro", ignoreCase = true)
  val existingTags = fetchExistingTags()

  val microVersion =
      if (isMicroPatch) {
        calculateNextMicroVersion(existingTags, tagPrefix, modifier, majorVersion, minorVersion).toString()
      } else {
        formatCalverPart(microFormat, now)
      }

  val baseVersion = "$majorVersion.$minorVersion.$microVersion"
  val versionStr = "$baseVersion$modifier"
  val fullTag = "$tagPrefix$versionStr"

  println("🔢 Calculated CalVer Version: $versionStr")
  println("🏷️ Calculated Full Tag: $fullTag")

  setOutput("version", versionStr)
  setOutput("tag", fullTag)
}

main()
