# 🐘 Setup Gradle Composite Action

A reusable composite GitHub Action to set up Java (JDK), Gradle (with caching), grant executable permissions to
`./gradlew`, decode base64 keystores, and cache Kotlin/JS dependencies.

## ✨ Features

- **JDK Setup**: Powered by [`actions/setup-java`](https://github.com/actions/setup-java).
- **Gradle Setup**: Powered by [`gradle/actions/setup-gradle`](https://github.com/gradle/actions). Automatic Gradle
  build caching and configuration.
- **Gradle Executable Permission**: Automatically runs `chmod +x ./gradlew` if `./gradlew` exists.
- **Keystore Decoding**: Safely decodes base64-encoded keystore secrets to a specified runner temp path for signing
  Android apps or other purposes.
- **Kotlin/JS Cache**: Optional cache step for Kotlin/JS build dependencies using `kotlin-js-cache` (`yarn` or `npm`).

## 🚀 Usage

```yaml
steps:
  - name: Checkout Repository
    uses: actions/checkout@v6

  - name: Setup Java & Gradle
    uses: tozydev/gh-actions/setup-gradle@v1
    with:
      github-token: ${{ secrets.GITHUB_TOKEN }}
      keystore-base64: ${{ secrets.KEYSTORE_BASE64 }}
      kotlin-js-cache: yarn # or "npm"
```

## 📥 Inputs

| Input                             | Description                                                                    | Required | Default                               |
|-----------------------------------|--------------------------------------------------------------------------------|----------|---------------------------------------|
| `java-version`                    | Java JDK version to setup.                                                     | `false`  | `25`                                  |
| `java-distribution`               | Java JDK distribution vendor.                                                  | `false`  | `jetbrains`                           |
| `github-token`                    | GitHub token for authentication (`actions/setup-java`).                        | `false`  | `${{ github.token }}`                 |
| `keystore-base64`                 | Base64-encoded keystore file string to decode.                                 | `false`  | `""`                                  |
| `keystore-path`                   | Output path to save the decoded keystore file.                                 | `false`  | `${{ runner.temp }}/release.keystore` |
| `kotlin-js-cache`                 | Enables Kotlin/JS dependency caching (`'yarn'`, `'npm'`, or empty to disable). | `false`  | `""`                                  |
| `kotlin-js-cache-dependency-path` | Glob pattern or path to Kotlin/JS lockfiles for cache hashing.                 | `false`  | `.kotlin-locks/**/package-lock.json`  |

## 📄 License

Distributed under the [Apache 2.0 License](../LICENSE).
