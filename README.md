# 🐙 Reusable GitHub Actions & Workflows

This repository contains reusable GitHub Actions and Workflows for my personal projects. Feel free to use them in your
own projects and contribute back if you have improvements or new ideas.

## 🧰 Actions

| Action                                 | Description                                                                                                                                                                    |
|:---------------------------------------|:-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [**CalVer Release**](./calver-release) | Automatically calculates Calendar Versioning (CalVer) releases, updates files, commits changes, creates git tags, and publishes GitHub releases with optional build artifacts. |
| [**Setup Gradle**](./setup-gradle)     | Sets up Java JDK (default JDK 25 JetBrains), Gradle with build caching, gradlew executable permissions, keystore decoding, and Kotlin/JS dependency caching.                   |

## 🔄 Workflows

| Workflow                                         | Description                                                                                                                            |
|:-------------------------------------------------|:---------------------------------------------------------------------------------------------------------------------------------------|
| [**Shared Gradle Build**](./.github/workflows/shared-gradle-build.md) | Reusable workflow to set up Java & Gradle, decode keystores, execute Gradle tasks or custom build scripts, and upload build artifacts. |

## 📄 License

This project is licensed under the Apache License 2.0 — see the [LICENSE](LICENSE) file for details.
