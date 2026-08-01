# 🚀 CalVer Release Action

A GitHub Composite Action that automatically makes GitHub release using Calendar Versioning
([CalVer](https://calver.org/))

## ✨ Features

- **Automated CalVer Calculation**: Flexible version schemes using date patterns (e.g. `yyyy`, `M`, `dd`) or incremental
  `patch` counters.
- **File Version Bumping**: Update version placeholders across configuration/manifest files.
- **Git Commit & Tagging**: Automatically commits file changes using custom commit messages and tag format.
- **GitHub Release & Artifact Upload**: Automatically creates GitHub Releases with attached release artifacts.
- **Flexible Authentication**: Supports default `GITHUB_TOKEN` or GitHub App Client ID & Private Key authentication.

## 📥 Inputs

| Input             | Description                                                                  | Required | Default                           |
|:------------------|:-----------------------------------------------------------------------------|:--------:|:----------------------------------|
| `major`           | Major version format pattern or `'patch'` for incremental counter.           |    No    | `yyyy`                            |
| `minor`           | Minor version format pattern or `'patch'` for incremental counter.           |    No    | `M`                               |
| `micro`           | Micro version format pattern or `'patch'` for incremental counter.           |    No    | `patch`                           |
| `tag-prefix`      | Prefix added to the tag name.                                                |    No    | `v`                               |
| `modifier`        | Version/tag modifier suffix (e.g. `-alpha` or `dev`).                        |    No    | `""`                              |
| `prerelease`      | Whether to mark the release as a GitHub prerelease (`true`/`false`).         |    No    | `false`                           |
| `extra-files`     | Newline-separated list of file paths in which to bump version.               |    No    | `""`                              |
| `artifacts`       | Newline-separated list of artifact file paths to upload to release.          |    No    | `""`                              |
| `commit-message`  | Commit message template for version bump (supports `{tag}` and `{version}`). |    No    | `chore(release): {tag} [skip ci]` |
| `github-token`    | GitHub token for git checkout, commit, tag, and release operations.          |    No    | `${{ github.token }}`             |
| `app-client-id`   | Optional GitHub App Client ID for authentication.                            |    No    | `""`                              |
| `app-private-key` | Optional GitHub App Private Key for authentication.                          |    No    | `""`                              |

## 📤 Outputs

| Output    | Description                                       | Example     |
|:----------|:--------------------------------------------------|:------------|
| `version` | Calculated release version string without prefix. | `2026.8.1`  |
| `tag`     | Full tag string including prefix and modifier.    | `v2026.8.1` |

## 💡 Usage Example

```yaml
- name: CalVer Release
  uses: tozydev/gh-actions/calver-release@main
```

For a complete workflow setup, see
the [tusu release workflow](https://github.com/tozydev/tusu/blob/0b1191268d5e5ec7249ae6c8dd16da49c40195bd/.github/workflows/release.yml).

## 📄 License

Distributed under the [Apache 2.0 License](../LICENSE).
