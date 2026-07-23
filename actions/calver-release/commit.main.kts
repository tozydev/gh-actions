#!/usr/bin/env kotlin
/**
 * Commits and push to GitHub via GitHub API.
 *
 * **Inputs (Environment Variables):**
 * - `COMMIT_FILES` *(Required)*: Comma-separated list of relative workspace file paths to commit.
 * - `COMMIT_MESSAGE` *(Optional)*: Headline message for the commit.
 * - `TARGET_BRANCH` *(Optional)*: Target git branch (defaults to `main`).
 *
 * **Outputs (GitHub Output):**
 * - `commit-sha`: New commited SHA
 */
@file:Suppress("KotlinPrintToLogpoint")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.path.Path
import kotlin.io.path.appendText
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.readBytes
import kotlin.io.path.writeText

@Serializable data class BranchInput(val repositoryNameWithOwner: String, val branchName: String)

@Serializable data class MessageInput(val headline: String)

@Serializable data class AdditionFile(val path: String, val contents: String)

@Serializable data class FileChangesInput(val additions: List<AdditionFile>)

@Serializable
data class CreateCommitInput(
    val branch: BranchInput,
    val expectedHeadOid: String,
    val message: MessageInput,
    val fileChanges: FileChangesInput,
)

@Serializable data class GraphQLVariables(val input: CreateCommitInput)

@Serializable data class GraphQLRequest(val query: String, val variables: GraphQLVariables)

@Serializable data class CommitOid(val oid: String)

@Serializable data class CreateCommitPayload(val commit: CommitOid)

@Serializable data class MutationData(val createCommitOnBranch: CreateCommitPayload)

@Serializable data class GraphQLResponse(val data: MutationData)

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

fun main() {
  val repo = env("GITHUB_REPOSITORY")

  val commitMessage =
      envOrDefault("COMMIT_MESSAGE", "chore: auto update data via Kotlin Script [skip ci]")

  val filesInput = env("COMMIT_FILES")
  val filesToCommit =
      filesInput.split(Regex("[\\r\\n,]+")).map { it.trim() }.filter { it.isNotEmpty() }
  require(filesToCommit.isNotEmpty()) {
    "❌ File list for commit is empty. Please check COMMIT_FILES."
  }

  val branch = envOrDefault("TARGET_BRANCH", "main")

  println("⚡ Fetching current HEAD commit SHA...")
  val expectedHeadOid = executeCommand("git", "rev-parse", "HEAD")
  println("📌 HEAD OID: $expectedHeadOid")

  val additions = filesToCommit.mapNotNull { path ->
    val file = Path(path)
    if (file.exists()) {
      val base64Contents = Base64.encode(file.readBytes())
      println("📄 Processed file: $path")
      AdditionFile(path = path, contents = base64Contents)
    } else {
      println("⚠️ Warning: File not found, skipping: $path")
      null
    }
  }

  check(additions.isNotEmpty()) {
    "⚠️ No valid files found to commit."
  }

  val requestPayload =
      GraphQLRequest(
          query =
              $$"""
              mutation($input: CreateCommitOnBranchInput!) {
                createCommitOnBranch(input: $input) {
                  commit { oid }
                }
              }
              """
                  .trimIndent(),
          variables =
              GraphQLVariables(
                  input =
                      CreateCommitInput(
                          branch = BranchInput(repositoryNameWithOwner = repo, branchName = branch),
                          expectedHeadOid = expectedHeadOid,
                          message = MessageInput(headline = commitMessage),
                          fileChanges = FileChangesInput(additions = additions),
                      )
              ),
      )

  val jsonConfig = Json { ignoreUnknownKeys = true }
  val jsonPayloadString = jsonConfig.encodeToString(requestPayload)
  val payloadFile = Path("payload.json")

  try {
    payloadFile.writeText(jsonPayloadString)

    println("🚀 Submitting request for signed commit: \"$commitMessage\"")
    val apiResponseRaw = executeCommand("gh", "api", "graphql", "--input", payloadFile.name)

    val responseObj = jsonConfig.decodeFromString<GraphQLResponse>(apiResponseRaw)
    val newCommitSha = responseObj.data.createCommitOnBranch.commit.oid

    println("🎉 Signed commit created successfully!")
    println("🔑 New Commit SHA: $newCommitSha")

    setOutput("commit-sha", newCommitSha)
    println("✅ Output variable 'commit-sha' set successfully.")
  } finally {
    payloadFile.deleteIfExists()
  }
}

main()
