#!/usr/bin/env kotlin
import kotlin.io.path.Path
import kotlin.io.path.appendText

@DslMarker annotation class ExecDsl

@ExecDsl
class ExecBuilder {
  private lateinit var command: String
  private val args = mutableListOf<String>()

  fun command(cmd: String) {
    command = cmd
  }

  fun args(vararg arguments: String) {
    args += arguments
  }

  internal fun start(): Process =
      ProcessBuilder(command, *args.toTypedArray())
          .redirectError(ProcessBuilder.Redirect.PIPE)
          .redirectOutput(ProcessBuilder.Redirect.PIPE)
          .start()
}

fun exec(ignoreExitCode: Boolean = false, block: ExecBuilder.() -> Unit): String =
    ExecBuilder().apply(block).start().run {
      var err = ""
      val errThread = Thread.startVirtualThread {
        err = errorStream.bufferedReader().readText().trim()
      }

      val out = inputStream.bufferedReader().readText().trim()
      errThread.join()
      val exitCode = waitFor()

      if (exitCode != 0 && !ignoreExitCode) {
        throw RuntimeException(
            "Command failed [${args.joinToString(" ")}] with exit code $exitCode:\nOutput: $out\nError: $err"
        )
      }
      out
    }

private val githubOutputPath by lazy {
  val env = System.getenv("GITHUB_OUTPUT")
  if (env == null) {
    println(
        "Warning: GITHUB_OUTPUT environment variable is not set. Output will be printed to console."
    )
  }
  env?.let { Path(it) }
}

fun setOutput(name: String, value: String) {
  if (githubOutputPath == null) {
    println("GITHUB_OUTPUT ($name): $value")
  }
  githubOutputPath?.appendText("$name=$value\n")
}
