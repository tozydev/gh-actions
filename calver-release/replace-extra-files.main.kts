#!/usr/bin/env kotlin
import kotlin.io.path.Path
import kotlin.io.path.appendText
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.pathString
import kotlin.io.path.readText
import kotlin.io.path.writeText

fun env(name: String): String =
    requireNotNull(System.getenv(name)?.takeIf { it.isNotBlank() }) {
      "❌ Missing $name environment variable."
    }

private val githubOutputPath = Path(env("GITHUB_OUTPUT"))

fun setOutput(name: String, value: String) {
  githubOutputPath.appendText("$name=$value\n")
}

fun main() {
  val newVersion = env("INPUT_VERSION")
  val filesInput = System.getenv("INPUT_EXTRA_FILES") ?: ""

  val files = filesInput.split(Regex("[\\r\\n]+")).map { it.trim() }.filter { it.isNotEmpty() }
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
        println("📝 Updated version in file: ${file.pathString}")
      }
    } else {
      println("⚠️ Warning: File not found: $path")
    }
  }

  setOutput("modified-files", modifiedFiles.joinToString(","))
}

main()
