# 🐘 Shared Gradle Build Reusable Workflow

A reusable GitHub Actions workflow ([`shared-gradle-build.yml`](shared-gradle-build.yml)) that sets up Java JDK & Gradle
(using `setup-gradle`), decodes keystores, handles Kotlin/JS caching, executes Gradle tasks or custom build scripts, and
uploads artifacts.

## ✨ Features

- **JDK & Gradle Setup**: Invokes [`tozydev/gh-actions/setup-gradle`](../../setup-gradle) action to configure JDK and
  Gradle with build caching.
- **Flexible Execution**: Accepts `gradle-args` (e.g. `build --stacktrace`) or a custom `build-script`. If
  `build-script` is provided, it executes the custom script instead of `./gradlew <gradle-args>`.
- **Keystore Decoding**: Supports base64 keystore decoding via secrets.
- **Kotlin/JS Caching**: Supports Yarn/NPM dependency caching for Kotlin/JS projects.
- **Artifact Uploading**: Uploads build artifacts using when `upload-artifacts-path` is specified.

## 🚀 Usage

Call this workflow from any repository:

```yaml
name: Build

on:
  push:
    branches: [ main ]
  pull_request:

jobs:
  build:
    uses: tozydev/gh-actions/.github/workflows/shared-gradle-build.yml@v1
    with:
      gradle-args: 'build check'
      upload-artifacts-path: 'build/libs/*.jar'
    secrets:
      github-token: ${{ secrets.GITHUB_TOKEN }}
      keystore-base64: ${{ secrets.KEYSTORE_BASE64 }}
```

Using a custom `build-script`:

```yaml
jobs:
  build:
    uses: tozydev/gh-actions/.github/workflows/shared-gradle-build.yml@v1
    with:
      build-script: |
        ./gradlew assemble
        ./gradlew check --continue
```

## 📥 Inputs

| Input                             | Description                                                                    | Required | Default                               |
|-----------------------------------|--------------------------------------------------------------------------------|----------|---------------------------------------|
| `java-version`                    | Java JDK version to setup.                                                     | `false`  | `25`                                  |
| `java-distribution`               | Java JDK distribution vendor.                                                  | `false`  | `jetbrains`                           |
| `gradle-args`                     | Gradle task(s) and CLI arguments to execute.                                   | `false`  | `build`                               |
| `build-script`                    | Custom shell script to run instead of `./gradlew <gradle-args>`.               | `false`  | `""`                                  |
| `keystore-path`                   | Output path to save the decoded keystore file.                                 | `false`  | `${{ runner.temp }}/release.keystore` |
| `kotlin-js-cache`                 | Enables Kotlin/JS dependency caching (`'yarn'`, `'npm'`, or empty to disable). | `false`  | `""`                                  |
| `kotlin-js-cache-dependency-path` | Glob pattern or path to Kotlin/JS lockfiles for cache hashing.                 | `false`  | `.kotlin-locks/**/package-lock.json`  |
| `upload-artifacts-path`           | Newline-separated paths or glob patterns of artifacts to upload.               | `false`  | `""`                                  |
| `upload-artifacts-name`           | Name for the uploaded artifact.                                                | `false`  | `build-artifacts`                     |
| `env-vars`                        | Multiline `KEY=VALUE` environment variables string.                            | `false`  | `""`                                  |

## 🔑 Secrets

| Secret            | Description                                                | Required |
|-------------------|------------------------------------------------------------|----------|
| `github-token`    | GitHub token for setup-java and checkout authentication.   | `false`  |
| `keystore-base64` | Base64-encoded keystore secret string.                     | `false`  |
| `env-vars`        | Multiline `KEY=VALUE` secret environment variables string. | `false`  |

## 📄 License

This project is licensed under the Apache License 2.0 — see the [LICENSE](../../LICENSE) file for details.
