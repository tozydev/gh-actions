#!/usr/bin/env kotlin
@file:Suppress("KotlinPrintToLogpoint")

import kotlin.io.path.Path
import kotlin.io.path.exists

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

fun main() {
  val tag = env("INPUT_TAG")
  val version = env("INPUT_VERSION")
  val targetSha = env("TARGET_SHA")
  val repo = env("GITHUB_REPOSITORY")
  val artifactsRaw = envOrDefault("INPUT_ARTIFACTS", "")

  val artifactPaths =
      artifactsRaw
          .split(Regex("[\\r\\n]+"))
          .map { it.trim() }
          .filter { it.isNotEmpty() }
          .filter { Path(it).exists() }

  val isPrerelease = envOrDefault("INPUT_PRERELEASE", "false").toBoolean()

  val ghArgs =
      mutableListOf(
          "release",
          "create",
          tag,
          "--repo",
          repo,
          "--target",
          targetSha,
          "--generate-notes",
          "--title",
          version,
      )
  if (isPrerelease) {
    ghArgs.add("--prerelease")
  }
  ghArgs.addAll(artifactPaths)

  executeCommand("gh", *ghArgs.toTypedArray())
  println("🎉 Created GitHub Release: $tag")
}

main()
